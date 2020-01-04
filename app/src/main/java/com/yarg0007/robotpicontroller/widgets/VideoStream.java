package com.yarg0007.robotpicontroller.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.media.MediaPlayer;

public class VideoStream extends SurfaceView implements MediaPlayer.OnPreparedListener, SurfaceHolder.Callback {

    public VideoStream(Context context) {
        super(context);
        this.getHolder().addCallback(this);
    }

    public VideoStream(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.getHolder().addCallback(this);
    }

    public VideoStream(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.getHolder().addCallback(this);
    }

    public VideoStream(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.getHolder().addCallback(this);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
