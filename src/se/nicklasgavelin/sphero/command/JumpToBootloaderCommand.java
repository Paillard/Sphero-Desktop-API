package se.nicklasgavelin.sphero.command;

import se.nicklasgavelin.sphero.command.CommandMessage.COMMAND_MESSAGE_TYPE;

/**
 * Command to set the robot in bootloader application. Will result in a lost
 * connection to the Sphero
 * 
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of Technology
 */
public class JumpToBootloaderCommand extends CommandMessage
{
	/**
	 * Create a JumpToBootloaderCommand to send to the Sphero
	 */
	public JumpToBootloaderCommand()
	{
		super( COMMAND_MESSAGE_TYPE.JUMP_TO_BOOTLOADER );
	}
}
