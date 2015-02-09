package se.nicklasgavelin.sphero;

import se.nicklasgavelin.bluetooth.BluetoothConnection;
import se.nicklasgavelin.bluetooth.BluetoothDevice;
import se.nicklasgavelin.configuration.ProjectProperties;
import se.nicklasgavelin.log.Logging;
import se.nicklasgavelin.sphero.command.*;
import se.nicklasgavelin.sphero.exception.RobotBluetoothException;
import se.nicklasgavelin.sphero.exception.RobotInitializeConnectionFailed;
import se.nicklasgavelin.sphero.macro.MacroObject;
import se.nicklasgavelin.sphero.macro.command.Delay;
import se.nicklasgavelin.sphero.macro.command.RGB;
import se.nicklasgavelin.sphero.response.InformationResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage;
import se.nicklasgavelin.util.Value;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Robot class. Mirrors the direct connection between the application
 * and the Sphero robot. Contains some macro commands to perform simple
 * direct commands.
 * 
 * It's also possible to create commands and send them directly using
 * the Robot.sendCommand method.
 * 
 * Commands may be sent with time delays or periodicity.
 * 
 * Example usage:
 * Robot r = new Robot(<BluetoothDevice>);
 * r.connect();
 * r.sendCommand(new FrontLEDCommand(0.5F));
 * 
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of
 *         Technology
 * @version 1.2
 * 
 *          TODO: Set temporary internal values on sending commands so that we don't
 *          update them too late if we send multiple commands
 */
public class Robot
{
	private RobotSetting rs;

	// Bluetooth
	private final BluetoothDevice bt;
	private BluetoothConnection btc;
	private boolean connected;
	// Listener/writer
	private RobotStreamListener listeningThread;
	private RobotSendingQueue sendingTimer;
	private final List<RobotListener> listeners;
	// Other
	private String name;
	// Robot macro
	private MACRO_SETTINGS macroSettings;
	// Robot position and led color
	private RobotMovement movement;
	private RobotRawMovement rawMovement;
	private RobotLED led;
	// Pinger
	private float PING_INTERVAL; // Time in milliseconds
	// Address
	/**
	 * The start of the Bluetooth address that is describing if the address
	 * belongs to a Sphero device.
	 */
	public static final String ROBOT_ADDRESS_PREFIX = "00066";

    /**
	 * Create a robot from a Bluetooth device. You need to call Robot.connect
	 * after creating a robot to connect to it via the Bluetooth connection
	 * given.
	 * 
	 * @param bt The Bluetooth device that represents the robot
	 * 
	 *             throws
	 *             RobotBluetoothException
	 */
	public Robot(BluetoothDevice bt) throws RobotBluetoothException
	{
		this(bt, null);
	}

