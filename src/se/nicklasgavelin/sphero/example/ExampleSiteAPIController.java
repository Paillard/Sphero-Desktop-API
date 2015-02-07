package se.nicklasgavelin.sphero.example;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import se.nicklasgavelin.bluetooth.Bluetooth;
import se.nicklasgavelin.bluetooth.BluetoothDevice;
import se.nicklasgavelin.bluetooth.BluetoothDiscoveryListener;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Created by paill on 07/02/15.
 */
public class ExampleSiteAPIController implements BluetoothDiscoveryListener, Initializable {

    @FXML
    ListView<BluetoothDevice> devicesLV;
    @FXML
    Button scanButton;
    @FXML
    Button connectButton;
    @FXML
    Button disconnectButton;
    ObservableList<BluetoothDevice> devicesNames;
    ExampleSiteAPI api;

    public void initialize(URL location, ResourceBundle resources) {
        // be sure to initiate here anything controller is controlling
        api = ExampleSiteAPI.getInstance();
        api.setBluetooth(new Bluetooth(this, Bluetooth.SERIAL_COM));
        devicesNames = FXCollections.observableArrayList();
        devicesLV.setItems(devicesNames);

        // Set events on buttons
        scanButton.setOnMouseClicked(event -> api.discover());
        connectButton.setOnMouseClicked(event -> api.connect());
        disconnectButton.setOnMouseClicked(event -> api.disconnectSelected());

        // add listeners to the change in the list of bluetooth devices
        devicesLV.getProperties().addListener((MapChangeListener.Change<?, ?> change) -> setCanConnect());
        devicesLV.getProperties().addListener((MapChangeListener.Change<?, ?> change) -> setCanDisconnect());

        // configure initial buttons's state
        connectButton.setDisable(true);
        disconnectButton.setDisable(true);
    }

    private void setCanConnect() {
        BluetoothDevice bluetoothDevice = devicesLV.getFocusModel().getFocusedItem();
        connectButton.setDisable(bluetoothDevice == null);
        api.setSelectedBluetoothDevice(bluetoothDevice);
        disconnectButton.setDisable(!api.isSelectedBluetoothDeviceConnected());
    }

    private void setCanDisconnect() {
        disconnectButton.setDisable(!api.isSelectedBluetoothDeviceConnected());
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
            devicesNames.add(bd);
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
}
