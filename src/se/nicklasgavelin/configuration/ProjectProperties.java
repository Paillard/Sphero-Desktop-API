/*
 * Please read the LICENSE file that is included with the source
 * code.
 */

package se.nicklasgavelin.configuration;

import se.nicklasgavelin.log.Logging.Level;
import se.nicklasgavelin.sphero.RobotSetting;
import se.nicklasgavelin.sphero.command.RawMotorCommand.MOTOR_MODE;

import java.awt.Color;
import java.util.Properties;

/**
 * Used for returning current configuration settings
 * Settings are stored in project.properties in the same packet as this class.
 * 
 * @author Nicklas Gavelin, nicklas.gavelin@gmail.com, Luleå University of
 *         Technology
 */
public class ProjectProperties extends Properties
{
	private static final long serialVersionUID = 4819632381205752349L;
	private static ProjectProperties instance;

	/**
	 * Create project properties
	 */
	private ProjectProperties()
	{

        try
		{
			// Load the property file
            load( ProjectProperties.class.getResourceAsStream( "project.properties" ) );
		}
		catch( Exception e )
		{
			// Unable to load property file, sorry :(
		}
	}

	/**
	 * Returns default robot settings
	 * 
	 * @return Default robot settings
	 */
	public RobotSetting getRobotSetting()
	{
		return new RobotSetting( new Color( Integer.parseInt(getProperty( "sphero.color.rgb.red", "255" ) ),
                Integer.parseInt(getProperty( "sphero.color.rgb.green", "255" ) ),
                Integer.parseInt(getProperty( "sphero.color.rgb.blue", "255" ) ) ),
                Integer.parseInt(getProperty("sphero.pinginterval", "255") ),
                Float.parseFloat(getProperty("sphero.color.brightness", "1") ),
                Integer.parseInt(getProperty("sphero.motor.heading", "0") ),
                Integer.parseInt(getProperty("sphero.motor.speed", "0") ),
                Integer.parseInt(getProperty("sphero.macro.size", "0") ),
                Integer.parseInt(getProperty( "sphero.macro.storage", "0" ) ),
                Integer.parseInt(getProperty( "sphero.macro.minsize", "128" ) ),
                Boolean.parseBoolean(getProperty("sphero.motor.stop", "true") ),
                Float.parseFloat(getProperty("sphero.macro.rotationrate", "0") ),
                MOTOR_MODE.valueOf(getProperty("sphero.motor.motormode",
                        MOTOR_MODE.FORWARD.toString())));
	}

	/**
	 * Returns size of received buffer
	 * 
	 * @return The size of the received buffer
	 */
	public int getBufferSize()
	{
		return Integer.parseInt(getProperty("sphero.socket.buffersize", "256") );
	}

	/**
	 * Returns the current debug state
	 * 
	 * @return True for on, false for off
	 */
	public boolean getDebugEnabled()
	{
		return Boolean.parseBoolean(getProperty( "debug.enabled", "false" ) );
	}

	/**
	 * Set debug status
	 * 
	 * @param enabled The new debug status (true for on, false otherwise)
	 */
	public void setDebugEnabled( boolean enabled )
	{
        setProperty( "debug.enabled", Boolean.toString( enabled ) );
	}

	/**
	 * Returns the current bluecove debug state
	 * 
	 * @return The current bluecove debug state
	 */
	public boolean getBluecoveDebugEnabled()
	{
		return Boolean.parseBoolean(getProperty( "debug.bluecove.enabled", "false" ) );
	}

	/**
	 * Set bluecove debug status
	 * 
	 * @param enabled New debug status
	 */
	public void setBluecoveDebugEnabled( boolean enabled )
	{
        setProperty( "debug.bluecove.enabled", Boolean.toString( enabled ) );
	}

	/**
	 * Returns the logger name
	 * 
	 * @return The logger name
	 */
	public String getLoggerName()
	{
		return getProperty( "debug.loggername", "se.nicklasgavelin" );
	}

	/**
	 * Set the name of the logger
	 * WILL NOT WORK AFTER THE LOGGER HAVE BEEN INITIALIZED
	 * 
	 * @param name The new name for the logger
	 */
	public void setLoggerName( String name )
	{
        setProperty( "debug.loggername", name );
	}

	/**
	 * Returns the current debug level,
	 * default level is Logging.Level.FATAL
	 * 
	 * @return The set debug level
	 */
	public Level getDebugLevel()
	{
		return Level.valueOf(getProperty( "debug.level", Level.FATAL.toString() ) );
	}

	/**
	 * Returns the properies instance
	 * 
	 * @return The property instance
	 */
	public static ProjectProperties getInstance()
	{
		// Check if we have a previous instance
		if( instance == null )
			instance = new ProjectProperties();

		return instance;
	}
}
