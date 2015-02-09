package se.nicklasgavelin.sphero;

/**
 * Base abstract class for algorithms that are used to calculate heading, speed
 * and any other necessary parameter for driving the Sphero robot directly using
 * the Robot.drive method.
 * 
 * Directly copied from Orbotix code
 * 
 * @author Orbotix
 */
public abstract class DriveAlgorithm
{
	// Internal storage
	/**
	 * Number of maximum coordinates
	 */
	public static final int MAX_COORDINATES = 3;
	protected double heading;
	protected double headingOffset;
	protected double adjustedHeading;
	protected double speed;
	protected double speedScale;
	protected double[] stopPosition;
	protected double[] deadZoneDelta;
	protected OnConvertListener convertListener;

	/**
	 * Create a drive algorithm and set default values
	 */
	public DriveAlgorithm()
	{
        headingOffset = 0.0D;
        speedScale = 1.0D;
        stopPosition = new double[3];
        deadZoneDelta = new double[3];
	}

	/**
	 * Set the listener for the conversion
	 * 
	 * @param listener The listener
	 */
	public void setOnConvertListener( OnConvertListener listener)
	{
        convertListener = listener;
	}

	/**
	 * Convert x, y, z values into heading and speed values
	 * 
	 * @param x The x coordinate
	 * @param y The y coordinate
	 * @param z The z coordinate
	 */
	public abstract void convert( double x, double y, double z);

	/**
	 * Notify the listener about a conversion success
	 */
	protected void postOnConvert()
	{
		if (convertListener == null)
			return;
        convertListener.onConvert(heading, speed, speedScale);
	}

	/**
	 * Modify to the correct heading (offset and maximum)
	 */
	protected void adjustHeading()
	{
        adjustedHeading = heading + headingOffset;
		if (adjustedHeading >= 360.0D)
            adjustedHeading %= 360.0D;
	}

    public double getAdjustedHeading() {
        return adjustedHeading;
    }

    public void setAdjustedHeading(double adjustedHeading) {
        this.adjustedHeading = adjustedHeading;
    }

    public double getSpeed() {
        return speed;
    }

    public void setHeadingOffset(double headingOffset) {
        this.headingOffset = headingOffset;
    }

    /**
	 * Listener class,
	 * listens for conversion events
	 */
    public interface OnConvertListener
	{
		/**
		 * Event called when the conversion of the values are done
		 * 
		 * @param paramDouble1 x
		 * @param paramDouble2 y
		 * @param paramDouble3 z
		 */
        void onConvert(double paramDouble1, double paramDouble2, double paramDouble3);
	}
}