package se.nicklasgavelin.sphero.response.regular;

import se.nicklasgavelin.sphero.response.ResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage.ResponseHeader;

/**
 * Response for the GetConfigurationBlockCommand
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class GetConfigurationBlockResponse extends ResponseMessage
{
    /**
     * Create a GetCOnfigurationBlockResponse from the received data
     *
     * @param rh The response header containing the response data
     */
    public GetConfigurationBlockResponse( ResponseHeader rh )
    {
        super( rh );
    }
}
