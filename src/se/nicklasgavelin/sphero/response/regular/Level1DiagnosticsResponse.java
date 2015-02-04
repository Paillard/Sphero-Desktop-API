package se.nicklasgavelin.sphero.response.regular;

import se.nicklasgavelin.sphero.response.ResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage.ResponseHeader;

/**
 * Response for the Level1DiagnosticsResponse
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class Level1DiagnosticsResponse extends ResponseMessage
{
    /**
     * Create a Level1DiagnosticsResponse from the received data
     *
     * @param rh The response header containing the response data
     */
    public Level1DiagnosticsResponse( ResponseHeader rh )
    {
        super( rh );
    }
}
