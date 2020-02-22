package com.yarg0007.robotpicontroller.ssh;

import com.yarg0007.robotpicontroller.ssh.commands.CommandExpectPair;

import java.util.ArrayList;
import java.util.List;

public class SshCommandPayload {

    private String id;
    private List<CommandExpectPair> commands;

    /**
     * Initialize with the given id and an empty list of commands.
     * @param id Unique ID to identify this payload with.
     */
    public SshCommandPayload(String id) {
        this(id, null);
    }

    /**
     * Initialize with the given id and list of commands.
     * @param id Unique ID to identify this payload with.
     * @param commands Commands to execute.
     */
    public SshCommandPayload(String id, List<CommandExpectPair> commands) {

        this.id = id;

        this.commands = new ArrayList<>();

        if (commands != null) {
            for (CommandExpectPair command : commands) {
                this.commands.add(command);
            }
        }
    }

    /**
     * Add the next command to execute.
     * @param pair Ssh command and expect pair instance.
     */
    public void addCommandPair(CommandExpectPair pair) {
        this.commands.add(pair);
    }

    /**
     * Get the execution payload ID.
     * @return Execution payload ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the list of commands to execute.
     * @return List of command and expect pairs.
     */
    public List<CommandExpectPair> getCommands() {
        return commands;
    }
}
