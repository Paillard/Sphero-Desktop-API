package se.nicklasgavelin.sphero.response.regular;

import se.nicklasgavelin.sphero.response.ResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage.ResponseHeader;

/**
 * The response for the SpinRightCommand
 * 
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class SpinRightResponse extends ResponseMessage
{
	/**
	 * Create a SpinRightResponse from the received data
	 * 
	 * @param rh The response header containing the response data
	 */
	public SpinRightResponse( ResponseHeader rh )
	{
		super( rh );
	}
}
