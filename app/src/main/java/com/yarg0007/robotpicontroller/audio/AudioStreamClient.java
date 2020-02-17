package com.yarg0007.robotpicontroller.audio;

import com.yarg0007.robotpicontroller.input.AudioControls;

import java.net.UnknownHostException;

public class AudioStreamClient implements AudioControls {

//    private int RECEIVE_PORT = 49808;
//
//    private int SEND_PORT = 49809;
//
//    private String SERVER_ADDRESS = "robotpi.local";

    private AndroidOutputAudioStreamThread androidOutputAudioStreamThread;
    private AndroidInputAudioStreamThread androidInputAudioStreamThread;

    public AudioStreamClient(String host, int port) throws UnknownHostException {
        androidOutputAudioStreamThread = new AndroidOutputAudioStreamThread(host, port);
        androidInputAudioStreamThread = new AndroidInputAudioStreamThread(port);
    }

    public void startConnection() {
        androidOutputAudioStreamThread.startConnection();
        androidInputAudioStreamThread.startAudioStreamSpeakers();
    }

    public void stopConnection() {
        androidOutputAudioStreamThread.stopConnection();
        androidInputAudioStreamThread.stopAudioStreamSpeakers();
    }

    // -------------------------------------------------------------------------
    // Methods required by AudioControls
    // -------------------------------------------------------------------------

    @Override
    public void playAudioFile(String audioFilePath) {
        androidOutputAudioStreamThread.playAudioFile(audioFilePath);
    }

    @Override
    public void playMicrophone() {
        androidOutputAudioStreamThread.playMicrophone();
    }

    @Override
    public void stopAudioFile() {
        androidOutputAudioStreamThread.stopAudioFile();
    }

    @Override
    public void stopMicrophone() {
        androidOutputAudioStreamThread.stopMicrophone();
    }

    @Override
    public void setAudioFilePacketDelay(long milliseconds) {
    }
}
