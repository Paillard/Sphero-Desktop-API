package se.nicklasgavelin.sphero;

import se.nicklasgavelin.bluetooth.BluetoothConnection;
import se.nicklasgavelin.log.Logging;
import se.nicklasgavelin.sphero.command.CommandMessage;
import se.nicklasgavelin.util.ByteArrayBuffer;
import se.nicklasgavelin.util.Pair;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles the sending of commands to the active robot.
 * Manages multiple queues (one timer and one sending queue). The
 * sending queue is for sending direct messages and the timer queue
 * is used to schedule commands to be sent after a certain delay
 * or with periodic transmissions.
 *
 * @author Nicklas Gavelin
 */
class RobotSendingQueue extends Timer
{
    private Robot robot_outer_arg;
    // Internal storage
    private boolean stop, stopAccepting;
    private final BluetoothConnection btc;
    // Writer & queue that the writer uses
    private Writer writer;
    private final BlockingQueue<Pair<CommandMessage, Boolean>> sendingQueue;

    /**
     * Create a robot stream writer for a specific Bluetooth connection
     *
     * @param btc The Bluetooth connection to send to
     */
    protected RobotSendingQueue(Robot robot_outer_arg, BluetoothConnection btc)
    {
        this.robot_outer_arg = robot_outer_arg;
        this.btc = btc;
        sendingQueue = new LinkedBlockingQueue<>();
        writer = new Writer();

        startWriter();
    }

    /**
     * Start the writer thread.
     * The writer will motorStop at the same time as the RobotSendinQueue is
     * stopped.
     */
    private void startWriter()
    {
        writer.start();
    }

    /**
     * Forces a command to be sent even if the stopAccepting flag
     * is set to true. The command sent will be a system command
     *
     * @param command The command to enqueue
     */
    public void forceCommand(CommandMessage command)
    {
        sendingQueue.add(new Pair<>(command, true));
    }

    /**
     * Enqueue a single command to be sent as soon as possible without using
     * the timer objects that are often used to enqueue commands to be sent
     * after a certain delay.
     *
     * @param command The command to send
     * @param systemCommand True if the command is a system command, false
     *            otherwise
     */
    public void enqueue(CommandMessage command, boolean systemCommand)
    {
        synchronized(sendingQueue)
        {
            try
            {
                if (!stop && !stopAccepting)
                    sendingQueue.put(new Pair<>(command, systemCommand));
            }
            catch(InterruptedException e)
            {
                System.err.println("InterruptedException from: " + Thread.currentThread() + ": " + e);
            }
        }
    }

    /**
     * Enqueue a single command to be sent as soon as possible without using
     * the timer objects that are often used to enqueue commands to be sent
     * after a certain delay. The command will be sent as a SYSTEM command
     * and will not notify any RobotListeners after a response is received!
     *
     * @param command The command to send
     * @param delay The delay to send the command after (in ms)
     */
    public void enqueue(CommandMessage command, float delay)
    {
        enqueue(command, delay, false);
    }

//		/**
//		 * Enqueue a command with a certain repeat period and initial delay
//		 * before sending the
//		 * first message. <b>The message will be repeated as long as the writer
//		 * allows it</b>.
//		 *
//		 * @param command The command to transmit
//		 * @param initialDelay The initial delay before sending the first one
//		 * @param periodLength The period length between the transmissions
//		 */
//		public void enqueue(CommandMessage command, float initialDelay, float periodLength)
//		{
//			this.enqueue(command, false, initialDelay, periodLength);
//		}

    /**
     * Enqueue a command with a certain repeat period and initial delay
     * before sending the
     * first message. <b>The message will be repeated as long as the writer
     * allows it</b>.
     *
     * @param command The command to send
     * @param systemCommand True for a system command, false otherwise
     * @param initialDelay The initial delay for sending
     * @param periodLength The period length between transmissions
     */
    public void enqueue(CommandMessage command, boolean systemCommand, float initialDelay, float periodLength)
    {
        if (!stop && !stopAccepting)
            schedule(new CommandTask(new Pair<>(command, systemCommand)), (long) initialDelay, (long) periodLength);
    }

    /**
     * Enqueue an already existing command task to run at a certain initial
     * delay and
     * a certain period length
     *
     * @param task The task to run after the timer runs
     * @param delay The delay before running the task in milliseconds
     */
    private void enqueue(CommandTask task, float delay)
    {
        if (!stop && !stopAccepting)
            schedule(task, (long) delay);
    }

