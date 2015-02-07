package se.nicklasgavelin.sphero.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import se.nicklasgavelin.bluetooth.Bluetooth;
import se.nicklasgavelin.bluetooth.BluetoothDevice;
import se.nicklasgavelin.sphero.Robot;
import se.nicklasgavelin.sphero.RobotListener;
import se.nicklasgavelin.sphero.command.CommandMessage;
import se.nicklasgavelin.sphero.exception.RobotBluetoothException;
import se.nicklasgavelin.sphero.response.InformationResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple test class to test the Sphero API
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class ExampleSiteAPI extends Application implements RobotListener
{
    // Internal storage
    private int responses;
    private Bluetooth bt;
    private Map<BluetoothDevice, Robot> robots;
    private BluetoothDevice selectedBluetoothDevice;
    public Parent root;

    private static ExampleSiteAPI exampleSiteAPI;

    /**
     * Get the instance of the application
     * @return the instance of the application
     */
    public static ExampleSiteAPI getInstance() {
        return exampleSiteAPI;
    }

    /**
     * Main method
     */
    public static void main(String... args) {
        launch(args);
    }

    /**
     *
     * @param bt
     */
    public void setBluetooth(Bluetooth bt) {
        this.bt = bt;
    }

    /**
     *
     * @param bluetoothDevice
     */
    public void setSelectedBluetoothDevice(BluetoothDevice bluetoothDevice) {
        selectedBluetoothDevice = bluetoothDevice;
    }

    @Override
    public void start(Stage stage) throws Exception {
        exampleSiteAPI = this;
        robots = new ConcurrentHashMap<>();

        root = FXMLLoader.load(getClass().getResource("exampleSiteAPI.fxml"));

        Scene scene = new Scene(root, 300, 275);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                disconnect();
                stage.close();
            }
        });
        stage.setTitle("Sphero Desktop Control");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Stop everything regarding the connection and robots
     */
    private void disconnect() {
        System.out.println("Stopping Thread");
        if(bt != null)
            bt.cancelDiscovery();

        // Disconnect from all robots and clear the connected list
        robots.forEach((bluetoothDevice, robot) -> {
            robot.disconnect();
            robot.removeListener(this);
            bluetoothDevice.cancelDiscovery();
        });
        robots.clear();
    }

    public void disconnectSelected() {
        assert robots != null && !robots.isEmpty() : "No device to deconnect";
        assert selectedBluetoothDevice != null : "No bluetooth device selected";

        Robot r = robots.get(selectedBluetoothDevice);
        r.disconnect();
        r.removeListener(this);
        robots.remove(selectedBluetoothDevice);
        selectedBluetoothDevice.cancelDiscovery();
    }

 /*   @Deprecated
    public void terminal() {
        try {
            Robot r = null;
            Scanner kb = new Scanner(System.in);

            // Start client interface with sphero
            String cmd = "";
            boolean run = true;
            while (run) {
                switch (cmd) {
                    case "diagnostic":
                        if (r != null && r.isConnected())
                            r.sendCommandAfterMacro(new Level1DiagnosticsCommand());
                        break;
                    case "reset":
                        if (r != null && r.isConnected()) {
                            r.resetHeading();
                            r.setRGBLedColor(new Color(0xFF00));
                            r.setFrontLEDBrightness(0F);
                            r.setRotationRate(1F);
                        }
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
                            r.rgbBreath(new Color(0xff00), new Color(0x0000ff), 100, 0);
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
                        if (r != null && r.isConnected()) {
                            System.out.println("### enter rotation degres (0-360) ###");
                            try {
                                int rot = Math.abs(kb.nextInt() % 360);
                                r.rotate(rot);
                                r.setRGBLedColor(new Color(0xFF00));
                                Thread.sleep(500);
                                r.setRGBLedColor(new Color(38, 240, 255));
                            } catch (InputMismatchException ime) {
                                System.err.println("You should have enter a number");
                                r.setRGBLedColor(new Color(0xFF0000));
                                Thread.sleep(500);
                                r.setRGBLedColor(new Color(38, 240, 255));
                            }
                        }
                        break;
                    case "trip":
                        if (r != null && r.isConnected()) {
                            for (int red = 0; red < 256; red += 5) {
                                for (int green = 0; green < 256; green += 5) {
                                    for (int blue = 0; blue < 256; blue += 5) {
                                        r.setRGBLedColor(new Color(red, green, blue));
                                        Thread.sleep(100);
                                    }
                                }
                            }
                        }
                        break;
                    case "360":
                        if (r != null && r.isConnected()) {
                            for (int i = 0; i < 360; i += 10) {
                                r.rotate(i);
                                Thread.sleep(100);
                            }
                        }
                        break;
                    case "color":
                        if (r != null && r.isConnected()) {
                            try {
                                System.out.println("### enter red value (0 - 255) ###");
                                int red = Math.abs(kb.nextInt() % 255);
                                System.out.println("### enter green value (0 - 255) ###");
                                int green = Math.abs(kb.nextInt() % 255);
                                System.out.println("### enter blue value (0 - 255) ###");
                                int blue = Math.abs(kb.nextInt() % 255);
                                r.setRGBLedColor(new Color(red, green, blue));
                            } catch (InputMismatchException ime) {
                                System.err.println("You should have enter a number");
                                r.setRGBLedColor(new Color(0xFF0000));
                                Thread.sleep(500);
                                r.setRGBLedColor(new Color(38, 240, 255));
                            }
                        }
                        break;
                    case "roll":
                        if (r != null && r.isConnected()) {
                            try {
                                System.out.println("### enter heading (rotation 0 - 360) (float) ###");
                                float heading = Math.abs(kb.nextFloat() % 360);
                                System.out.println("### enter speed (0 - 1) (float) ###");
                                float speed = Math.abs(kb.nextFloat());
                                if (speed >= 0F && speed < 1F) {
                                    System.out.println("### enter wanted number of iterations ###");
                                    int repeat =   Math.abs(kb.nextInt() % 1000);
                                    for (int i = 0; i < repeat; i++)
                                        r.roll(heading, speed);
                                    r.setRGBLedColor(new Color(0xFF00));
                                    Thread.sleep(500);
                                    r.setRGBLedColor(new Color(38, 240, 255));
                                } else {
                                    r.setRGBLedColor(new Color(0xFF0000));
                                    Thread.sleep(500);
                                    r.setRGBLedColor(new Color(38, 240, 255));
                                }
                            } catch (InputMismatchException ime) {
                                System.err.println("You should have enter a number");
                                r.setRGBLedColor(new Color(0xFF0000));
                                Thread.sleep(500);
                                r.setRGBLedColor(new Color(38, 240, 255));
                            }
                        }
                        break;
                    case "frontled":
                        if (r != null && r.isConnected())
                            r.sendCommand(new FrontLEDCommand(1));
                        break;
                    default:
                        System.out.println("Existing commands: exit, diagnostic, reset, calibrate, transition, breath, info" +
                                "disconnect, start, robot, exit, diagnostic, roll, rotate, frontled, color, trip, 360"); // TODO change for methods tab pointer
                }//!switch
                if (run) {
                    System.out.println("### What to do? ###");
                    cmd = kb.nextLine().toLowerCase().trim();
                }
            } // !while
        } catch(Exception e) {
            // Failure in searching for devices for some reason.
            e.printStackTrace();
        }
        System.out.println("end of run");
    }//!run*/

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
     * Will start a research of all available and visible
     * bluetooth devices.
     */
    public void discover() {
        assert bt != null : "Bluetooth should have been initialized before calling for search";

        bt.discover();
    }

    /**
     * Connect to the selected bluetooth device.
     */
    public void connect() {
        assert selectedBluetoothDevice != null : "Should pick up an address of one device";

        // Create the robot from the bluetooth device
        try {
            Robot r = new Robot(selectedBluetoothDevice);
            if (r.connect()) {
                System.out.println("Connected");

                // Add ourselves as listeners
                r.addListener(this);
                if (!robots.containsKey(selectedBluetoothDevice))
                    robots.put(selectedBluetoothDevice, r);
            } else {
                System.err.println("Failed to connect");
            }
        } catch (RobotBluetoothException e) {
            System.err.println(e);
        }
    }

    public BluetoothDevice getSelectedBluetoothDevice() {
        return selectedBluetoothDevice;
    }

    public boolean isSelectedBluetoothDeviceConnected() {
        if (robots != null && !robots.isEmpty()) {
            if (selectedBluetoothDevice != null) {
                Robot r = robots.get(selectedBluetoothDevice);
                if (r != null) return r.isConnected();
            }
        }
        return false;
    }
}
