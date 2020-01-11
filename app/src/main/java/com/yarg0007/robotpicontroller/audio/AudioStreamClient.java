package com.yarg0007.robotpicontroller.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.yarg0007.robotpicontroller.input.AudioControls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AudioStreamClient extends Thread implements AudioControls {

//    private int SEND_PORT = 49809;
//
//    private String SERVER_ADDRESS = "robotpi.local";

    private byte[] buffer;
    private static DatagramSocket socket;

    private AudioRecord recorder;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean running = false;

    private boolean sendMicAudio = false;
    private boolean sendAudioFile = false;
    private String audioFileToSend = null;

    private int port = 50005;
    private final InetAddress host;

    public AudioStreamClient(String host, int port) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        this.port = port;
    }

    public void startConnection() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,minBufSize*10);
        running = true;
        this.start();
    }

    public void stopConnection() {
        running = false;
    }

    // -------------------------------------------------------------------------
    // Methods required by AudioControls
    // -------------------------------------------------------------------------

    @Override
    public void playAudioFile(String audioFilePath) {
        stopMicrophone();
        this.audioFileToSend = audioFilePath;
        sendAudioFile = true;
    }

    @Override
    public void playMicrophone() {
        stopAudioFile();
        recorder.startRecording();
        sendMicAudio = true;
    }

    @Override
    public void stopAudioFile() {
        sendAudioFile = false;
    }

    @Override
    public void stopMicrophone() {
        sendMicAudio = false;
        recorder.stop();
    }

    @Override
    public void setAudioFilePacketDelay(long milliseconds) {
        return;
    }

    @Override
    public void run() {

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
                    Log.d("AUDIO_STREAM", "IOException sending audio data. Terminating audio thread.");
                    stopConnection();
                }
            }
        }
    }
}
