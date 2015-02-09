package se.nicklasgavelin.sphero;

import se.nicklasgavelin.sphero.command.*;
import se.nicklasgavelin.sphero.macro.MacroCommand;
import se.nicklasgavelin.sphero.macro.MacroObject;
import se.nicklasgavelin.sphero.macro.command.Emit;
import se.nicklasgavelin.util.ByteArrayBuffer;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Manages transmission of Macro commands and allows for
 * streaming possibilities (dividing macro into pieces and sending them off
 * one by one until the whole macro has been transmitted and played)
 *
 * @author Orbotix
 * @author Nicklas Gavelin
 */
class MACRO_SETTINGS
{
    private Robot robot_outer_arg;
    private final Collection<MacroCommand> commands;
    private final ThreadLocal<Collection<CommandMessage>> sendingQueue;
    private final Collection<Integer> ballMemory;
    private boolean macroRunning, macroStreamingEnabled;
    private int emits;

    /**
     * Create a macro settings object
     */
    MACRO_SETTINGS(Robot robot_outer_arg)
    {
        this.robot_outer_arg = robot_outer_arg;
        commands = new ArrayList<>();
        sendingQueue = new ThreadLocal<Collection<CommandMessage>>() {
            @Override
            protected Collection<CommandMessage> initialValue() {
                return new ArrayList<>();
            }
        };
        ballMemory = new ArrayList<>();
        macroRunning = false;
        macroStreamingEnabled = true;
    }

    /**
     * Stop any current macros from running
     */
    void stopMacro()
    {
        // Abort the current macro
        robot_outer_arg.sendCommand(new AbortMacroCommand());

        // Clear the memory
        commands.clear();
        ballMemory.clear();

        // Set motorStop flag
        macroRunning = false;
    }

    /**
     * Stop macro from executing (finished)
     */
    protected void stopIfFinished()
    {
        emits = emits > 0 ? emits - 1 : 0;
        if (commands.isEmpty() && macroRunning && emits == 0)
        {
            stopMacro();

            // Notify listeners about macro done event
            robot_outer_arg.notifyListenerEvent(RobotListener.EVENT_CODE.MACRO_DONE);
        }
    }

    /**
     * Play a given macro object
     *
     * @param macro The given macro object
     */
    void playMacro(MacroObject macro)
    {
        if (macro.getMode().equals(MacroObject.MacroObjectMode.Normal))
        {
            // Normal macro mode
            robot_outer_arg.sendSystemCommand(new SaveTemporaryMacroCommand(1, macro.generateMacroData()));
            robot_outer_arg.sendSystemCommand(new RunMacroCommand(-1));
        }
        else
        {
            if (!macroStreamingEnabled) {
                System.err.println("macro Streaming disabled");
                return;
            }

            if (macro.getMode().equals(MacroObject.MacroObjectMode.CachedStreaming))
            {
                // Cached streaming mode
                if (!macro.getCommands().isEmpty())
                {
                    // Get all macro commands localy instead
                    // this.commands.clear();
                    commands.addAll(macro.getCommands());

                    macroRunning = true;

                    // Now empty our queue
                    emptyMacroCommandQueue();

                    // if (!this.macroRunning)
                    // {
                    // this.macroRunning = true;
                    // // this.sendSystemCommand(new RunMacroCommand(-2));
                    // }
                } else System.err.println("Command is empty");
            }
        }
    }

    /**
     * Send a command after a CachedStreaming macro has run
     *
     * @param command The command to send after the macro is finished
     *            running
     */
    public void sendCommandAfterMacro(CommandMessage command)
    {
        sendingQueue.get().add(command);
    }

    /**
     * Clears the queue used for storing commands to send after the macro
     * has finished running
     */
    public void clearSendingQueue()
    {
        sendingQueue.get().clear();
    }

    /**
     * Continue emptying the macro command queue by creating new commands
     * and sending them to the Sphero device
     */
    synchronized void emptyMacroCommandQueue()
    {
        // Calculate number of free bytes that we have
        int ballSpace = freeBallMemory(), freeBytes = ballSpace > robot_outer_arg.getRobotSettings().getMacroMaxSize() ? robot_outer_arg.getRobotSettings().getMacroMaxSize() : ballSpace, chunkSize = 0;

        // Check if we need or can create more commands
        if (commands.isEmpty() || ballSpace <= robot_outer_arg.getRobotSettings().getMacroMinSpaceSize())
            return;

        // Create our sending collection (stuff that we want to send)
        Collection<MacroCommand> send = new ArrayList<>();

        // Emit marker (we will receive a message from the Sphero when this emit
        // marker is reached)
        Emit em = new Emit(1);
        int emitLength = em.getLength();

        // Go through new commands that we want to send
        for(MacroCommand cmd : commands)
        {
            // Check if we allow for the new command to be added (that we still got
            // enough space left to add it)
            if (freeBytes - (chunkSize + cmd.getLength() + emitLength) <= 0 || chunkSize + cmd.getLength() + emitLength > robot_outer_arg.getRobotSettings().getMacroMaxSize())
                break;

            // Add the command to the send queue and increase the space we've used
            send.add(cmd);
            chunkSize += cmd.getLength();
        }

        // Remove the commands that we can send from the waiting command queue
        commands.removeAll(send);

        // Add emitter
        send.add(em);
        chunkSize += em.getLength();

        // Create our sending buffer to add commands to
        ByteArrayBuffer sendBuffer = new ByteArrayBuffer(chunkSize);

        // Add all commands to the buffer
        for(MacroCommand cmd : send)
            sendBuffer.append(cmd.getByteRepresentation());

        if (this.commands.isEmpty())
            sendBuffer.append(MacroCommand.MACRO_COMMAND.MAC_END.getValue());

        ballMemory.add(chunkSize);

        // Send a save macro command to the Sphero with the new data
        SaveMacroCommand svc = new SaveMacroCommand(SaveMacroCommand.MacroFlagMotorControl, SaveMacroCommand.MACRO_STREAMING_DESTINATION, sendBuffer.toByteArray());

        emits++;
        robot_outer_arg.sendSystemCommand(svc);

        // Check if we can continue creating more messages to send
        if (!commands.isEmpty() && freeBallMemory() > robot_outer_arg.getRobotSettings().getMacroMinSpaceSize())
            emptyMacroCommandQueue();
    }

    /**
     * Returns the number of free bytes for the ball
     *
     * @return The number of free bytes for the Sphero device
     */
    private int freeBallMemory()
    {
        int bytesInUse = 0;
        for (Integer aBallMemory : ballMemory) bytesInUse = bytesInUse + aBallMemory;

        return robot_outer_arg.getRobotSettings().getMacroRobotStorageSize() - bytesInUse;
    }

    public boolean getMacroRunning() {
        return macroRunning;
    }

    public Collection<Integer> getBallMemory() {
        return ballMemory;
    }
}
