package com.yarg0007.robotpicontroller.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AndroidOutputAudioStreamThread implements Runnable {

    private static final String TAG = "OutAudioStreamThread";

    private AudioRecord recorder;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean running = false;

    private boolean sendMicAudio = false;
    private boolean sendAudioFile = false;
    private String audioFileToSend;

    private final int port; //49809 (original port for sending audio) or 50005;
    private final String hostName;
    private InetAddress host;
    private Thread runningThread;

    AndroidOutputAudioStreamThread(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    void startConnection() {

        if (running) {
            return;
        }

        running = true;
        runningThread = new Thread(this);
        runningThread.start();
    }

    void stopConnection() {
        running = false;
        runningThread.interrupt();
    }

    boolean isRunning() {
        return running;
    }

    void playAudioFile(String audioFilePath) {
        stopMicrophone();
        this.audioFileToSend = audioFilePath;
        sendAudioFile = true;
    }

    void playMicrophone() {
        stopAudioFile();
        recorder.startRecording();
        sendMicAudio = true;
    }

    void stopAudioFile() {
        sendAudioFile = false;
    }

    void stopMicrophone() {
        sendMicAudio = false;
        if (recorder != null) {
            recorder.stop();
        }
    }

    @Override
    public void run() {

        try {
            InetAddress[] matches = InetAddress.getAllByName(hostName);
            int b = 0;
            b++;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            host = InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            running = false;
            e.printStackTrace();
            return;
        }

        if (host == null) {
            running = false;
            return;
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,minBufSize*10);
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            stopConnection();
        }

        byte[] buffer = new byte[minBufSize];
        FileInputStream audioFileInputStream;

        DatagramPacket packet;

        while(running) {

            //reading data from MIC into buffer
            if (sendMicAudio) {
                minBufSize = recorder.read(buffer, 0, buffer.length);
            }

            if (sendAudioFile) {
                try {
                    audioFileInputStream = new FileInputStream(audioFileToSend);
                } catch (FileNotFoundException e) {
                    stopAudioFile();
                    break;
                }

                try {
                    if (audioFileInputStream.read(buffer) == -1) {
                        stopAudioFile();
                        break;
                    }
                } catch (IOException e) {
                    stopAudioFile();
                    break;
                }
            }

            if (sendMicAudio || sendAudioFile) {

                //putting buffer in the packet
                packet = new DatagramPacket(buffer, buffer.length, host, port);

                try {
                    socket.send(packet);
                } catch (IOException e) {
                    Log.d(TAG, "IOException sending audio data.");
//                    stopConnection();
                } catch (NullPointerException npe) {
                    Log.d(TAG, "Null pointer exception while sending audio data.");
//                    stopConnection();
                }
            }
        }
    }
}
