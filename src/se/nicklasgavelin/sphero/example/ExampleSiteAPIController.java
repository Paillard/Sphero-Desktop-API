package se.nicklasgavelin.sphero.example;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import se.nicklasgavelin.bluetooth.Bluetooth;
import se.nicklasgavelin.bluetooth.BluetoothDevice;
import se.nicklasgavelin.bluetooth.BluetoothDiscoveryListener;
import se.nicklasgavelin.sphero.Robot;
import se.nicklasgavelin.sphero.RobotListener;
import se.nicklasgavelin.sphero.command.CommandMessage;
import se.nicklasgavelin.sphero.exception.RobotBluetoothException;
import se.nicklasgavelin.sphero.response.InformationResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage;

import javax.bluetooth.BluetoothStateException;
import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Created by paill on 07/02/15.
 */
public class ExampleSiteAPIController implements BluetoothDiscoveryListener, Initializable, RobotListener {

    @FXML
    BorderPane root;
    @FXML
    ListView<BluetoothDevice> bluetoothDeviceListView;
    @FXML
    ListView<Robot> robotListView;
    @FXML
    Button scanButton, connectButton, disconnectButton;
    @FXML
    Label addressLabel, connectionURLLabel, idLabel, nameLabel,
            stoppedLabel, leftMotorSpeedLabel, rightMotorSpeedLabel,
            rightMotorModeLabel, leftMotorModeLabel;

    private ObservableList<BluetoothDevice> bluetoothDeviceObservableList;
    private ObservableList<Robot> robotObservableList;
    private ExampleSiteAPI api;
    private BluetoothDevice selectedBluetoothDevice;
    private long responses;
    // private Bluetooth bt;

