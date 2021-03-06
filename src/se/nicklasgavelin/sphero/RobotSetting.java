/*
 * Please read the LICENSE file that is included with the source
 * code.
 */

package se.nicklasgavelin.sphero;

import se.nicklasgavelin.sphero.command.RawMotorCommand.MOTOR_MODE;
import se.nicklasgavelin.util.Value;

import java.awt.*;

/**
 * 
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of
 *         Technology
 */
public class RobotSetting
{
	private Color ledRGB;
	private float ledBrightness, motorRotationRate;
	private int socketPingInterval, motorHeading, motorStartSpeed,
			macroMaxSize, macroRobotStorageSize, macroMinSpaceSize;
	private boolean motorStop;
	private MOTOR_MODE motorMode;

	/**
	 * Create a robot setting
	 * 
	 * @param ledRGB Initial color
	 * @param socketPinginterval Ping interval
	 * @param ledBrightness Brightness level
	 * @param motorHeading Heading
	 * @param motorStartSpeed Start speed
	 * @param macroMaxMacroSize Max macro size
	 * @param macroRobotStorageSize Storage volume on robot for macro
	 * @param macroMinSpaceSendSize Min macro size to send
	 * @param motorStop True or false
	 * @param motorRotationrate Rotation rate
	 * @param motorMode Motor mode
	 */
	public RobotSetting( Color ledRGB, int socketPinginterval, float ledBrightness, int motorHeading, int motorStartSpeed, int macroMaxMacroSize, int macroRobotStorageSize, int macroMinSpaceSendSize, boolean motorStop, float motorRotationrate, MOTOR_MODE motorMode )
	{
		this.ledRGB = ledRGB;
        this.socketPingInterval = Value.clamp( socketPinginterval, 1000, 120000 );
		this.ledBrightness = Value.clamp( ledBrightness, 0, 1 );
		this.motorHeading = Value.clamp( motorHeading, 0, 359 );
		this.motorStartSpeed = Value.clamp( motorStartSpeed, 0, 255 );
        this.macroMaxSize = Value.clamp( macroMaxMacroSize, 50, 240 );
		this.macroRobotStorageSize = Value.clamp( macroRobotStorageSize, 256, 1000 );
        this.macroMinSpaceSize = Value.clamp( macroMinSpaceSendSize, 50, 240 );
		this.motorStop = motorStop;
        this.motorRotationRate = Value.clamp( motorRotationrate, 0, 1 );
		this.motorMode = motorMode;
	}

    public MOTOR_MODE getMotorMode() {
        return motorMode;
    }

    public int getMotorStartSpeed() {
        return motorStartSpeed;
    }

    public int getMacroMinSpaceSize() {
        return macroMinSpaceSize;
    }

    public int getMacroMaxSize() {
        return macroMaxSize;
    }

    public int getMacroRobotStorageSize() {
        return macroRobotStorageSize;
    }

    public int getMotorHeading() {
        return motorHeading;
    }

    public float getMotorRotationRate() {
        return motorRotationRate;
    }

    public boolean getMotorStop() {
        return motorStop;
    }

    public Color getLedRGB() {
        return ledRGB;
    }

    public float getLedBrightness() {
        return ledBrightness;
    }

    public int getSocketPingInterval() {
        return socketPingInterval;
    }
}
