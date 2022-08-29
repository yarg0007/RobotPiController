package com.yarg0007.robotpicontroller.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Handle the audio input from the robot and play it through speakers.
 *
 * Android AudioTrack example: https://github.com/DeanThomson/android-udp-audio-chat/blob/master/src/hw/dt83/udpchat/AudioCall.java
 */
public class AndroidInputAudioStreamThread implements Runnable {

    private static final String LOG_TAG = "SPEAKER";

    /*

    Audio Format settings from the previous client software.
    Robot will be using the same audio format settings.
    Copied here for reference.

    float sampleRate = 44100.0f;
    int sampleSizeInBits = 16;
    int channels = 1;
    boolean signed = true;
    boolean bigEndian = true;

    int frameSizeInBytes = getAudioFormat().getFrameSize();
    int bufferLengthInFrames = sourceDataLine.getBufferSize() / 8;
    int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
    return bufferLengthInBytes;

     */

    private static final int SAMPLE_RATE = 44100; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes

    private int serverPort;

    /** Flag execution state of thread. */
    private boolean running;
    private Thread runningThread;

    AndroidInputAudioStreamThread(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Start the speaker thread after opening connections.
     */
    void startAudioStreamSpeakers() {
        running = true;
        runningThread = new Thread(this);
        runningThread.start();
    }

    /**
     * Stop the speaker thread, close connections etc.
     */
    void stopAudioStreamSpeakers() {
        running = false;
        runningThread.interrupt();
    }

    @Override
    public void run() {

        AudioTrack   track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUF_SIZE,
                AudioTrack.MODE_STREAM);

        track.play();

        try {
            // Define a socket to receive the audio
            DatagramSocket socket = new DatagramSocket(serverPort);
            byte[] buf = new byte[BUF_SIZE];

            while(running) {
                // Play back the audio received from packets
                DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                socket.receive(packet);
                Log.i("SPEAKER", "Packet received: " + packet.getLength());
                track.write(packet.getData(), 0, BUF_SIZE);
            }

            // Stop playing back and release resources
            socket.disconnect();
            socket.close();
            track.stop();
            track.flush();
            track.release();
        }
        catch(SocketException e) {
            Log.e(LOG_TAG, "SocketException: " + e.toString());
        }
        catch(IOException e) {
            Log.e(LOG_TAG, "IOException: " + e.toString());
        }
    }
}
