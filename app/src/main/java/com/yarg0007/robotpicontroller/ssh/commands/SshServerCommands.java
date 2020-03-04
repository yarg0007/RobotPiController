package com.yarg0007.robotpicontroller.ssh.commands;

import com.yarg0007.robotpicontroller.ssh.SshCommandPayload;

import java.util.ArrayList;

public final class SshServerCommands {

    public static final String startVideoStreamId = "StartVideoStream";
    public static final String stopVideoStreamId = "StopVideoStream";
    public static final String startServerId = "StartServer";
    public static final String stopServerId = "StopServer";
    public static final String shutdownRaspberryPiId = "ShutdownRaspberryPi";
    public static final String connectedId = "Connected";

    // Make it impossible to instantiate
    private SshServerCommands() {

    }

    /**
     * Commands to start the video stream. ID: startVideoStreamId
     * @return Command and expect pairs.
     */
    public static SshCommandPayload getStartVideoPayload() {

        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("cd ~/mjpg-streamer-master/mjpg-streamer-experimental/", "$"));
        commands.add(new CommandExpectPair("export LD_LIBRARY_PATH=.", "$"));
        commands.add(new CommandExpectPair("nohup ./mjpg_streamer -o \"output_http.so -w ./www\" -i \"input_raspicam.so\"", "nohup: ignoring input and appending output"));

        return new SshCommandPayload(startVideoStreamId, commands);
    }

    /**
     * Commands to stop the video stream. ID: stopVideoStreamId
     * @return Command and expect pairs.
     */
    public static SshCommandPayload getStopVideoPayload() {

        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("killall mjpg_streamer", "$"));

        return new SshCommandPayload(stopVideoStreamId, commands);
    }

    /**
     * Commands to start the robot server. ID: startServerId
     * @return Command and expect pairs.
     */
    public static SshCommandPayload getStartServerPayload() {
        // TODO: finish
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("nohup sudo java -cp RobotPiServer-0.0.1-SNAPSHOT.jar:pi4j-core-1.0.jar com.yarg.robotpiserver.RobotPiServer &", "$"));

        return new SshCommandPayload(startServerId, commands);
    }

    /**
     * Commands to stop the robot server. ID: stopServerId
     * @return Command and expect pairs.
     */
    public static SshCommandPayload getStopServerPayload() {
        // TODO: finish - confirm that this is the best way to end the server
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("sudo pkill -f 'java -cp'", "$"));

        return new SshCommandPayload(stopServerId, commands);
    }

    /**
     * Commands to shutdown the raspberry pi. ID: shutdownRaspberryPiId
     * @return Command and expect pairs.
     */
    public static SshCommandPayload getShutdownRaspberryPiPayload() {
        // TODO: finish - confirm that this is the best way to end the server
        ArrayList<CommandExpectPair> commands = new ArrayList<>();
        commands.add(new CommandExpectPair("sudo shutdown -h now", ""));

        return new SshCommandPayload(shutdownRaspberryPiId, commands);
    }
}
