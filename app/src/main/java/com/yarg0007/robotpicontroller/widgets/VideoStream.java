package com.yarg0007.robotpicontroller.widgets;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Displays a low-latency H264 video stream from the Pi.
 *
 * The Pi runs: raspivid -ih ... -o - | nc -l PORT
 *
 * We connect via TCP and parse the raw Annex-B H264 stream into individual NAL
 * units, feeding one NAL per MediaCodec input buffer. Feeding multiple frames in
 * one buffer (e.g. 64 KB chunks) causes hardware decoders to discard everything
 * past the first access unit. SPS and PPS are extracted from the stream header
 * and supplied as CSD-0/CSD-1 so the decoder configures itself before the first
 * IDR frame arrives.
 *
 * Typical latency is 100-300 ms — no RTSP session, no VLC jitter buffer.
 */
public class VideoStream extends SurfaceView implements SurfaceHolder.Callback {

    public interface OnVideoStartedListener {
        void onVideoStarted();
    }

    private static final String TAG = "VideoStream";
    private static final int RETRY_DELAY_MS = 1000;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 480;
    private static final long FRAME_INTERVAL_US = 1_000_000L / 15; // 15 fps

    private OnVideoStartedListener onVideoStartedListener;
    private String streamHost;
    private int streamPort;
    private volatile boolean wantPlaying = false;
    private volatile boolean surfaceReady = false;
    private Thread streamThread;

