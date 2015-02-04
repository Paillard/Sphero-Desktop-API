package se.nicklasgavelin.sphero;

import se.nicklasgavelin.util.Value;

/**
 * Directly copied from Orbotix code
 * 
 * @author Orbotix
 */
public class JoyStickDriveAlgorithm extends DriveAlgorithm
{
	private final double center_x;
	private final double center_y;

	public JoyStickDriveAlgorithm( double padWidth, double padHeight )
	{
        center_x = padWidth / 2.0D;
        center_y = padHeight / 2.0D;
	}

	@Override
	public void convert( double x, double y, double unused )
	{
		double x_length = x - center_x;
		double y_length = center_y - y;

		if(center_x > center_y)
			x_length *= center_y / center_x;
		else if(center_x < center_y)
			y_length *= center_x / center_y;

		if(center_x > 0.0D && center_y > 0.0D)
		{
            speed = Math.sqrt( x_length * x_length + y_length * y_length ) / Math.min(center_x, center_y);
            speed = Value.clamp(speed, 0.0D, 1.0D ) * speedScale;
		}
		else
            speed = 0.0D;

        heading = Math.atan2( x_length, y_length );// TODO verify SuspiciousNameCombination: invert x/y?
		if(heading < 0.0D )
            heading += 6.283185307179586D;

        heading *= 57.295779513082323D;

        this.postOnConvert();
	}
}