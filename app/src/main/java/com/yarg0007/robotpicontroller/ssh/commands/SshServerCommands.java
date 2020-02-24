package com.yarg0007.robotpicontroller.ssh.commands;

import com.yarg0007.robotpicontroller.ssh.SshCommandPayload;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SshServerCommands {

    // Payload IDs
    public static final String startVideoStreamId = "StartVideoStream";
    public static final String stopVideoStreamId = "StopVideoStream";

    public static SshCommandPayload getStartVideoPayload() {

        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("cd ~/mpjg-streamer-master/mjpg-streamer-experimental", "$"));
        commands.add(new CommandExpectPair("export LD_LIBRARY_PATH=.", "$"));
        commands.add(new CommandExpectPair("./mjpg_streamer -o \"output_http.so -w ./www\" -i \"input_raspicam.so\"", "$"));

        return new SshCommandPayload(startVideoStreamId, commands);
    }

    public static SshCommandPayload getStopVideoPayload() {

        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("killall mjpg_streamer", "$"));

        return new SshCommandPayload(stopVideoStreamId, commands);
    }

    public static final List<String> stopVideoStreamCommands = Arrays.asList("");

    // Start server executable
    // TODO
    public static final String startServerId = "StartServer";

    public static final List<String> startServerCommands = Arrays.asList("");

    // Stop server executable
    // TODO
    public static final String stopServerId = "StopServer";

    public static final List<String> stopServerCommands = Arrays.asList("");

    // Shutdown raspberry pi
    public static final String shutdownRaspberryPiId = "ShutdownRaspberryPi";

    public static final List<String> shutdownRaspberryPiCommands = Arrays.asList("sudo shutdown -h now");
}