    /**
     * Enqueue a single command to be sent after a specific delay
     *
     * @param command The command to send
     * @param delay The delay to send after (in ms)
     * @param systemCommand True if the command is a system command, false
     *            otherwise
     */
    public void enqueue(CommandMessage command, float delay, boolean systemCommand)
    {
        if (!stop && !stopAccepting)
            schedule(new CommandTask(new Pair<>(command, systemCommand)), (long) delay);
    }

    /**
     * Stops the current timer. Will not be possible to restart it once
     * this method is run!
     */
    @Override
    public void cancel()
    {
        stopAccepting = true;
        super.cancel();
    }

    /**
     * Stop everything
     */
    public void stopAll()
    {
        stop = true;
    }

    public Thread getWriter() {
        return writer;
    }

    /**
     * Handles the transmission of a single command
     *
     * @author Nicklas Gavelin
     */
    private class CommandTask extends TimerTask
    {
        // Storage of the command to send
        private Pair<CommandMessage, Boolean> execute;
        private int repeat;
        private float delay;
        private boolean repeating;

        /**
         * Create a command task to send a command
         *
         * @param execute The command together with a boolean value
         *            describing if it's a system message or not
         */
        private CommandTask(Pair<CommandMessage, Boolean> execute)
        {
            this.execute = execute;
        }

        /**
         * Create a command task with a repeated number of runs
         *
         * @param execute The command to execute
         * @param delay The delay between the commands
         * @param repeat The number of repeats to perform (-1 for infinite
         *            repeats)
         */
        private CommandTask(Pair<CommandMessage, Boolean> execute, float delay, int repeat)
        {
            this(execute);
            this.repeat = repeat;
            this.delay = delay;

            if (repeat != -1) // Infinite command, will be sent until the end of
                // time!
                repeating = true;
        }

        @Override
        public void run()
        {
            // Enqueue the command directly to the writer
            enqueue(execute.getFirst(), execute.getSecond());

            // Check if we want to repeat the command
            if (repeating)
            {
                if (repeat == -1 || --repeat > 0)
                    enqueue(this, delay);
            }
        }
    }

    /**
     * Handles all transmissions to the Sphero device.
     *
     * @author Nicklas Gavelin
     */
    private class Writer extends Thread
    {
        @Override
        public void run()
        {
            ByteArrayBuffer sendingBuffer = new ByteArrayBuffer(256);

            // Run until we manually motorStop the thread or
            // a connection error occurs.
            while(!stop)
            {
                try
                {
                    // Fetch a message from the sending queue and append the data of that packet to our
                    // sending buffer. We will then try to add more data to our sending buffer.
                    Pair<CommandMessage, Boolean> p = sendingQueue.take(); // FIXME #4

                    // Append message to sending buffer
                    sendingBuffer.append(p.getFirst().getPacket(), 0, p.getFirst().getPacketLength());

                    // Add command to listening queue
                    robot_outer_arg.getListeningThread().enqueue(p);
                    // sent.add(p);

                    Logging.debug("Queueing " + p.getFirst());

                    // Lock until we have sent our messages in-case someone
                    // else tries to do access our sendingQueue at the same time (enqueue)
                    synchronized(sendingQueue)
                    {
                        try
                        {
                            // We will try to send as much as we can
                            if (!sendingQueue.isEmpty())
                            {
                                // Go through all the messages that we can
                                for(int i = 0; i < sendingQueue.size(); i++)
                                {
                                    Pair<CommandMessage, Boolean> c = sendingQueue.peek();

                                    // Peek at the the rest of the messages
                                    int length = c.getFirst().getPacketLength();

                                    // Check that we have enough space to add the next
                                    // message to, if not
                                    // send what we got and continue later on
                                    if (sendingBuffer.length() - length < 0)
                                        break;

                                    // Enqueue the next command
                                    sendingBuffer.append(c.getFirst().getPacket(), 0, c.getFirst().getPacketLength());
                                    robot_outer_arg.getListeningThread().enqueue(c);
                                    // sent.add(c);
                                    sendingQueue.remove();

                                    Logging.debug("Queueing " + c.getFirst());
                                }
                            }

                            // Write to socket
                            Logging.debug("Sending " + sendingBuffer);
                            btc.write(sendingBuffer.toByteArray());
                            btc.flush();

                            // update(sent);
                        }
                        catch(IOException e)
                        {
                            // Close unexpectedly
                            if (robot_outer_arg.isConnected())
                                Logging.fatal("Writing thread closed down unexpectedly", e);
                            robot_outer_arg.connectionClosedUnexpected();
                        }
                        finally
                        {
                            sendingBuffer.clear();
                        }
                    }
                }
                catch(InterruptedException e)
                {
                    System.err.println("interruptedException: "+ Thread.currentThread());
                } // Nothing important, just continue on
            }
        }
    }
}
