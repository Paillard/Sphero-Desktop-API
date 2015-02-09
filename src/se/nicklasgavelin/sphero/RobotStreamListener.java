package se.nicklasgavelin.sphero;

import se.nicklasgavelin.bluetooth.BluetoothConnection;
import se.nicklasgavelin.configuration.ProjectProperties;
import se.nicklasgavelin.log.Logging;
import se.nicklasgavelin.sphero.command.CommandMessage;
import se.nicklasgavelin.sphero.response.InformationResponseMessage;
import se.nicklasgavelin.sphero.response.ResponseMessage;
import se.nicklasgavelin.sphero.response.regular.GetBluetoothInfoResponse;
import se.nicklasgavelin.util.Pair;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Handles the listening for the connected robot
 *
 * @author Nicklas Gavelin
 */
class RobotStreamListener extends Thread
{
    private Robot robot_outer_argument;
    // Thread motorStop/continue
    private boolean stop;
    // Bluetooth connection to use
    private BluetoothConnection btc;
    // Queue for commands that are waiting for responses
    private LinkedList<Pair<CommandMessage, Boolean>> waitingForResponse;

    /**
     * Create a listener from the Bluetooth connection
     *
     * @param btc The Bluetooth connection
     */
    public RobotStreamListener(Robot robot_outer_argument, BluetoothConnection btc)
    {
        this.robot_outer_argument = robot_outer_argument;
        this.btc = btc;
        waitingForResponse = new LinkedList<>();
    }

    /**
     * Enqueue a command that are waiting for a response from the device
     *
     * @param cmd The pair of the command and the flag that tells if it's a
     *            system command or not
     */
    protected void enqueue(Pair<CommandMessage, Boolean> cmd)
    {
        waitingForResponse.add(cmd);
    }

    /**
     * Stop the actively running thread
     */
    public void stopThread()
    {
        stop = true;
    }

    private byte[] linkedToArray(List<Byte> list)
    {
        byte[] d = new byte[ list.size() ];
        for(int i = 0; i < list.size(); i++)
            d[i] = list.get(i);

        // for (int i = 0; list.size() > 0; i++)
        // d[ i] = list.remove(0);

        return d;
    }