    public void initialize(URL location, ResourceBundle resources) {
        // be sure to initiate here anything controller is controlling
        api = ExampleSiteAPI.getInstance();
//        bt = new Bluetooth(this, Bluetooth.SERIAL_COM);
        bluetoothDeviceObservableList = FXCollections.observableArrayList();
        bluetoothDeviceListView.setItems(bluetoothDeviceObservableList);
        robotObservableList = FXCollections.observableArrayList();
        robotListView.setItems(robotObservableList);

        // Set events on buttons
        scanButton.setOnMouseClicked(event -> discover());
        connectButton.setOnMouseClicked(event -> connect());
        disconnectButton.setOnMouseClicked(event -> disconnectSelected());

        // add listeners to the change in the list of bluetooth devices
        bluetoothDeviceListView.getProperties().addListener((MapChangeListener.Change<?, ?> change) -> setCanConnect());
        robotListView.getProperties().addListener((MapChangeListener<? super Object, ? super Object>) c -> {
            setCanDisconnect();
            updateLabels();
        });

        root.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                disconnect();
                api.stage.close();
            }
        });

        // configure initial buttons's state
        connectButton.setDisable(true);
        disconnectButton.setDisable(true);
    }

    private void updateLabels() {
        Robot r = robotListView.getFocusModel().getFocusedItem();
        if (r != null) {
            nameLabel.setText(r.getName());
            addressLabel.setText(r.getAddress());
            idLabel.setText(r.getId());
            stoppedLabel.setText(Boolean.toString(r.isStopped()));
            connectionURLLabel.setText(r.getConnectionURL());
            leftMotorSpeedLabel.setText(Integer.toString(r.getRobotRawMovement().getLeftMotorSpeed()));
            leftMotorModeLabel.setText(r.getRobotRawMovement().getRightMotorMode().toString());
            rightMotorSpeedLabel.setText(Integer.toString(r.getRobotRawMovement().getRightMotorSpeed()));
            rightMotorModeLabel.setText(r.getRobotRawMovement().getRightMotorMode().toString());
        } else {
            nameLabel.setText("");
            addressLabel.setText("");
            idLabel.setText("");
            stoppedLabel.setText("");
            connectionURLLabel.setText("");
            leftMotorSpeedLabel.setText("");
            leftMotorModeLabel.setText("");
            rightMotorSpeedLabel.setText("");
            rightMotorModeLabel.setText("");
        }
    }

    private void setCanConnect() {
        BluetoothDevice bluetoothDevice = bluetoothDeviceListView.getFocusModel().getFocusedItem();
        connectButton.setDisable(bluetoothDevice == null);
        setSelectedBluetoothDevice(bluetoothDevice);
        api.root.getProperties().addListener((MapChangeListener.Change<?, ?> change) -> setCanDisconnect());
    }

    private void setCanDisconnect() {
        disconnectButton.setDisable(!isSelectedBluetoothDeviceConnected());
    }

    /**
     * Called when the device search is completed with detected devices
     *
     * @param devices The devices detected
     */
    @Override
    public void deviceSearchCompleted(Collection<BluetoothDevice> devices) {
        // Device search is completed
        System.out.println("Completed device discovery");
        for (BluetoothDevice bd: devices) {
            if (!bluetoothDeviceObservableList.contains(bd))
                bluetoothDeviceObservableList.add(bd);
        }
    }

    /**
     * Called when the search is started
     */
    @Override
    public void deviceSearchStarted() {
        System.out.println("Started device search");
    }

    /**
     * Called if something went wrong with the device search
     *
     * @param error The error that occurred
     */
    @Override
    public void deviceSearchFailed(Bluetooth.EVENT error) {
        System.err.println("Failed with device search: " + error.getErrorMessage());
    }

    /**
     * Called when a Bluetooth device is discovered
     *
     * @param device The device discovered
     */
    @Override
    public void deviceDiscovered(BluetoothDevice device) {
        System.out.println("Discovered device " + device.getName() + " : " + device.getAddress());
    }

    /**
     * Connect to the selected bluetooth device.
     */
    public void connect() {
        assert selectedBluetoothDevice != null : "Bluetooth device is needed to connect";
        // Create the robot from the bluetooth device
        scanButton.setDisable(true);
        connectButton.setDisable(true);
        try {
            // FIXME try with a new bluetoothDevice #2
            // selectedBluetoothDevice.setConnectionUrl(String.format("btspp://%s:1;authenticate=true;encrypt=false;master=false", selectedBluetoothDevice.getAddress()));
            // BluetoothDevice nbtd = new BluetoothDevice(bt, String.format("btspp://%s:1;authenticate=true;encrypt=false;master=false", selectedBluetoothDevice.getAddress()));
            // Robot r = new Robot(nbtd); // FIXME seems to come from Robot already using 3 Threads #3
            Robot r = new Robot(
                    new BluetoothDevice(
                            new Bluetooth( this, Bluetooth.SERIAL_COM ),
                            String.format("btspp://%s:1;authenticate=true;encrypt=false;master=false", selectedBluetoothDevice.getAddress())));
            if (r.connect()) {
                System.out.println("Connected");
                // Add ourselves as listeners
                r.addListener(this);
                if (robotObservableList.filtered(robot -> robot.getAddress().equals(selectedBluetoothDevice.getAddress())).isEmpty()) {
                    robotObservableList.add(r);
                    System.out.println("added Robot to list");
                }
            } else {
                System.err.println("Failed to connect");
            }
        } catch (RobotBluetoothException | BluetoothStateException e) {
            System.err.println(e);
        }
        scanButton.setDisable(false);
        connectButton.setDisable(false);
    }

    public boolean isSelectedBluetoothDeviceConnected() {
        if (robotObservableList != null && !robotObservableList.isEmpty()) {
            Robot selectedRobot = robotListView.getFocusModel().getFocusedItem();
            if (selectedRobot != null) {
                FilteredList<Robot> flr = robotObservableList.filtered(robot -> robot.getAddress().equals(selectedBluetoothDevice.getAddress()));
                Robot r = flr.stream().findFirst().get();
                if (r != null) return r.isConnected();
            }
        }
        return false;
    }

    /**
     * Called when a response is received from a robot
     *
     * @param r The robot the event concerns
     * @param response The response received
     * @param dc The command the response is concerning
     */
    @Override
    public void responseReceived(Robot r, ResponseMessage response, CommandMessage dc) {
        System.out.println("(" + ++responses + ") Received response: " + response.getResponseCode() + " to message " + dc.getCommand());
    }

    /**
     * Event that may occur for a robot
     *
     * @param r The robot the event concerns
     * @param code The event code for the event
     */
    @Override
    public void event(Robot r, EVENT_CODE code) {
        System.out.println("Received event: " + code);
    }

    @Override
    public void informationResponseReceived(Robot r, InformationResponseMessage response) {
        // Information response (Ex. Sensor data)
        System.out.println(String.format("%s respond following informations: %s", r.toString(), response.toString()));
    }

    /**
     *
     * @param bluetoothDevice
     */
    public void setSelectedBluetoothDevice(BluetoothDevice bluetoothDevice) {
        selectedBluetoothDevice = bluetoothDevice;
    }

    /**
     * Stop everything regarding the connection and robots
     */
    private void disconnect() {
        System.out.println("Stopping Thread");
        /*if (bt != null) {
            System.out.println("Stopping bluetooth");
            System.out.println("remove from listeners: " + bt.removeListener(this));
            System.out.println("cancel discovery: " + bt.cancelDiscovery());
            BlueCoveImpl.shutdown();
            // FIXME work if scan but no connection established
            // FIXME doesn't work if scan, connection, ESC or disconnect then ESC
            BlueCoveImpl.shutdownThreadBluetoothStack();
        }*/
        // Disconnect from all robots and clear the connected list
        robotObservableList.stream().forEach(r -> r.removeListener(this));
        robotObservableList.stream().forEach(Robot::disconnect);
        robotObservableList.clear();
        bluetoothDeviceObservableList.stream().forEach(BluetoothDevice::cancelDiscovery);
        bluetoothDeviceObservableList.clear();
    }

    private void disconnectSelected() {
        assert robotObservableList != null && !robotObservableList.isEmpty() : "No device to deconnect";
        assert selectedBluetoothDevice != null : "No bluetooth device selected";

        Robot r = robotObservableList.stream().filter(robot -> robot.getAddress().equals(selectedBluetoothDevice.getAddress())).findFirst().get();
        r.removeListener(this);
        r.disconnect();
        robotObservableList.remove(r);
        selectedBluetoothDevice.cancelDiscovery();
        updateLabels();
    }
    /**
     * Will start a research of all available and visible
     * bluetooth devices.
     */
    public void discover() {
        try {
            Bluetooth bt = new Bluetooth(this, Bluetooth.SERIAL_COM);
            bt.discover().join(); // FIXME create a new thread from runnable bluetooth then return it
        } catch (BluetoothStateException | InterruptedException e) {
            System.err.println(e);
        }
    }

}
