package se.nicklasgavelin.sphero.example;

import se.nicklasgavelin.bluetooth.Bluetooth;
import se.nicklasgavelin.bluetooth.Bluetooth.EVENT;
import se.nicklasgavelin.bluetooth.BluetoothDevice;
import se.nicklasgavelin.bluetooth.BluetoothDiscoveryListener;
import se.nicklasgavelin.sphero.Robot;
import se.nicklasgavelin.sphero.RobotListener;
import se.nicklasgavelin.sphero.command.CommandMessage;
import se.nicklasgavelin.sphero.command.FrontLEDCommand;
import se.nicklasgavelin.sphero.command.Level1DiagnosticsCommand;
import se.nicklasgavelin.sphero.response.InformationResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

/**
 * Simple test class to test the Sphero API
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class Example_Site_API// extends JFrame TODO : go to javaFX
{
    // Internal storage
    private int responses; // @FIXME : never assigned

    /**
     * Main method
     *
     * @param args Will be ignored
     */
    @SuppressWarnings( "unused" )
    public static void main( String... args )
    {
        new Example_Site_API();
    }

    /**
     * Our example application
     */
    public Example_Site_API() {
        System.out.println("Going to interact with a Sphero");
        ConnectThread ct = new ConnectThread();
        ct.start();
    }

    /**
     * Handles the detection of new devices and listens on our robots for
     * responses and events
     */
    private class ConnectThread extends Thread implements BluetoothDiscoveryListener, RobotListener
    {
        private Bluetooth bt;
        private Collection<Robot> robots;

        /**
         * Create a connect thread
         */
        public ConnectThread()
        {
            robots = new ArrayList<>();
            bt = new Bluetooth(this, Bluetooth.SERIAL_COM);
            try {
                bt.discover().join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        /**
         * Stop everything regarding the connection and robots
         */
        private void stopThread()
        {
            System.out.println("Stopping Thread");
            if(bt != null)
                bt.cancelDiscovery();

            // Disconnect from all robots and clear the connected list
            for(Robot r : robots) {
                System.out.println("Disconnecting " + r);
                r.disconnect();
            }
            robots.clear();
            System.exit(0);
        }

        @Override
        public void run()
        {
            try
            {
                Robot r = null;
                Scanner kb = new Scanner(System.in);

                // Start client interface with sphero
                while (true) {
                    System.out.println("### What to do? ###");
                    String cmd = kb.nextLine().toLowerCase().trim();
                    switch (cmd) {
                        case "discover":
                            try {
                                bt.discover().join();
                            } catch (InterruptedException e) {
                                //e.printStackTrace();
                            }
                            break;
                        case "disconnect":
                            if (r != null && r.isConnected())
                                r.disconnect();
                            break;
                        case "robot":
                            // connect directly to a given Sphero
                            System.out.println("### Please enter the address of your sphero ###");
                            String bluetoothAddress = kb.nextLine().toLowerCase().trim(); // TODO : check address format
                            BluetoothDevice btd = new BluetoothDevice(bt,
                                    String.format("btspp://%s:1;authenticate=true;encrypt=false;master=false", bluetoothAddress));

                            // Create the robot from the bluetooth device
                            r = new Robot(btd);
                            System.out.println("Robot created");
                        case "connect":
                            // Try to connect to the robot
                            if (r != null && r.connect()) {
                                System.out.println("Connected");

                                // Add ourselves as listeners
                                r.addListener(this);
                                robots.add(r);
                            }
                            else
                                System.err.println("Failed to connect");
                            break;
                        case "exit":
                            stopThread();
                            System.exit(0);
                        case "diagnostic":
                            if (r != null && r.isConnected())
                                r.sendCommandAfterMacro(new Level1DiagnosticsCommand());
                            break;
                        case "reset":
                            if (r != null && r.isConnected())
                                r.resetHeading();
                            break;
                        case "calibrate":
                            if (r != null && r.isConnected())
                                r.calibrate(0);
                            break;
                        case "transition":
                            if (r != null && r.isConnected())
                                r.rgbTransition(0, 255, 0, 255, 0, 0, 100);
                            break;
                        case "breath":
                            if (r != null && r.isConnected())
                                r.rgbBreath(new Color(0, 255, 0), new Color(255, 0, 0), 100, 0);
                            break;
                        case "info":
                            if (r != null && r.isConnected()) {
                                System.out.println(String.format("address:\t%s\nurl:\t%s\n\nid:\t%s\nname:\t%s\n",
                                        r.getAddress(), r.getConnectionURL(), r.getId(), r.getName()));
                                System.out.println(String.format("stopped?:\t%s\nleft motor speed:\t%d\nright motor speed:\t%d\nled:\t%s\n",
                                        r.isStopped(), r.getRobotRawMovement().getLeftMotorSpeed(), r.getRobotRawMovement().getRightMotorSpeed(), r.getLed().toString()));
                            }
                            break;
                        case "rotate":
                            if (r != null && r.isConnected())
                                r.rotate(180);
                            break;
                        case "roll":
                            if (r != null && r.isConnected())
                                r.roll(0F, 0.2F);
                            break;
                        case "frontled":
                            if (r != null && r.isConnected())
                                r.sendCommand(new FrontLEDCommand(1));
                            break;
                        default:
                            System.out.println("Existing commands: exit, diagnostic, reset, calibrate, transition, breath, info" +
                                    "disconnect, start, robot, exit, diagnostic, roll, rotate, frontled");
                    }
                } // !while
            }
            catch( Exception e )
            {
                // Failure in searching for devices for some reason.
                e.printStackTrace();
            }
        }

		/*
		 * *************************************
		 * BLUETOOTH DISCOVERY STUFF
		 */

        /**
         * Called when the device search is completed with detected devices
         *
         * @param devices The devices detected
         */
        @Override
        public void deviceSearchCompleted( Collection<BluetoothDevice> devices )
        {
            // Device search is completed
            System.out.println( "Completed device discovery" );

            // Try and see if we can find any Spheros in the found devices
            // Check if the Bluetooth device is a Sphero device or not
            devices.stream()/*.filter(Robot::isValidDevice)*/.forEach(d -> System.out.println("Found robot " + d.getAddress()));
        }

        /**
         * Called when the search is started
         */
        @Override
        public void deviceSearchStarted()
        {
            System.out.println( "Started device search" );
        }

        /**
         * Called if something went wrong with the device search
         *
         * @param error The error that occurred
         */
        @Override
        public void deviceSearchFailed( EVENT error )
        {
            System.err.println("Failed with device search: " + error.getErrorMessage());
        }

        /**
         * Called when a Bluetooth device is discovered
         *
         * @param device The device discovered
         */
        @Override
        public void deviceDiscovered( BluetoothDevice device )
        {
            System.out.println( "Discovered device " + device.getName() + " : " + device.getAddress() );
        }

		/*
		 * ********************************************
		 * ROBOT STUFF
		 */

        /**
         * Called when a response is received from a robot
         *
         * @param r The robot the event concerns
         * @param response The response received
         * @param dc The command the response is concerning
         */
        @Override
        public void responseReceived( Robot r, ResponseMessage response, CommandMessage dc )
        {
            System.out.println( "(" +responses + ") Received response: " + response.getResponseCode() + " to message " + dc.getCommand() );
        }

        /**
         * Event that may occur for a robot
         *
         * @param r The robot the event concerns
         * @param code The event code for the event
         */
        @Override
        public void event( Robot r, EVENT_CODE code ) {
            System.out.println( "Received event: " + code );
        }

        @Override
        public void informationResponseReceived( Robot r, InformationResponseMessage response ) {
            // Information response (Ex. Sensor data)
            System.out.println(String.format("%s respond following informations: %s", r.toString(), response.toString()));
        }
    }
}
