package com.yarg0007.robotpicontroller.audio;

import android.content.Context;

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

    public AudioStreamClient(String host, int audioInputPort, int audioOutputPort, Context context) {
        androidOutputAudioStreamThread = new AndroidOutputAudioStreamThread(host, audioOutputPort, context);
        androidInputAudioStreamThread = new AndroidInputAudioStreamThread(audioInputPort);
    }

    public boolean startConnection() {
        androidOutputAudioStreamThread.startConnection();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!androidOutputAudioStreamThread.isRunning()) {
            return false;
        }
        androidInputAudioStreamThread.startAudioStreamSpeakers();
        return true;
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
