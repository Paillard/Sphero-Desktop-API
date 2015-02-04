/*
 * Please read the LICENSE file that is included with the source
 * code.
 */

package se.nicklasgavelin.sphero.response.information;

import se.nicklasgavelin.sphero.response.InformationResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage.ResponseHeader;

/**
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class DataResponse extends InformationResponseMessage
{
	/**
	 * Response message for sensor data
	 * 
	 * @param rh The response header
	 */
	public DataResponse( ResponseHeader rh )
	{
		super( rh );
	}

	/**
	 * Returns the data received in the data message
	 * 
	 * @return The data for the sensors
	 */
	public byte[] getSensorData()
	{
		return this.getMessageHeader().getPacketPayload();
	}

	/**
	 * Returns the length of the sensor data
	 * 
	 * @return The sensor data length
	 */
	public int getSensorDataLength()
	{
		return this.getPacketPayload().length;
	}
}
