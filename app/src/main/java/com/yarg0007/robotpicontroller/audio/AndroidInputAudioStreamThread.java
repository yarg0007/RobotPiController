package com.yarg0007.robotpicontroller.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

public class AndroidInputAudioStreamThread extends Thread {


    private int serverPort;

    /** The connected client. Setup to only allow a single client connection.*/
    private DatagramSocket serverDatagramSocket = null;

    /** Flag execution state of thread. */
    private boolean running;

    /** Plays audio to the speakers. */
    private SourceDataLine sourceDataLine;

    /**
     * Default constructor.
     */
    public SourceDataLineThread(int serverPort) {
        this.serverPort = serverPort;
        initialize();
    }

    /**
     * Initialize the instance. Setup Datagram server and then do all remaining
     * the setup magic. Must be called after getting class instance.
     */
    public void initialize() {

        if (running) {
            running = false;
            this.interrupt();

            // Let the thread terminate and then proceed.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        if (serverDatagramSocket != null) {
            serverDatagramSocket.close();
        }

        try {
            serverDatagramSocket = new DatagramSocket(serverPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (sourceDataLine == null) {

            DataLine.Info dataLineInfo =
                    new DataLine.Info(SourceDataLine.class, getAudioFormat());

            try {
                sourceDataLine =
                        (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceDataLine.open(getAudioFormat());
            } catch (LineUnavailableException e1) {
                e1.printStackTrace();
                System.out.println("Source data line unable to open. Bailing");
                stopAudioStreamSpeakers();
                return;
            }

            sourceDataLine.start();
        }
    }

    /**
     * Start the speaker thread after opening connections.
     */
    public void startAudioStreamSpeakers() {

        running = true;
        this.start();
    }

    /**
     * Stop the speaker thread, close connections etc.
     */
    public void stopAudioStreamSpeakers() {

        running = false;
        this.interrupt();

        sourceDataLine.flush();
        sourceDataLine.close();
        sourceDataLine = null;

        serverDatagramSocket.close();
        serverDatagramSocket = null;
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {

        // TODO: https://github.com/DeanThomson/android-udp-audio-chat/blob/master/src/hw/dt83/udpchat/AudioCall.java

        String url = "http://........"; // your URL here
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {

        }
        mediaPlayer.prepare(); // might take long! (for buffering, etc)
        mediaPlayer.start();



        int dataLen = getAudioBufferSizeBytes();
        byte[] datagramBuffer = new byte[dataLen];
        DatagramPacket datagramPacket = new DatagramPacket(datagramBuffer, dataLen);

        while (running) {

            try {
                serverDatagramSocket.receive(datagramPacket);
            } catch (IOException e) {

                System.out.println("Exception on incoming audio stream. Pausing before continuing.");
                e.printStackTrace();

                // Let the system rest and then loop back to try the
                // next incoming data bit.
                try {
                    sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                continue;
            }

            sourceDataLine.write(
                    datagramPacket.getData(),
                    0,
                    datagramPacket.getLength());
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    /**
     * Get the audio format.
     * @return Audio format to use for recording.
     */
    private AudioFormat getAudioFormat() {

        float sampleRate = 44100.0f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;

        return new AudioFormat(
                sampleRate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian);
    }

    /**
     * Size of the playback buffer in bytes.
     * @return Size of buffer
     */
    private int getAudioBufferSizeBytes() {

        int frameSizeInBytes = getAudioFormat().getFrameSize();
        int bufferLengthInFrames = sourceDataLine.getBufferSize() / 8;
        int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        return bufferLengthInBytes;
    }

}
