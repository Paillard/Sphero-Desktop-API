package se.nicklasgavelin.bluetooth;

import se.nicklasgavelin.bluetooth.Bluetooth.EVENT.EVENT_CODE;
import se.nicklasgavelin.log.Logging;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnectionNotifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Gives the possibility to find and connect to remote Bluetooth
 * devices. Functionality for creating a server socket will be added
 * in the future.
 *
 * To listen for events on this class implement the "BluetoothDiscoveryListener"
 * class
 * for the classes. The BluetoothDiscoveryListener will receive events happening
 * in this class.
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 * @version 0.1alpha
 */
public class Bluetooth implements DiscoveryListener, Runnable
{
    /**
     * Value for setting communication mode to serial communication
     */
    public static final int SERIAL_COM = 0x1101;

    // Bluetooth
    private final LocalDevice local;
    private DiscoveryAgent dAgent;

    // Listeners
    private Collection<BluetoothDiscoveryListener> listeners;

    // UUID
    private UUID uuid;
    // private String UUID_DEFAULT = "102030405060708090A0B0C0D0E0F010";

    // Temporary stuff
    private Collection<BluetoothDevice> devices;

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device.
     *
     * @param listener The one that will listen for events on this class
     *            instance
     * @param uuid The UUID for the Bluetooth connections
     */
    public Bluetooth(BluetoothDiscoveryListener listener, long uuid) throws BluetoothStateException {
        this(listener, Long.toString(uuid));
    }

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device.
     *
     * @param listener The one that will listen for events on this class
     *            instance
     * @param uuid The UUID for the Bluetooth connections
     */
    public Bluetooth(BluetoothDiscoveryListener listener, int uuid) throws BluetoothStateException {
        this(listener);
        this.uuid = new UUID(uuid);
    }

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device.
     *
     * @param listener The one that will listen for events on this class
     *            instance
     * @param uuid The UUID for the Bluetooth connections
     */
    public Bluetooth(BluetoothDiscoveryListener listener, String uuid) throws BluetoothStateException {
        this(listener);
        this.uuid = new UUID(uuid, false);
    }

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device.
     *
     * @param uuid The UUID for the Bluetooth connections
     */
    public Bluetooth(int uuid) throws BluetoothStateException {
        this(null, uuid);
    }

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device.
     *
     * @param uuid The UUID for the Bluetooth connections
     */
    public Bluetooth(long uuid) throws BluetoothStateException {
        this(null, uuid);
    }

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device.
     *
     * @param uuid The UUID for the Bluetooth connections
     */
    public Bluetooth(String uuid) throws BluetoothStateException {
        this(null, uuid);
    }

    /**
     * Create a Bluetooth instance that uses the build in Bluetooth
     * device. Do NOT use this directly as the UUID has to be set by the
     * other constructors!
     *
     * @param listener The one that will listen for events on this class
     *            instance
     */
    private Bluetooth(BluetoothDiscoveryListener listener) throws BluetoothStateException {
        listeners = new ArrayList<>();

        // Add the listener
        if (listener != null)
            listeners.add(listener);
        try
        {
            // Try to get everything that we need regarding the local
            // bluetooth device
            local = LocalDevice.getLocalDevice();
            dAgent = local.getDiscoveryAgent();
        } catch (BluetoothStateException e) {
            notifyListeners(new Bluetooth.EVENT(e.getMessage(), EVENT_CODE.ERROR_BLUETOOTH_EXCEPTION));
            throw e;
        }
    }