    public VideoStream(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public VideoStream(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public VideoStream(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    public VideoStream(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getHolder().addCallback(this);
    }

    public void configure(String host, int port) {
        this.streamHost = host;
        this.streamPort = port;
    }

    public void setOnVideoStartedListener(OnVideoStartedListener listener) {
        this.onVideoStartedListener = listener;
    }

    public void startVideoStream() {
        wantPlaying = true;
        if (surfaceReady && (streamThread == null || !streamThread.isAlive())) {
            launchStreamThread();
        }
    }

    public void stopVideoStream() {
        wantPlaying = false;
        if (streamThread != null) {
            streamThread.interrupt();
            streamThread = null;
        }
    }

    public boolean isVideoStreamRunning() {
        return streamThread != null && streamThread.isAlive();
    }

    private void launchStreamThread() {
        final Surface surface = getHolder().getSurface();
        streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (wantPlaying && !Thread.currentThread().isInterrupted()) {
                    MediaCodec codec = null;
                    Socket socket = null;
                    try {
                        socket = new Socket(streamHost, streamPort);
                        socket.setTcpNoDelay(true);
                        NalReader nalReader = new NalReader(socket.getInputStream());

                        // raspivid -ih always emits SPS, PPS, IDR at stream start.
                        // Scan until we have both SPS and PPS for CSD configuration.
                        byte[] sps = null, pps = null, savedSlice = null;
                        for (int i = 0; i < 30 && (sps == null || pps == null); i++) {
                            byte[] nal = nalReader.readNal();
                            if (nal == null) break;
                            int t = nalType(nal);
                            if (t == 7) sps = nal;
                            else if (t == 8) pps = nal;
                            else if ((t == 5 || t == 1) && sps != null && pps != null) {
                                savedSlice = nal; // first slice after SPS+PPS — feed after codec starts
                                break;
                            }
                        }

                        if (sps == null || pps == null) {
                            Log.e(TAG, "SPS/PPS not found in H264 stream");
                            continue;
                        }

                        // CSD-0 = SPS, CSD-1 = PPS, both without the 4-byte Annex-B start code.
                        MediaFormat format = MediaFormat.createVideoFormat("video/avc", VIDEO_WIDTH, VIDEO_HEIGHT);
                        format.setByteBuffer("csd-0", ByteBuffer.wrap(stripStartCode(sps)));
                        format.setByteBuffer("csd-1", ByteBuffer.wrap(stripStartCode(pps)));

                        codec = MediaCodec.createDecoderByType("video/avc");
                        codec.configure(format, surface, null, 0);
                        codec.start();

                        notifyVideoStarted();

                        MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
                        long ptsUs = 0;

                        // Feed any slice NAL captured during the SPS/PPS scan.
                        if (savedSlice != null) {
                            feedNal(codec, savedSlice, ptsUs, sliceFlags(nalType(savedSlice)), bufInfo);
                            ptsUs += FRAME_INTERVAL_US;
                        }

                        // Main loop: one NAL unit per codec input buffer.
                        while (wantPlaying && !Thread.currentThread().isInterrupted()) {
                            byte[] nal = nalReader.readNal();
                            if (nal == null) break;

                            int t = nalType(nal);
                            feedNal(codec, nal, ptsUs, sliceFlags(t), bufInfo);
                            if (t == 1 || t == 5) ptsUs += FRAME_INTERVAL_US;

                            // Drain every available decoded frame and render to surface immediately.
                            int outIdx;
                            while ((outIdx = codec.dequeueOutputBuffer(bufInfo, 0)) >= 0) {
                                codec.releaseOutputBuffer(outIdx, true);
                            }
                        }

                    } catch (IOException e) {
                        Log.d(TAG, "Stream error: " + e.getMessage());
                    } catch (Exception e) {
                        Log.d(TAG, "Codec error: " + e.getMessage());
                    } finally {
                        if (codec != null) {
                            try { codec.stop(); } catch (Exception ignored) {}
                            try { codec.release(); } catch (Exception ignored) {}
                        }
                        if (socket != null) {
                            try { socket.close(); } catch (IOException ignored) {}
                        }
                    }

                    if (wantPlaying && !Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        streamThread.setDaemon(true);
        streamThread.setName("VideoStreamThread");
        streamThread.start();
    }

    /**
     * Queues one NAL unit to the codec, draining output buffers while waiting
     * for an available input buffer to prevent deadlock.
     */
    private void feedNal(MediaCodec codec, byte[] nal, long ptsUs, int flags,
                         MediaCodec.BufferInfo bufInfo) {
        while (wantPlaying) {
            int inIdx = codec.dequeueInputBuffer(10_000);
            if (inIdx >= 0) {
                ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                inBuf.clear();
                if (nal.length <= inBuf.capacity()) {
                    inBuf.put(nal);
                    codec.queueInputBuffer(inIdx, 0, nal.length, ptsUs, flags);
                } else {
                    // NAL too large for this buffer — skip it rather than truncate
                    Log.w(TAG, "NAL too large (" + nal.length + " > " + inBuf.capacity() + "), skipping");
                    codec.queueInputBuffer(inIdx, 0, 0, ptsUs, 0);
                }
                return;
            }
            // No input buffer available yet — drain output to prevent deadlock.
            int outIdx = codec.dequeueOutputBuffer(bufInfo, 0);
            if (outIdx >= 0) {
                codec.releaseOutputBuffer(outIdx, true);
            }
        }
    }

    /** Returns the NAL unit type (lower 5 bits of the byte after the start code). */
    private static int nalType(byte[] nal) {
        return (nal != null && nal.length >= 5) ? (nal[4] & 0x1F) : -1;
    }

    /** Returns BUFFER_FLAG_SYNC_FRAME for IDR slices, 0 otherwise. */
    private static int sliceFlags(int type) {
        return (type == 5) ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
    }

    /** Removes the 4-byte Annex-B start code (00 00 00 01) from a NAL unit. */
    private static byte[] stripStartCode(byte[] nal) {
        return (nal.length > 4) ? Arrays.copyOfRange(nal, 4, nal.length) : nal;
    }

    private void notifyVideoStarted() {
        if (onVideoStartedListener != null) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (onVideoStartedListener != null) {
                        onVideoStartedListener.onVideoStarted();
                    }
                }
            });
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (wantPlaying && (streamThread == null || !streamThread.isAlive())) {
            launchStreamThread();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        if (streamThread != null) {
            streamThread.interrupt();
            streamThread = null;
        }
    }

    /**
     * Extracts individual H264 Annex-B NAL units from a byte stream.
     *
     * Maintains an internal buffer. Each call to readNal() returns one complete
     * NAL unit (including its 4-byte start code) by locating the next start code
     * as the boundary. raspivid outputs 4-byte start codes (00 00 00 01) only.
     */
    private static final class NalReader {
        private byte[] buf;
        private int dataLen = 0;
        private final InputStream in;

        NalReader(InputStream in) {
            this.in = in;
            this.buf = new byte[131072]; // 128 KB; grows if needed
        }

        /**
         * Returns the next complete NAL unit including its 4-byte start code,
         * or null if the stream is closed.
         */
        byte[] readNal() throws IOException {
            fill(5);
            if (dataLen < 5) return null;

            while (true) {
                int next = findStartCode(4); // search past the current start code at offset 0
                if (next >= 0) {
                    byte[] nal = Arrays.copyOf(buf, next);
                    dataLen -= next;
                    System.arraycopy(buf, next, buf, 0, dataLen);
                    return nal;
                }
                int before = dataLen;
                fill(dataLen + 1);
                if (dataLen == before) {
                    // EOF: return whatever remains
                    if (dataLen == 0) return null;
                    byte[] nal = Arrays.copyOf(buf, dataLen);
                    dataLen = 0;
                    return nal;
                }
            }
        }

        /** Read from the stream until buf contains at least targetLen bytes, or EOF. */
        private void fill(int targetLen) throws IOException {
            while (dataLen < targetLen) {
                if (dataLen >= buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
                int n = in.read(buf, dataLen, buf.length - dataLen);
                if (n < 0) return;
                dataLen += n;
            }
        }

        /** Returns the index of the first 4-byte start code at or after fromOffset, or -1. */
        private int findStartCode(int fromOffset) {
            int end = dataLen - 3;
            for (int i = fromOffset; i < end; i++) {
                if (buf[i] == 0 && buf[i + 1] == 0 && buf[i + 2] == 0 && buf[i + 3] == 1) {
                    return i;
                }
            }
            return -1;
        }
    }
}
