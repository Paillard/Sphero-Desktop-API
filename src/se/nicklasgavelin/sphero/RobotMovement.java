package se.nicklasgavelin.sphero;

/**
 * Holds the robot position, rotation rate and the drive algorithm used.
 * All the internal values may be accessed with the get methods that are
 * available.
 *
 * @author Nicklas Gavelin
 */
public class RobotMovement
{
    private Robot robot_outer_arg;
    // The current values
    private float heading, velocity, rotationRate;
    private boolean stop = true;
    // The current drive algorithm that is used for calculating velocity
    // and motorHeading when running .drive no Robot
    private DriveAlgorithm algorithm;

    /**
     * Create a new robot movement object
     */
    RobotMovement(Robot robot_outer_arg)
    {
        this.robot_outer_arg = robot_outer_arg;
        reset();
    }

    /**
     * Returns the current motorHeading of the robot
     *
     * @return The current motorHeading of the robot (0-360)
     */
    public float getHeading()
    {
        return heading;
    }

    /**
     * Returns the current velocity of the robot
     *
     * @return The current velocity (0-1)
     */
    public float getVelocity()
    {
        return velocity;
    }

    /**
     * Returns the current rotation rate of the robot.
     *
     * @return The current rotation rate (0-1)
     */
    public float getRotationRate()
    {
        return rotationRate;
    }

    /**
     * Returns the current motorStop value of the robot.
     * True means the robot is stopped, false means it's
     * moving with a certain velocity
     *
     * @return True if moving, false otherwise
     */
    public boolean getStop()
    {
        return stop;
    }

    /**
     * Returns the current drive algorithm that is used to
     * calculate the velocity and motorHeading when running .drive on Robot
     *
     * @return The current drive algorithm
     */
    public DriveAlgorithm getDriveAlgorithm()
    {
        return algorithm;
    }

    /**
     * Resets all values of the class instance.
     * Will NOT send any commands to the robot, this has to be
     * done manually! BE SURE TO DO IT!
     */
    private void reset()
    {
        heading = robot_outer_arg.getRobotSettings().getMotorHeading();
        velocity = robot_outer_arg.getRobotSettings().getMotorStartSpeed();
        rotationRate = robot_outer_arg.getRobotSettings().getMotorRotationRate();
        stop = robot_outer_arg.getRobotSettings().getMotorStop();
        algorithm = new RCDriveAlgorithm();
    }

    public void setDriveAlgorithm(DriveAlgorithm driveAlgorithm) {
        this.algorithm = driveAlgorithm;
    }

    public void setRotationRate(float rotationRate) {
        this.rotationRate = rotationRate;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
