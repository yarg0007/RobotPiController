package com.yarg0007.robotpicontroller.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import androidx.core.app.ActivityCompat;

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
    private Context context;

    private int sampleRate = 16000; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_OUT_STEREO; //.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean running = false;

    private boolean sendMicAudio = false;
    private boolean sendAudioFile = false;
    private String audioFileToSend;

    private final int port; //49809 (original port for sending audio) or 50005;
    private final String hostName;
    private InetAddress host;
    private Thread runningThread;

    AndroidOutputAudioStreamThread(String hostName, int port, Context context) {
        this.hostName = hostName;
        this.port = port;
        this.context = context;
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

        int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);
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
