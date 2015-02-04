package se.nicklasgavelin.sphero.response.regular;

import se.nicklasgavelin.sphero.response.ResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage.ResponseHeader;

public class SetDataStreamingResponse extends ResponseMessage
{
	/**
	 * Create a set data streaming response message
	 * 
	 * @param rh The response header containing the response data
	 */
	public SetDataStreamingResponse( ResponseHeader rh )
	{
		super( rh );
	}
}