	/**
	 * Create a robot from a Bluetooth device. You need to call Robot.connect
	 * after creating a robot to connect to it via the Bluetooth connection
	 * given.
	 * 
	 * @param bt The Bluetooth device that represents the robot
	 * 
	 * @throws RobotBluetoothException
	 */
	public Robot(BluetoothDevice bt, RobotSetting rs) throws RobotBluetoothException
	{
		this.bt = bt;

		// Create a unique logger for this class instance
		// this.logger = Logging.createLogger(Robot.class, Robot.logLevel,
		// this.bt.getAddress());

		// Control that we got a valid robot
        // FIXME what are the addresses boundaries given to spheros? (every hardware buy those addresses to an organism)
/*		if (!this.bt.getAddress().startsWith(ROBOT_ADDRESS_PREFIX))
		{
			String msg = invalidAddressResponses[Value.clamp(error_num++, 0, invalidAddressResponses.length - 1)];
			Logging.error(msg);
			throw new InvalidRobotAddressException(msg);
		}
*/
        this.rs = rs == null ? ProjectProperties.getInstance().getRobotSetting() : rs;

		// Set ping interval
        PING_INTERVAL = this.rs.getSocketPingInterval();

		// Initialize the position and LEDs
        movement = new RobotMovement(this);
        rawMovement = new RobotRawMovement(this);
        led = new RobotLED(this);
        macroSettings = new MACRO_SETTINGS(this);

		// Discover the connection services that we can use
		bt.discover();

		// Create an empty array of listeners
        listeners = new ArrayList<>();

		Logging.debug("Robot created successfully");

		// Add system hook // FIXME shutdown hook on a thread with a thread
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Force disconnect asap!
            try
            {
                // Wait for disconnect event
                disconnect(false);

                if (sendingTimer != null && sendingTimer.getWriter() != null) {
                    sendingTimer.getWriter().join();
                }
            }
            catch(InterruptedException ex)
            {
                Logger.getLogger(Robot.class.getName()).log(Level.SEVERE, null, ex);
            }
        }));
	}

	/*
	 * *****************************************************
	 * LISTENERS
	 * ****************************************************
	 */

	/**
	 * Add a robot listener to the current class instance
	 * 
	 * @param l The listener to add
	 */
	public void addListener(RobotListener l)
	{
		Logging.debug("Adding listener of type " + l.getClass().getCanonicalName());

		synchronized(listeners)
		{
			if (!listeners.contains(l))
                listeners.add(l);
		}
	}

	/**
	 * Remove a listener that is listening from the current class
	 * instance.
	 * 
	 * @param l The listener to remove
	 */
	public void removeListener(RobotListener l)
	{
		synchronized(listeners)
		{
			// Check so that we can remove it
			if (listeners.contains(l))
                listeners.remove(l);
		}
	}

	/**
	 * Notify all listeners about a device response
	 * 
	 * @param dr The device response that was received
	 * @param dc The device command belonging to the device response
	 */
    void notifyListenersDeviceResponse(ResponseMessage dr, CommandMessage dc)
	{
		Logging.debug("Notifying listeners about device respose " + dr + " for device command " + dc);

		synchronized(listeners)
		{
			// Go through all listeners and notify them
			for(RobotListener r : listeners)
				r.responseReceived(this, dr, dc);
		}
	}

	void notifyListenersInformationResponse(InformationResponseMessage dir)
	{
		Logging.debug("Nofifying listeners about information response " + dir);
		
		synchronized(listeners)
		{
			for(RobotListener r : listeners)
				r.informationResponseReceived(this, dir);
		}
	}

	/**
	 * Notify listeners of occurring events
	 * 
	 * @param event The event to notify about
	 */
    void notifyListenerEvent(RobotListener.EVENT_CODE event)
	{
		Logging.debug("Notifying listeners about event " + event);

		// Notify all listeners
		synchronized(listeners)
		{
			for(RobotListener r : listeners)
				r.event(this, event);
		}
	}

	/**
	 * Called to close down the complete connection and notify listeners
	 * about an unexpected closing.
	 */
    void connectionClosedUnexpected()
	{
		// Cancel the sending of new messages
        sendingTimer.cancel();

		// Cancel the listening of incomming messages
        listeningThread.stopThread();

		// Close the bluetooth connection
        btc.stop();

		// Notify about disconnect
		if (connected)
		{
            connected = false;
			Logging.error("Connection closed unexpectedly for some reason, all threads have been closed down for the robot");
            notifyListenerEvent(RobotListener.EVENT_CODE.CONNECTION_CLOSED_UNEXPECTED);
		}
	}

	/*
	 * *****************************************************
	 * CONNECTION MANAGEMENT
	 * ****************************************************
	 */

	/**
	 * Connect to the robot via the Bluetooth connection given in the
	 * constructor.
	 * Will NOT throw any exceptions if connection fails.
	 * 
	 * @return True if connection succeeded, false otherwise
	 */
	public boolean connect()
	{
		return connect(false);
	}

	/**
	 * Connect with a possible chance of getting an exception thrown if the
	 * connection
	 * fails.
	 * 
	 * @param throwException True to throw exception, false otherwise
	 * 
	 * @throws RobotInitializeConnectionFailed Thrown if throwException is set
	 *             to true and initialization failed
	 * @return True if connected, will throw exception if not connected
	 */
	public boolean connect(boolean throwException)
	{
		try
		{
			return internalConnect();
		}
		catch(RobotBluetoothException e)
		{
			if (throwException)
				throw new RobotInitializeConnectionFailed(e.getMessage());
		}
		catch(RobotInitializeConnectionFailed e)
		{
			if (throwException)
				throw e;
		}

		return false;
	}

	/**
	 * Connects to the robot via Bluetooth. Will return true if the connection
	 * was successful, throws an exception otherwise.
	 * 
	 * @throws RobotInitializeConnectionFailed If connection failed
	 * @return True if connection succeeded
	 */
	private boolean internalConnect() throws RobotInitializeConnectionFailed, RobotBluetoothException
	{
		Logging.debug("Trying to connect to " + getName() + ":" + getAddress());
        btc = bt.connect();

		// Check if we could connect to the bluetooth device
		if (btc == null)
		{
			Logging.error("Failed to connect to the robot bluetooth connection");
			throw new RobotInitializeConnectionFailed("Failed to connect due to bluetooth error");
		}

		// We are now connected, continue with
		// the initialization of everything else regarding the connection
        connected = true;

		// Create a listening thread and close any old ones down
		if (listeningThread != null)
            listeningThread.stopThread();
        listeningThread = new RobotStreamListener(this, btc);
        listeningThread.start();

		// Create our sending timer
		if (sendingTimer != null)
            sendingTimer.cancel();
        sendingTimer = new RobotSendingQueue(this, btc);

		// Reset the robot
        sendSystemCommand(new AbortMacroCommand());
        sendSystemCommand(new RollCommand(movement.getHeading(), movement.getVelocity(), movement.getStop()));
        sendSystemCommand(new CalibrateCommand(movement.getHeading()));
        sendSystemCommand(new FrontLEDCommand(led.getFrontLEDBrightness()));
        sendSystemCommand(new RGBLEDCommand(getLed().getRGBColor()));

		// Create our pinger
        sendSystemCommand(new PingCommand(this), PING_INTERVAL, PING_INTERVAL);

		// Notify listeners
        notifyListenerEvent(connected ? RobotListener.EVENT_CODE.CONNECTION_ESTABLISHED : RobotListener.EVENT_CODE.CONNECTION_FAILED);

		// Return connection status
		return connected;
	}

	/**
	 * Disconnect from the robot (closes all streams and Bluetooth connections,
	 * also closes down all internal threads).
	 */
	public void disconnect()
	{
        disconnect(true);
	}

	private boolean disconnecting;

	/**
	 * Method to notify listeners about a disconnect and set the connect flag
	 * 
	 * @param notifyListeners True to notify listeners, false otherwise
	 */
	private void disconnect(boolean notifyListeners)
	{
		Logging.debug("Disconnecting from the current robot");

		if (connected)
		{
			// Close all connection
            closeConnections();
		}
		else
		{
			// Check if we want to notify listeners that there exists no active connection
			if (notifyListeners)
                notifyListenerEvent(RobotListener.EVENT_CODE.NO_CONNECTION_EXISTS);
		}
	}

	/**
	 * Closes down all listening and sending sockets
	 */
	private void closeConnections()
	{
		// Check if we have something to disconnect from
		if (connected)
		{
            // Set our connection flag to false
            connected = false;
            disconnecting = true;

			// Stop our transmission of commands
            sendingTimer.cancel();

			// Send a direct command to motorStop any movement (eludes the .cancel
			// command)
            sendingTimer.forceCommand(new AbortMacroCommand());
            sendingTimer.forceCommand(new RollCommand(0, 0, true));
            sendingTimer.forceCommand(new FrontLEDCommand(0));
            sendingTimer.forceCommand(new RGBLEDCommand(Color.BLACK));
            // Cancel the sending of new
//            sendingTimer.cancel();

            // Cancel the listening of incomming messages
            listeningThread.stopThread();

            // Close the bluetooth connection
            btc.stop();

            // Notify about disconnect
            Logging.error("Connection closed unexpectedly for some reason, all threads have been closed down for the robot");
            notifyListenerEvent(RobotListener.EVENT_CODE.CONNECTION_CLOSED_UNEXPECTED);
		}
	}

	/*
	 * *****************************************************
	 * COMMANDS
	 * ****************************************************
	 */

	/**
	 * Send a command to the active robot
	 * 
	 * @param command The command to send
	 */
	public void sendCommand(CommandMessage command)
	{
        sendingTimer.enqueue(command, false);
	}

	/**
	 * Enqueue a command to be sent after a macro has finished execution
	 * 
	 * @param command The command to run after macro command execution
	 */
	public void sendCommandAfterMacro(CommandMessage command)
	{
        macroSettings.sendCommandAfterMacro(command);
	}

	/**
	 * Stops the commands entered to be sent after the macro is finished
	 * running.
	 * To send new commands they have to be re-entered into the
	 * sendCommandAfterMacro method.
	 */
	public void cancelSendCommandAfterMacro()
	{
        macroSettings.clearSendingQueue();
	}

	/**
	 * Send a command with a given delay
	 * 
	 * @param command The command to send
	 * @param delay The delay before the command is sent
	 */
	public void sendCommand(CommandMessage command, float delay)
	{
        sendingTimer.enqueue(command, delay);
	}

	/**
	 * Send a command infinite times with a certain initial delay and a certain
	 * given period length between next-coming messages.
	 * 
	 * @param command The command to send
	 * @param initialDelay The initial delay before the first message is sent
	 *            (in milliseconds)
	 * @param periodLength The length between the transmissions
	 */
	public void sendPeriodicCommand(CommandMessage command, float initialDelay, float periodLength)
	{
        sendingTimer.enqueue(command, false, initialDelay, periodLength);
	}

	/**
	 * Send a system command
	 * 
	 * @param command The command to send
	 */
    void sendSystemCommand(CommandMessage command)
	{
        sendingTimer.enqueue(command, true);
	}

	/**
	 * Send a system command after a given delay
	 * 
	 * @param command The command to send
	 * @param delay The delay before sending the message
	 */
	private void sendSystemCommand(CommandMessage command, float delay)
	{
        sendingTimer.enqueue(command, delay, true);
	}

	/**
	 * Send a system command infinitely with a certain initial delay before the
	 * first message and a period length between nextcomming messages.
	 * 
	 * @param command The command to send
	 * @param initialDelay The initial delay before the first message is sent
	 *            (in milliseconds)
	 * @param periodLength The length between the transmissions
	 */
	private void sendSystemCommand(CommandMessage command, float initialDelay, float periodLength)
	{
        sendingTimer.enqueue(command, true, initialDelay, periodLength);
	}

	private boolean receivedFirstDisconnect;

    /**
	 * Updates position, led colors/brightness etc depending on the command that
	 * is sent.
	 * 
	 * @param command The command that is suppose to be sent
	 */
    void updateInternalValues(CommandMessage command)
	{
		// Disconnect event, we will disconnect if we are not connected and
		// we have sent both a roll motorStop command and a front led turn off command
		if (!connected && (command instanceof FrontLEDCommand || command instanceof RollCommand))
		{
			if (receivedFirstDisconnect)
			{
				// Stop any active listening thread
				if (listeningThread != null)
                    listeningThread.stopThread();

				// Stop any active sending timer thread
				if (sendingTimer != null)
                    sendingTimer.stopAll();

				// Stop the bluetooth connection
				if (btc != null)
                    btc.stop();

                // Send disconnect event
                notifyListenerEvent(RobotListener.EVENT_CODE.DISCONNECTED);
			}
			else
                receivedFirstDisconnect = true;
		}

		// Logging.debug("Updating internal values for " + command);

		// Update stuff event
		switch (command.getCommand())
		{
		/*
		 * Roll command, update internal values
		 */
			case ROLL:
                if (command instanceof RollCommand) {
                    RollCommand rc = (RollCommand) command;

                    // Set new values
                    movement.setHeading(rc.getHeading());
                    movement.setVelocity(rc.getVelocity());
                    movement.setStop(rc.getStopped());
                }
				break;

			case SPIN_LEFT:
				// TODO: Movements are stopped other than for some special commands
				break;

			case SPIN_RIGHT:
				// TODO: Movements are stopped other than for some special commands
				break;

			case RAW_MOTOR:
                if (command instanceof RawMotorCommand) {
                    RawMotorCommand raw = (RawMotorCommand) command;
                    rawMovement.setLeftMotorMode(raw.getLeftMode());
                    rawMovement.setRightMotorMode(raw.getRightMode());
                    rawMovement.setLeftMotorSpeed(raw.getLeftSpeed());
                    rawMovement.setRightMotorSpeed(raw.getRightSpeed());
                }
				break;

			/*
			 * Rotation rate.
			 * TODO: Does it have some effect on the robot? Havn't seen any
			 * definite
			 * effects when performed
			 */
			case ROTATION_RATE:
                if (command instanceof RotationRateCommand) {
                    RotationRateCommand rrc = (RotationRateCommand) command;
                    movement.setRotationRate(rrc.getRate());
                }
				break;

			case JUMP_TO_BOOTLOADER:
			case GO_TO_SLEEP:
				// Graceful disconnect as we will loose the connection when
				// jumping to the bootloader
                disconnect();

				break;

			case RGB_LED_OUTPUT:
                if (command instanceof RGBLEDCommand) {
                    RGBLEDCommand rgb = (RGBLEDCommand) command;

                    // Update led values
                    led.setRed(rgb.getRed());
                    led.setGreen(rgb.getGreen());
                    led.setBlue(rgb.getBlue());
                }
				break;

			case FRONT_LED_OUTPUT:
                if (command instanceof FrontLEDCommand) {
                    FrontLEDCommand flc = (FrontLEDCommand) command;
                    led.setBrightness(flc.getBrightness());
                }
				break;

			/*
			 * Havn't seen any effect of this command
			 */
            case PING:
                break;
            case VERSIONING:
                break;
            case SET_BLUETOOTH_NAME:
				// Update the name
                bt.updateName();

				break;
            case GET_BLUETOOTH_INFO:
                break;
            case LEVEL_1_DIAGNOSTICS:
                break;
            case JUMP_TO_MAIN:
                break;
            case CALIBRATE:
                break;
            case STABILIZATION:
                break;
            case BOOST:
                break;
            case GET_CONFIGURATION_BLOCK:
                break;
            case RUN_MACRO:
                break;
            case MACRO:
                break;
            case SAVE_MACRO:
                break;
            case ABORT_MACRO:
                break;
            case SET_DATA_STREAMING:
                break;
            case CUSTOM_PING:
                break;
        }
	}

	/*
	 * *****************************************************
	 * MACRO COMMANDS
	 * ****************************************************
	 */

	/**
	 * Roll the robot with a given motorHeading and speed
	 * 
	 * @param heading The motorHeading (0-360)
	 * @param speed The speed (0-1)
	 */
	public void roll(float heading, float speed)
	{
        sendCommand(new RollCommand(heading, speed, false));
	}

	/**
	 * Calibrate the motorHeading to a specific motorHeading (Will move the
	 * robot to this
	 * motorHeading)
	 * 
	 * @param heading The motorHeading to calibrate to (0-359)
	 */
	public void calibrate(float heading)
	{
        sendCommand(new RollCommand(heading, 0F, false));
        sendCommand(new CalibrateCommand(heading));

		// Blink the main led a few times to show calibration
        sendSystemCommand(new FrontLEDCommand(getLed().getFrontLEDBrightness()), 11000);
        sendSystemCommand(new FrontLEDCommand(0));
        sendSystemCommand(new FrontLEDCommand(0), 100, 10);
        sendSystemCommand(new FrontLEDCommand(1F), 200, 10);
	}

	/**
	 * Creates a transition between two different colors with a number of
	 * changes between the colors (the transition itself). The delay between
	 * each step is set to 25 ms.
	 * 
	 * @param from The color to go from
	 * @param to The color to end up with
	 * @param steps The number of steps to take between the change between the
	 *            two colors
	 */
	public void rgbTransition(Color from, Color to, int steps)
	{
        rgbTransition(from, to, steps, 25);
	}
	
	/**
	 * Creates a transition between two different colors with a number of
	 * changes between the colors (the transition itself). The delay between
	 * each color shift is set to 25 ms.
	 * 
	 * @param fRed The initial red color value
	 * @param fGreen The initial green color value
	 * @param fBlue The initial blue color value
	 * @param tRed The final red color value
	 * @param tGreen The final green color value
	 * @param tBlue The final blue color value
	 * @param steps Number of steps to take (The number of times to change
	 *            color until reaching the final color)
	 */
	public void rgbTransition(int fRed, int fGreen, int fBlue, int tRed, int tGreen, int tBlue, int steps)
	{
        rgbTransition(fRed, fGreen, fBlue, tRed, tGreen, tBlue, steps, 25);
	}

	/**
	 * Creates a transition between two different colors with a number of
	 * changes between the colors (the transition itself).
	 * 
	 * @param from The color to go from
	 * @param to The color to end up with
	 * @param steps The number of steps to take between the change between the
	 *            two colors
	 * @param dDelay Delay between the color shifts
	 */
	public void rgbTransition(Color from, Color to, int steps, int dDelay)
	{
        rgbTransition(from.getRed(), from.getGreen(), from.getBlue(), to.getRed(), to.getGreen(), to.getBlue(), steps, dDelay);
	}

	/**
	 * Creates a transition between two different colors with a number of
	 * changes between the colors (the transition itself).
	 * 
	 * @param fRed The initial red color value
	 * @param fGreen The initial green color value
	 * @param fBlue The initial blue color value
	 * @param tRed The final red color value
	 * @param tGreen The final green color value
	 * @param tBlue The final blue color value
	 * @param steps Number of steps to take (The number of times to change
	 *            color until reaching the final color)
	 * @param dDelay Delay between the color shifts
	 */
	public void rgbTransition(int fRed, int fGreen, int fBlue, int tRed, int tGreen, int tBlue, int steps, int dDelay)
	{
        // Hue, saturation, intensity
		float[] fHSB = Color.RGBtoHSB(fRed, fGreen, fBlue, null);
		float[] tHSB = Color.RGBtoHSB(tRed, tGreen, tBlue, null);

		float hDif = Math.abs(fHSB[0] - tHSB[0]);
		float sDif = Math.abs(fHSB[1] - tHSB[1]);
		float iDif = Math.abs(fHSB[2] - tHSB[2]);

		boolean iHue = fHSB[0] < tHSB[0];
		boolean iSat = fHSB[1] < tHSB[1];
		boolean iInt = fHSB[2] < tHSB[2];

		float incHue = hDif / steps;
		float incSat = sDif / steps;
		float incInt = iDif / steps;

		Color c;
		float[] n = new float[ 3 ];

		// Create macro
		MacroObject mo = new MacroObject();

		// Go through all steps
		for(int i = 0; i < steps; i++)
		{
			// Calculate hue saturation and intensity
			n[0] = iHue ? fHSB[0] + i * incHue : fHSB[0] - i * incHue;
			n[1] = iSat ? fHSB[1] + i * incSat : fHSB[1] - i * incSat;
			n[2] = iInt ? fHSB[2] + i * incInt : fHSB[2] - i * incInt;

			// Get new color
			int ik = Color.HSBtoRGB(Value.clamp(n[0], 0, 1), Value.clamp(n[1], 0, 1), Value.clamp(n[2], 0, 1));
			c = new Color(ik);

			// Add new RGB commands
			mo.addCommand(new RGB(c, 0));
			mo.addCommand(new Delay(dDelay));
		}

		// Set streaming as we don't know if we can fit all macro commands in one message
		mo.setMode(MacroObject.MacroObjectMode.CachedStreaming);

		// Send macro to the Sphero device
        sendCommand(mo);
	}
	
	private void createFromToColorMacroObject(MacroObject mo, Color from, Color to, int steps, int dDelay)
	{
        // Hue, saturation, intensity
		float[] fHSB = Color.RGBtoHSB(from.getRed(), from.getGreen(), from.getBlue(), null);
		float[] tHSB = Color.RGBtoHSB(to.getRed(), to.getGreen(), to.getBlue(), null);

		float hDif = Math.abs(fHSB[0] - tHSB[0]);
		float sDif = Math.abs(fHSB[1] - tHSB[1]);
		float iDif = Math.abs(fHSB[2] - tHSB[2]);

		boolean iHue = fHSB[0] < tHSB[0];
		boolean iSat = fHSB[1] < tHSB[1];
		boolean iInt = fHSB[2] < tHSB[2];

		float incHue = hDif / steps;
		float incSat = sDif / steps;
		float incInt = iDif / steps;

		Color c;
		float[] n = new float[ 3 ];

		// Go through all steps
		for(int i = 0; i < steps; i++)
		{
			// Calculate hue saturation and intensity
			n[0] = iHue ? fHSB[0] + i * incHue : fHSB[0] - i * incHue;
			n[1] = iSat ? fHSB[1] + i * incSat : fHSB[1] - i * incSat;
			n[2] = iInt ? fHSB[2] + i * incInt : fHSB[2] - i * incInt;

			// Get new color
			int ik = Color.HSBtoRGB(Value.clamp(n[0], 0, 1), Value.clamp(n[1], 0, 1), Value.clamp(n[2], 0, 1));
			c = new Color(ik);

			// Add new RGB commands
			mo.addCommand(new RGB(c, 0));
			mo.addCommand(new Delay(dDelay));
		}
	}

	public void rgbBreath(Color from, Color to, int steps, int dDelay)
	{
		MacroObject mo = new MacroObject();
        createFromToColorMacroObject(mo, from, to, steps/2, dDelay/2);
        createFromToColorMacroObject(mo, to, from, steps/2, dDelay/2);

		// Set streaming as we don't know if we can fit all macro commands in one message
		mo.setMode(MacroObject.MacroObjectMode.CachedStreaming);

		// Send macro to the Sphero device
        sendCommand(mo);
	}
	
	/**
	 * Rotate the robot
	 * 
	 * @param heading The new motorHeading, 0-360
	 */
	public void rotate(float heading)
	{
        roll(heading, 0.0F);
	}

	/**
	 * Jump the robot to the bootloader part.
	 * 
	 * NOTICE: THE DEVICE CONNETION WILL DISCONNECT WHEN THE ROBOT
	 * JUMPS TO THE BOOTLOADER!
	 */
	public void jumpToBootloader()
	{
        sendCommand(new JumpToBootloaderCommand());
	}

	/**
	 * Send a sleep command to the robot.
	 * The sleep time is given in seconds.
	 * 
	 * @param time Number of seconds to sleep. The connection WILL be LOST to
	 *            the robot and have to be re-initialized.
	 */
	public void sleep(int time)
	{
        sendCommand(new SleepCommand(time));
	}

	/**
	 * Update the robot rotation rate
	 * 
	 * @param rotationRate The new rotation rate, value 0-1
	 */
	public void setRotationRate(float rotationRate)
	{
        sendCommand(new RotationRateCommand(rotationRate));
	}

	/**
	 * Set a new RGB color for the robot RGB LED
	 * 
	 * @param red The new red value
	 * @param green The new green value
	 * @param blue The new blue value
	 */
	public void setRGBLEDColor(int red, int green, int blue)
	{
        sendCommand(new RGBLEDCommand(red, green, blue));
	}

	/**
	 * Set a new color for the robot RGB LED
	 * 
	 * @param c The new color
	 */
	public void setRGBLedColor(Color c)
	{
        sendCommand(new RGBLEDCommand(c));
	}

	/**
	 * Resets the robots motorHeading.
	 * 
	 * Sends a roll command with current velocity and motorStop value and also a
	 * calibrate
	 * command to reset the motorHeading.
	 */
	public void resetHeading()
	{
        sendCommand(new RollCommand(0.0F, movement.getVelocity(), movement.getStop()));
        sendCommand(new CalibrateCommand(0.0F));
	}

	/**
	 * Update motorHeading offset
	 * 
	 * @param offset The motorHeading offset
	 */
	public void setHeadingOffset(double offset)
	{
        movement.getDriveAlgorithm().setHeadingOffset(offset);
	}

	/**
	 * Set ledBrightness of the front led. 0-1
	 * 
	 * @param brightness The ledBrightness value, 0-1
	 */
	public void setFrontLEDBrightness(float brightness)
	{
        sendCommand(new FrontLEDCommand(brightness));
	}

	/**
	 * Set the name of the robot.
	 * 
	 * Note: Doesn't seem to update anything, maybe not implemented on the
	 * Sphero yet?
	 * 
	 * @param name The new name
	 */
	public void setRobotName(String name)
	{
        sendCommand(new SetRobotNameCommand(name));
	}

	/**
	 * Turn on/off stabilization
	 * 
	 * @param on True for on, false for off
	 */
	public void stabilization(boolean on)
	{
        sendCommand(new StabilizationCommand(on));
	}

	/**
	 * Drive in a direction
	 * 
	 * @param x X direction
	 * @param y Y direction
	 * @param z Z direction
	 */
	public void drive(double x, double y, double z)
	{
		// Convert the values to the correct ones depending on the given algorithm
        movement.getDriveAlgorithm().convert(x, y, z);
        movement.getDriveAlgorithm().adjustHeading();

		// Cap the value
        movement.getDriveAlgorithm().setAdjustedHeading(Value.clamp(movement.getDriveAlgorithm().getAdjustedHeading(), 0.0D, 359.0D));

		// Send the command
        roll((float) movement.getDriveAlgorithm().getAdjustedHeading(), (float) movement.getDriveAlgorithm().getSpeed());
	}

	/**
	 * Boost the robot (Speed increase to maximum)
	 * 
	 * @param timeInterval Time interval for the boost command (in ms)
	 */
	public void boost(float timeInterval)
	{
		// Create commands to send
		RollCommand boost = new RollCommand(movement.getHeading(), 1F, false);
		RollCommand resetBoost = new RollCommand(movement.getHeading(), movement.getVelocity(), movement.getStop());

		// Send commands
        sendSystemCommand(boost);
        sendSystemCommand(resetBoost, timeInterval);
	}

	/**
	 * Send a command to motorStop the robot motors
	 */
	public void stopMotors()
	{
        sendCommand(new RollCommand(movement.getHeading(), 0.0F, true));
	}

	/**
	 * Returns true if motors are stopped. False otherwise. Will not return true
	 * if the speed is 0!
	 * 
	 * @return True if motors are stopped, false otherwise
	 */
	public boolean isStopped()
	{
		return !movement.getStop();
	}

	/**
	 * Set the current drive algorithm. Only affects the Robot.drive method.
	 * 
	 * @param algorithm The new drive algorithm
	 */
	public void setDriveAlgorithm(DriveAlgorithm algorithm)
	{
        movement.setDriveAlgorithm(algorithm);
	}

	/**
	 * Returns the current drive algorithm that affects the Robot.drive
	 * method.
	 * 
	 * @return The current drive algorithm
	 */
	public DriveAlgorithm getDriveAlgorithm()
	{
		return movement.getDriveAlgorithm();
	}

	/*
	 * *****************************************************
	 * MACRO
	 * ****************************************************
	 */

	/**
	 * Stop any current macros from running
	 */
	public void stopMacro()
	{
        macroSettings.stopMacro();
	}

	/**
	 * Send a macro to the Sphero device. If the macro mode is set to Normal
	 * either
	 * a RunMacroCommand has to be sent or you have to run .playMacro on the
	 * Robot instance
	 * 
	 * @param macro The macro to send to the Sphero
	 */
	public void sendCommand(MacroObject macro)
	{
        macroSettings.playMacro(macro);
	}

	/*
	 * *****************************************************
	 * GETTERS
	 * ****************************************************
	 */
    /**
	 * Checks if a given Bluetooth address is a valid Sphero address or not.
	 * 
	 * @param address The Bluetooth address
	 * 
	 * @return True if valid, false otherwise
	 */
	public static boolean isValidAddress(String address)
	{
		return address.startsWith(ROBOT_ADDRESS_PREFIX);
	}

	/**
	 * Returns true if the robot is connected
	 * 
	 * @return True if connected to the robot, false otherwise
	 */
	public boolean isConnected()
	{
		return connected;
	}

	/**
	 * Returns the Bluetooth connection address or null if no
	 * address could be returned
	 * 
	 * @return The Bluetooth connection URL
	 */
	public String getConnectionURL()
	{
		return bt.getConnectionURL();
	}

	/**
	 * Checks if a given Bluetooth device is a valid Sphero Bluetooth device or
	 * not.
	 * 
	 * @param device The Bluetooth device
	 * 
	 * @return True if valid, false otherwise
	 */
	public static boolean isValidDevice(BluetoothDevice device)
	{
		return device.getAddress().startsWith(ROBOT_ADDRESS_PREFIX);
	}

	/**
	 * Returns the robot unique id (identical to the Bluetooth address of the
	 * device)
	 * 
	 * @return The unique Bluetooth id
	 */
	public String getId()
	{
		return bt.getAddress();// this.bt.getRemoteDevice().getBluetoothAddress();
	}

	/**
	 * Returns the Bluetooth address of the robot.
	 * Same as getId()
	 * 
	 * @return The Bluetooth address of the robot
	 */
	public String getAddress()
	{
		return bt.getAddress();// this.bt.getRemoteDevice().getBluetoothAddress();
	}

	/**
	 * Returns the name of the robot
	 * 
	 * @return The name of the robot
	 */
	public String getName()
	{
		String n = bt.getName();
		if (n == null)
			return name;
		return n;
	}

	/**
	 * Returns the robot led
	 * 
	 * @return The robot led
	 */
	public RobotLED getLed()
	{
		return led;
	}

	/**
	 * Returns the robot movement
	 * 
	 * @return The robot movement
	 */
	public RobotMovement getRobotMovement()
	{
		return movement;
	}

	/**
	 * Returns the raw movements of the Sphero robot
	 * 
	 * @return The raw movements of the robot
	 */
	public RobotRawMovement getRobotRawMovement()
	{
		return rawMovement;
	}

    public RobotStreamListener getListeningThread() {
        return listeningThread;
    }

    public boolean isDisconnecting() {
        return disconnecting;
    }

    public void setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
    }

    public MACRO_SETTINGS getMacroSettings() {
        return macroSettings;
    }

    public RobotSetting getRobotSettings() {
        return rs;
    }
}
