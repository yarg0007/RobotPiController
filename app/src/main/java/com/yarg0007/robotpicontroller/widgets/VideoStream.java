package com.yarg0007.robotpicontroller.widgets;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class VideoStream extends SurfaceView implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, SurfaceHolder.Callback {

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 3000;

    private MediaPlayer mediaPlayer;
    private String streamHost;
    private int streamPort;
    private boolean surfaceReady = false;
    private boolean startPending = false;
    private boolean wantPlaying = false;
    private int retryCount = 0;

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

    public void startVideoStream() {
        wantPlaying = true;
        retryCount = 0;
        if (surfaceReady) {
            beginPlayback();
        } else {
            startPending = true;
        }
    }

    public void stopVideoStream() {
        wantPlaying = false;
        startPending = false;
        releaseMediaPlayer();
    }

    public boolean isVideoStreamRunning() {
        return mediaPlayer != null;
    }

    private void beginPlayback() {
        releaseMediaPlayer();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDisplay(getHolder());
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);

        String uri = "rtsp://" + streamHost + ":" + streamPort + "/";
        try {
            mediaPlayer.setDataSource(uri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            releaseMediaPlayer();
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        retryCount = 0;
        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        releaseMediaPlayer();
        if (wantPlaying && retryCount < MAX_RETRIES && surfaceReady) {
            retryCount++;
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (wantPlaying && surfaceReady) {
                        beginPlayback();
                    }
                }
            }, RETRY_DELAY_MS);
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (startPending) {
            startPending = false;
            beginPlayback();
        } else if (wantPlaying && mediaPlayer == null) {
            beginPlayback();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        releaseMediaPlayer();
    }
}
