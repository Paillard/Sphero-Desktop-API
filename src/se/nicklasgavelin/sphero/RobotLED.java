package se.nicklasgavelin.sphero;

import java.awt.*;

/**
 * Manages the RGB and LED brightness information to prevent
 * storing this directly in objects in the Robot instance.
 *
 * @author Nicklas Gavelin
 */
public class RobotLED
{
    private Robot robot_outer_arg;
    // Internal values
    private int red, green, blue;
    private float brightness;

    /**
     * Create a new robot led object
     */
    RobotLED(Robot robot_outer_arg)
    {
        this.robot_outer_arg = robot_outer_arg;
        reset();
    }

    /**
     * Returns the red color value (0-255)
     *
     * @return The red color value
     */
    public int getRGBRed()
    {
        return red;
    }

    /**
     * Returns the green color value (0-255)
     *
     * @return The green color value
     */
    public int getRGBGreen()
    {
        return green;
    }

    /**
     * Returns the blue color value (0-255)
     *
     * @return The blue color value
     */
    public int getRGBBlue()
    {
        return blue;
    }

    /**
     * Returns the RGB Color value for the internal RGB LED
     *
     * @return The color for the RGB LED
     */
    public Color getRGBColor()
    {
        return new Color(red, green, blue);
    }

    /**
     * Returns the ledBrightness of the front led (0-1)
     *
     * @return The ledBrightness level of the front led
     */
    public float getFrontLEDBrightness()
    {
        return brightness;
    }

    /**
     * Resets the internal values to default values
     */
    private void reset()
    {
        // Set white color (default for connected devices)
        red = robot_outer_arg.getRobotSettings().getLedRGB().getRed();
        green = robot_outer_arg.getRobotSettings().getLedRGB().getGreen();
        blue = robot_outer_arg.getRobotSettings().getLedRGB().getBlue();

        // Reset the ledBrightness to 0 (off)
        brightness = robot_outer_arg.getRobotSettings().getLedBrightness();
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getGreen() {
        return green;
    }

    public void setRed(int red) {
        this.red = red;
    }
}
