package com.yarg0007.robotpicontroller.ssh;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SshCommands {

    // Start video
    public static final String startVideoStreamId = "StartVideoStream";

    public static final List<String> startVideoStreamCommands = Arrays.asList(
            "cd ~/mpjg-streamer-master/mjpg-streamer-experimental",
            "export LD_LIBRARY_PATH=.",
            "./mjpg_streamer -o \"output_http.so -w ./www\" -i \"input_raspicam.so\"");

    // Stop video
    // TODO
    public static final String stopVideoStreamId = "StopVideoStream";

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
