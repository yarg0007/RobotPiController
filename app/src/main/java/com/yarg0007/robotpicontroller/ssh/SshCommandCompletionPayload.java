package com.yarg0007.robotpicontroller.ssh;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SshCommandCompletionPayload {

    private String id;
    private List<String> commands;

    public SshCommandCompletionPayload(String id, List<String> commands) {

        this.id = id;

        this.commands = new ArrayList<>();
        for (String command : commands) {
            this.commands.add(command);
        }
    }

    public String getId() {
        return id;
    }

    public List<String> getCommands() {
        return commands;
    }
}
