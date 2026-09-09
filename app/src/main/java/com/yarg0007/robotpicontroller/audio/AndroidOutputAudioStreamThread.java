package com.yarg0007.robotpicontroller.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AndroidOutputAudioStreamThread extends Thread {

    private static final String TAG = "OutAudioStreamThread";

    private AudioRecord recorder;

    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean running = false;

    private boolean sendMicAudio = false;
    private boolean sendAudioFile = false;
    private boolean isRecording = false;
    private String audioFileToSend = null;

    // Persistent stream kept open across loop iterations so the file plays through
    private FileInputStream audioStream = null;
    // 16 kHz * 2 bytes per sample (16-bit mono)
    private static final int BYTES_PER_SEC = 16000 * 2;

    private int port;
    private final InetAddress host;

    AndroidOutputAudioStreamThread(String host, int port) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        this.port = port;
    }

    void startConnection() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
        running = true;
        this.start();
    }

    void stopConnection() {
        running = false;
    }

    void playAudioFile(String audioFilePath) {
        stopMicrophone();
        // If a different file is requested, close the current stream so the new one opens fresh
        if (!audioFilePath.equals(audioFileToSend)) {
            closeAudioStream();
            audioFileToSend = audioFilePath;
        }
        sendAudioFile = true;
    }

    void playMicrophone() {
        stopAudioFile();
        if (!isRecording) {
            recorder.startRecording();
            isRecording = true;
            Log.d(TAG, "Microphone recording started.");
        }
        sendMicAudio = true;
    }

    void stopAudioFile() {
        sendAudioFile = false;
        closeAudioStream();
    }

    void stopMicrophone() {
        sendMicAudio = false;
        if (isRecording) {
            recorder.stop();
            isRecording = false;
        }
    }

    private void closeAudioStream() {
        if (audioStream != null) {
            try { audioStream.close(); } catch (IOException ignored) {}
            audioStream = null;
        }
    }

    // Advances past the WAV file header by searching for the "data" sub-chunk.
    private void skipWavHeader(FileInputStream stream) throws IOException {
        stream.skip(12); // RIFF chunk descriptor: "RIFF" + file size + "WAVE"
        byte[] id = new byte[4];
        while (stream.read(id) == 4) {
            int b0 = stream.read(), b1 = stream.read(), b2 = stream.read(), b3 = stream.read();
            if (b3 < 0) break;
            long chunkSize = (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((long)(b3 & 0xFF) << 24);
            if (new String(id, "ASCII").equals("data")) break; // now positioned at raw PCM samples
            stream.skip(chunkSize);
        }
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
        DatagramPacket packet;

        while (running) {

            if (sendMicAudio) {
                minBufSize = recorder.read(buffer, 0, buffer.length);
            }

            if (sendAudioFile) {
                // Open the stream once; keep it open across iterations so the file plays through
                if (audioStream == null) {
                    try {
                        audioStream = new FileInputStream(audioFileToSend);
                        skipWavHeader(audioStream);
                    } catch (IOException e) {
                        Log.d(TAG, "Cannot open audio file: " + e.getMessage());
                        stopAudioFile();
                        continue;
                    }
                }

                int bytesRead;
                try {
                    bytesRead = audioStream.read(buffer);
                } catch (IOException e) {
                    stopAudioFile();
                    continue;
                }

                if (bytesRead == -1) {
                    stopAudioFile(); // reached end of file
                    continue;
                }

                // Zero-pad a partial final read so the full buffer is transmitted
                if (bytesRead < buffer.length) {
                    java.util.Arrays.fill(buffer, bytesRead, buffer.length, (byte) 0);
                }

                // Pace transmission to match 16 kHz / 16-bit / mono playback rate
                try {
                    Thread.sleep(buffer.length * 1000L / BYTES_PER_SEC);
                } catch (InterruptedException ignored) {}
            }

            if (sendMicAudio || sendAudioFile) {
                packet = new DatagramPacket(buffer, buffer.length, host, port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    Log.d(TAG, "IOException sending audio data.");
                } catch (NullPointerException npe) {
                    Log.d(TAG, "Null pointer exception while sending audio data.");
                }
            }
        }
    }
}