    /**
     * Runs the listening of the socket
     */
    @Override
    public void run()
    {
        // ByteArrayBuffer buf = new ByteArrayBuffer(BUFFER_SIZE);

        // Create a data array that contains all our read
        // data.
        byte[] data = new byte[ ProjectProperties.getInstance().getBufferSize() ];
        LinkedList<Byte> buffer = new LinkedList<>();

        // Run until we manually motorStop the thread
        while(!stop)
        {
            try
            {
                int read = btc.read(data);
                if (read == -1)
                    throw new IOException("Reached end of stream");

                // Append all newly read values to our buffer
                // These values may only be the header or may as well be
                // multiple messages depending on how much we could read this time
                for(int k = 0; k < read; k++)
                    buffer.add(data[k]);

                // Now we will continue to read until we got the whole message
                // if we already have the whole message, skip this part
                for(int dataLength = 0; buffer.size() < ResponseMessage.RESPONSE_HEADER_LENGTH + dataLength;)
                {
                    // We need to read more of the data input to get a complete
                    // message
                    // Now read once again until we reach the end of the message
                    read = btc.read(data); // Store it in the data array as
                    // earlier

                    // Append the read data to our data list
                    for(int k = 0; k < read; k++)
                        buffer.add(data[k]);

                    // Now we have read a number of bytes and may have the complete
                    // message
                    // But we will check so that we have read equal to the complete
                    // message
                    // length or more (more messages than one is fine as long as we
                    // have at least one
                    // complete message)
                    if (buffer.size() > ResponseMessage.PAYLOAD_LENGTH_INDEX)
                    {
                        // We check the length of the packet by reading the length
                        // index
                        // These indexes are the same both for information and regular
                        // packets
                        // so it's fine selecting whatever index that we need
                        dataLength = linkedToArray(buffer)[ResponseMessage.PAYLOAD_LENGTH_INDEX];
                    }
                }

                // Now we have at least one fine packet
                // convert the linked list to an array that we can read from (easier
                // reading)
                byte[] nData = linkedToArray(buffer);

                // Now we have our read data, the next step is to start reading
                // messages until
                // we have read all completed messages in the array, after we have
                // read all messages
                // we will dump all remaining data in the buffer once again and then
                // we will continue
                // reading from the top again
                int read2 = 0; // Read to point in array
                for(int pointer = 0; pointer < buffer.size() && nData.length - pointer >= ResponseMessage.RESPONSE_HEADER_LENGTH && nData.length - pointer >= ResponseMessage.RESPONSE_HEADER_LENGTH + nData[pointer + ResponseMessage.PAYLOAD_LENGTH_INDEX];)
                {
                    // Now the above restrictions make these things come true
                    // 1. Our current position in the buffer array (nData) is not more
                    // than our buffer size
                    // 2. Our current message length is above that of the header
                    // length of a message (We got a header to read)
                    // 3. Our current message length is above that of the header
                    // length + the packet length (We got a complete packet to read)
                    // These restriction makes us able to read a COMPLETED message and
                    // not only the header part of the message

                    // Now we will start by creating an object for our header
                    // The header will select our specific message values such as
                    // response code and type
                    // and also the length of the contained data
                    ResponseMessage.ResponseHeader drh = new ResponseMessage.ResponseHeader(nData, pointer);

                    // Check the type of the response,
                    // Regular response is messages received after sending a command
                    // to the device
                    // Information response is messages received as an effect of
                    // sending a specific command that
                    // sets the Sphero to keep sending information for some given
                    // reason
                    switch (drh.getResponseType())
                    {
                    /* Regular response message */
                        case REGULAR:
                            // We have received the message as an action that depends
                            // on a message
                            // we sent earlier, now check which message that this
                            // response corresponds to
                            Pair<CommandMessage, Boolean> cmd = waitingForResponse.remove();

                            // Fetch the type of command that we sent, this is used
                            // for debugging purposes
                            CommandMessage.COMMAND_MESSAGE_TYPE cmdType = cmd.getFirst().getCommand();

                            // The command that we sent will act as the decider for
                            // which type of response that
                            // we received. The response we create is in fact the
                            // response which corresponds to the
                            // command that we sent, although it's an super type that
                            // we extend for increased functionality
                            ResponseMessage response = ResponseMessage.valueOf(cmd.getFirst(), drh);

                            // Print some debug information that will help us if we
                            // end up with trouble later on
                            Logging.debug("Received response packet: " + response + (cmd.getSecond() ? " as a SYSTEM RESPONSE" : ""));

                            // Update internal values if we got an OK response code
                            // from the robot
                            // on the command that we sent. We use a switch case
                            // instead of an if/elseif for nicer looking code ;-)
                            switch (drh.getResponseCode())
                            {
                            /*
                             * Code OK, nothing went wrong with the command that we
                             * sent
                             */
                                case CODE_OK:
                                    // Update the internal settings for the robot with
                                    // the response stuff that we have received
                                    robot_outer_argument.updateInternalValues(cmd.getFirst());
                                    break;
                                default:
                                    Logging.error("Received response code " + drh.getResponseCode() + " for " + cmdType);
                                    break;
                            }

                            // Check if we sent the command as a system command
                            // (command sent by the inner classes or robot class for
                            // setting
                            // up the device itself and not by the user)
                            if (cmd.getSecond()) // System command
                            {
                                // The sent command is a system command
                                // Check which type of command to see if we need to
                                // update something internal
                                switch (cmdType)
                                {
                                /*
                                 * A bluetooth information message that returns
                                 * information about the bluetooth
                                 * connection
                                 */
                                    case GET_BLUETOOTH_INFO:
                                        // Check that the response is OK so that we
                                        // can do something with our data
                                        if (drh.getResponseCode().equals(ResponseMessage.RESPONSE_CODE.CODE_OK))
                                        {
                                            // Update Sphero name
                                            GetBluetoothInfoResponse gb = (GetBluetoothInfoResponse) response;
                                            if (!gb.isCorrupt())
                                                robot_outer_argument.setRobotName(gb.getName());
                                            break;
                                        }
                                        break;
                                    case RGB_LED_OUTPUT:
                                        if (robot_outer_argument.isDisconnecting())
                                        {
                                            if (cmd.getFirst().getCommand().equals(CommandMessage.COMMAND_MESSAGE_TYPE.RGB_LED_OUTPUT))
                                            {
                                                // Notify
                                                // We are disconnecting
                                                robot_outer_argument.setDisconnecting(false);
                                                stopThread();
                                            }
                                        }
                                        break;
                                }
                            }
                            else
                            // Notify user
                            {
                                // The sent command is a user sent command that we
                                // need to notify the user about
                                robot_outer_argument.notifyListenersDeviceResponse(response, cmd.getFirst());
                            }
                            break;

                        /* Information response message */
                        case INFORMATION:
                            // Check if we got a OK response code so that we can read
                            // the message that we received
                            // Otherwise we need to throw away the message
                            switch (drh.getResponseCode())
                            {
                            /* OK response code, message is fine */
                                case CODE_OK:
                                    // Now create our message from the data that we
                                    // have received
                                    InformationResponseMessage dir = InformationResponseMessage.valueOf(drh);

                                    if (!dir.isCorrupt())
                                    {
                                        // Message content is OK and we can send the
                                        // data onwards for handling
                                        switch (dir.getInformationResponseType())
                                        {
                                        /* Data message, contains sensor data */
                                        /* Emit macro message */
                                            case EMIT:
                                                if (robot_outer_argument.getMacroSettings().getMacroRunning())
                                                {
                                                    // We have a macro running and
                                                    // received an emit message
                                                    // now we want to continue sending
                                                    // any data that is left
                                                    // for transmission regarding a
                                                    // macro
                                                    if (!robot_outer_argument.getMacroSettings().getBallMemory().isEmpty())
                                                    {
                                                        // Remove the size of the last
                                                        // macro that we have
                                                        // allocated for the macro
                                                        // data
                                                        // as the robot has a limited
                                                        // amount of memory for macro
                                                        // storage
                                                        robot_outer_argument.getMacroSettings().getBallMemory().removeIf(Predicate.isEqual(robot_outer_argument.getMacroSettings().getBallMemory().toArray()));
                                                    }

                                                    // Transmit any remaining macro
                                                    // data now that we got more
                                                    // memory on the device
                                                    robot_outer_argument.getMacroSettings().emptyMacroCommandQueue();
                                                    robot_outer_argument.getMacroSettings().stopIfFinished();
                                                }
                                                break;

                                            /*
                                             * Data message and any other type of
                                             * message
                                             */
                                            case DATA:
                                                // Notify listeners about a received
                                                // data message
                                                robot_outer_argument.notifyListenersInformationResponse(dir);
                                                break;

                                            /*
                                             * Not implemented type of information
                                             * message received, ignore it and log
                                             * this
                                             * occurrence
                                             */
                                            default:
                                                // Logging.error(
                                                // "Unkown type of information message was received "
                                                //);
                                                break;
                                        }
                                    }
                                    else
                                        // Received a corrupt message code for some
                                        // reason, log the instance
                                        Logging.error("Received corrupt information response message " + dir);
                                    break;
                            }
                            break;

                        /* Unknown response code received */
                        default:
                            // Logging.error("Unkown response type received: " +
                            // drh.getResponseType());
                            break;
                    }

                    // Now we need to move our pointer forward so that
                    // we may continue to read any other messages that we have read in
                    // our
                    // buffer array, but first check which type of header that we need
                    // to use for calculating the complete packet length
                    int headerLength = drh.getResponseType().equals(ResponseMessage.ResponseHeader.RESPONSE_TYPE.INFORMATION) ? ResponseMessage.INFORMATION_RESPONSE_HEADER_LENGTH : ResponseMessage.RESPONSE_HEADER_LENGTH;

                    // Add the current packet length to the data pointer
                    read2 = pointer += drh.getPayloadLength() + headerLength;
                }

                // Now we need to clear our data array and add any data that
                // we couldn't read cause it was incomplete to our buffer
                // for handling when we have read more information
                buffer.clear();

                // Add the remaining data to the buffer by reading
                // from our abandoned position
                for(; read2 < nData.length; read2++)
                    buffer.add(nData[read2]);
            }
            catch(NullPointerException e)
            {
                System.err.println("Thread: " + Thread.currentThread() + ": "+ e);
                Logging.error("NullPointerException", e);
            }
            catch(NoSuchElementException e)
            {
                System.err.println("Thread: " + Thread.currentThread() + ": "+ e);
                Logging.error("NoSuchElementException", e);
            }
            catch(Exception e)
            {
                if (robot_outer_argument.isConnected())
                    Logging.fatal("Listening thread closed down unexpectedly", e);
                robot_outer_argument.connectionClosedUnexpected();
            }
        }
    }
}//!class RobotStreamListener
