package com.yarg0007.robotpicontroller.ssh;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SshCommands {



    public static final List<String> startVideoStreamCommands = Arrays.asList(
            "cd ~/mpjg-streamer-master/mjpg-streamer-experimental",
            "export LD_LIBRARY_PATH=.",
            "./mjpg_streamer -o \"output_http.so -w ./www\" -i \"input_raspicam.so\"");

    public static final List<String> shutdownRaspberryPi = Arrays.asList("sudo shutdown -h now");



    // TODO: launch whatever needs to be launched, including the video stream and server app
    // TODO: close out the server and stop the video stream
    // TODO: shutdown the raspberry pi
}
