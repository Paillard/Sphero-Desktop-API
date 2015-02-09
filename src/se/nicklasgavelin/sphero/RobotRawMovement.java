package se.nicklasgavelin.sphero;

import se.nicklasgavelin.sphero.command.RawMotorCommand;

/**
 * Raw movement values for the robot. These have no
 * connection to the ordinary robot movement RobotMovement as
 * these use direct commends to the engines instead of pre-defined
 * commands.
 *
 * @author Nicklas Gavelin
 */
public class RobotRawMovement
{
    private Robot robot_outer_arg;
    // Holds motor speed and mode (Forward, Reverse)
    private int leftMotorSpeed, rightMotorSpeed;
    private RawMotorCommand.MOTOR_MODE leftMotorMode, rightMotorMode;

    /**
     * Create a new raw robot movement
     */
    RobotRawMovement(Robot robot_outer_arg)
    {
        this.robot_outer_arg = robot_outer_arg;
        reset();
    }

    /**
     * Returns the left motor speed
     *
     * @return The left motor speed
     */
    public int getLeftMotorSpeed()
    {
        return leftMotorSpeed;
    }

    /**
     * Returns the right motor speed
     *
     * @return The right motor speed
     */
    public int getRightMotorSpeed()
    {
        return rightMotorSpeed;
    }

    /**
     * Returns the left motor mode
     *
     * @return The left motor mode (Forward/Reverse)
     */
    public RawMotorCommand.MOTOR_MODE getLeftMotorMode()
    {
        return leftMotorMode;
    }

    /**
     * Returns the right motor mode
     *
     * @return The right motor mode (Forward/Reverse)
     */
    public RawMotorCommand.MOTOR_MODE getRightMotorMode()
    {
        return rightMotorMode;
    }

    /**
     * Resets the internal values.
     * WARNING: WILL NOT SEND _ANY_ COMMANDS TO THE SPHERO DEVICE, THIS
     * HAS TO BE DONE MANUALLY!
     */
    private void reset()
    {
        leftMotorSpeed = rightMotorSpeed = robot_outer_arg.getRobotSettings().getMotorStartSpeed();
        leftMotorMode = rightMotorMode = robot_outer_arg.getRobotSettings().getMotorMode();
    }

    public void setRightMotorSpeed(int rightMotorSpeed) {
        this.rightMotorSpeed = rightMotorSpeed;
    }

    public void setLeftMotorSpeed(int leftMotorSpeed) {
        this.leftMotorSpeed = leftMotorSpeed;
    }

    public void setRightMotorMode(RawMotorCommand.MOTOR_MODE rightMotorMode) {
        this.rightMotorMode = rightMotorMode;
    }

    public void setLeftMotorMode(RawMotorCommand.MOTOR_MODE leftMotorMode) {
        this.leftMotorMode = leftMotorMode;
    }
}