    /**
     * Add listener to the Bluetooth instance
     *
     * @param listener The listener to add
     */
    public void addListener(BluetoothDiscoveryListener listener)
    {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

	/*
	 * *********************************************************************************************************
	 * 
	 * BLUETOOTH LISTENERS
	 * 
	 * ************************************************************************************
	 * *******************
	 */

    /**
     * Remove an active listener from the class
     *
     * @param listener The listener to remove
     */
    public boolean removeListener(BluetoothDiscoveryListener listener)
    {
        return listeners.contains(listener) && listeners.remove(listener);
    }

    /**
     * Notify all listeners with the error event
     *
     * @param error The error code
     */
    private void notifyListeners(Bluetooth.EVENT error)
    {
        for(BluetoothDiscoveryListener l : listeners)
            l.deviceSearchFailed(error);
    }

    /**
     * Notify listeners about new available devices (after a device search)
     *
     * @param devices The available devices
     */
    private void notifyListeners(Collection<BluetoothDevice> devices)
    {
        for(BluetoothDiscoveryListener l : listeners)
            l.deviceSearchCompleted(devices);
    }

    /**
     * Notify listeners about a new available device
     *
     * @param device The available device
     */
    private void notifyListeners(BluetoothDevice device)
    {
        for(BluetoothDiscoveryListener l : listeners)
            l.deviceDiscovered(device);
    }

    /**
     * Notify listeners that a search has been initialized
     */
    private void notifyListenersDiscoveryStarted()
    {
        listeners.forEach(BluetoothDiscoveryListener::deviceSearchStarted);
    }

    /**
     * Searches for devices in the vicinity that we may connect to.
     * Results are returned via the BluetoothListener methods.
     */
    public Thread discover()
    {
        log("Creating discovery thread");

        Thread deviceDiscoveryThread = new Thread(this);
        deviceDiscoveryThread.start();
        return  deviceDiscoveryThread;
    }

	/*
	 * *********************************************************************************************************
	 * 
	 * DEVICE DISCOVERY
	 * 
	 * ************************************************************************************
	 * *******************
	 */

    /**
     * Cancel an ongoing discovery event
     */
    public boolean cancelDiscovery()
    {
        // TODO: Should this really be an error?
        boolean res = dAgent.cancelInquiry(this);
        log("Device discovery canceled");
        notifyListeners(new Bluetooth.EVENT("Device discovery canceled by user", EVENT_CODE.ERROR_DISCOVERY_CANCELED));
        return res;
    }

    /**
     * Searches for devices in the vicinity that we may connect to.
     * Results are returned via the BluetoothListener methods.
     */
    private void performDiscovery()
    {
        assert local != null : "Local must be initialized before the call of that function";
        log("Starting discovery");
        notifyListenersDiscoveryStarted();

        // Clear the previous device list if there is one
        if (devices == null)
            devices = new ArrayList<>();
        else
            devices.clear();

        synchronized(local) //  : synchronize on non final variable -> may have differents threads with different instances of that variable
        {
            try
            {
                log("Starting inquiry");
                dAgent.startInquiry(DiscoveryAgent.GIAC, this); // Fixme null pointer exception on dAgent or startInquiry
                local.wait();
            } catch(BluetoothStateException e) {
                error("Failed to perform discovery, maybe interrupted");
                notifyListeners(new Bluetooth.EVENT("Failed to perform discovery due to exception", EVENT_CODE.ERROR_BLUETOOTH_EXCEPTION));
                // throw new RuntimeException(e.getMessage());
            } catch(InterruptedException e) {
                // Just ignore, we got nothing else to do
            }
        }

        // Go through all devices and set their names if they have one available
		/*
		 * for(BluetoothDevice d : this.devices)
		 * {
		 * // Set the name
		 * try
		 * {
		 * log("Setting device name for " + d);
		 * d.setName(d.getRemoteDevice().getFriendlyName(false));
		 * log("Name for " + d + " is now " + d.getName());
		 * }
		 * catch (IOException e) {}
		 * }
		 */

        // Notify observers
        notifyListeners(devices);
    }

    /**
     * Called when a device is discovered by the current discovery search
     *
     * @param device Discovered device
     * @param deviceClass Device class
     */
    @Override
    public void deviceDiscovered(RemoteDevice device, DeviceClass deviceClass)
    {
        log("Discovered device " + device);
        BluetoothDevice btd = new BluetoothDevice(this, device);
        devices.add(btd);

        // Notify listeners
        notifyListeners(btd);
    }

    /**
     * Called when the discovery is completed. Will unlock any active mutex
     * locks.
     */
    @Override
    public void inquiryCompleted(int arg0)
    {
        log("Discovery completed, notifying synchronized lock");
        synchronized(local)
        {
            local.notifyAll();
        }
    }

    @Override
    public void serviceSearchCompleted(int transId, int respCode)
    {
		/*
		 * log("Service search completed.");
		 *
		 * switch (respCode)
		 * {
		 * case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
		 * log("The service search completed normally");
		 * break;
		 * case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
		 * log("The service search request was cancelled by a call to
		 * DiscoveryAgent.cancelServiceSearch(int)");
		 * break;
		 * case DiscoveryListener.SERVICE_SEARCH_ERROR:
		 * log("An error occurred while processing the request");
		 * break;
		 * case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
		 * log("No records were found during the service search");
		 * break;
		 * case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
		 * log("The device specified in the search request could not be reached
		 * or the local device could not establish a connection to the remote
		 * device");
		 * break;
		 * default:
		 * log("Unknown Response Code - " + respCode);
		 * break;
		 * }
		 */
    }

    @Override
    public void servicesDiscovered(int arg0, ServiceRecord... arg1)
    {
		/*
		 * if (arg1.length == 0) System.out.println("No service record found");
		 * for(ServiceRecord r : arg1)
		 * System.out.println("Service: " +
		 * r.getHostDevice().getBluetoothAddress() +
		 * r.getConnectionURL(ServiceRecord.AUTHENTICATE_NOENCRYPT, false));
		 */
    }

    /**
     * Handles device discovery
     */
    public void run()
    {
        performDiscovery();
    }

	/*
	 * *********************************************************************************************************
	 * 
	 * THREAD
	 * 
	 * ************************************************************************************
	 * *******************
	 */

    /**
     * Starts a local Bluetooth server that listens for connections
     * on the Bluetooth address.
     *
     * @param name The name for the server
     */
    public void startServer(String name)
    {
        try
        {
            log("Setting up connection listener");
            local.setDiscoverable(DiscoveryAgent.GIAC);

            String url = "btspp://localhost:" + uuid + ";name=" + name;
            StreamConnectionNotifier server = (StreamConnectionNotifier) Connector.open(url);
            ServiceRecord record = local.getRecord(server);

            // // set availability to fully available
            record.setAttributeValue(0x0008, new DataElement(DataElement.U_INT_1, 0xFF));

            // // set device class to telephony
            record.setDeviceServiceClasses(0x400000);

            // // set up a service for this record and set it up as the thread
            BluetoothService s = new BluetoothService(null, record);//, this);
            new Thread(s).start();
        } catch (Exception e) {
            notifyListeners(new Bluetooth.EVENT("Failed to setup bluetooth server socket: " + e.getMessage(), EVENT_CODE.ERROR_BLUETOOTH_EXCEPTION));
            // throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Returns the discovery agent
     *
     * @return The discovery agent
     */
    protected DiscoveryAgent getDiscoveryAgent()
    {
        return dAgent;
    }

	/*
	 * *********************************************************************************************************
	 * 
	 * GETTERS
	 * 
	 * ************************************************************************************
	 * *******************
	 */

    /**
     * Returns the pre-set UUID
     *
     * @return The UUID
     */
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * Log a message
     *
     * @param msg The message to log
     */
    private void log(String msg)
    {
        Logging.debug(msg);
    }

	/*
	 * *********************************************************************************************************
	 * 
	 * DEBUG
	 * 
	 * ************************************************************************************
	 * *******************
	 */

    /**
     * Log an error
     *
     * @param msg The error to log
     */
    private void error(String msg)
    {
        Logging.error(msg);
    }

    /**
     * Describes an error for the Bluetooth
     *
     * @author Nicklas Gavelin
     */
    public static class EVENT
    {
        private String message;
        // The error code for the message
        private Bluetooth.EVENT.EVENT_CODE errorCode;

        /**
         * Create an error message
         *
         * @param message The message for the error
         * @param errorCode The error code of the message
         */
        protected EVENT(String message, Bluetooth.EVENT.EVENT_CODE errorCode)
        {
            this.message = message;
            this.errorCode = errorCode;
        }

        /**
         * Returns the error code
         *
         * @return The error code
         */
        public Bluetooth.EVENT.EVENT_CODE getErrorCode()
        {
            return errorCode;
        }

        /**
         * Returns the error message
         *
         * @return The error message
         */
        public String getErrorMessage()
        {
            return message;
        }

        /**
         * The code for each of the possible events
         */
        public enum EVENT_CODE
        {
            /**
             * Event code for when discovery is canceled
             */
            ERROR_DISCOVERY_CANCELED,

            /**
             * Event code for when Bluetooth exception occurs
             */
            ERROR_BLUETOOTH_EXCEPTION
        }
    }//!class EVENT

    @Override
    public String toString() {
        return "Bluetooth{" +
                "local=" + local +
                ", dAgent=" + dAgent +
                ", listeners=" + listeners +
                ", uuid=" + uuid +
                ", devices=" + devices +
                '}';
    }
}
