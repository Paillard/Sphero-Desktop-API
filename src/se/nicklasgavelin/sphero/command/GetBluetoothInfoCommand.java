package se.nicklasgavelin.sphero.command;

import se.nicklasgavelin.sphero.command.CommandMessage.COMMAND_MESSAGE_TYPE;

/**
 * A command to get Bluetooth information from the Sphero
 *
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class GetBluetoothInfoCommand extends CommandMessage
{
    /**
     * Create a GetBluetoothInfoCommand
     */
    public GetBluetoothInfoCommand()
    {
        super( COMMAND_MESSAGE_TYPE.GET_BLUETOOTH_INFO );
    }
}
