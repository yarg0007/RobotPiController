package com.yarg0007.robotpicontroller.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class Joypad extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {

    // Working from: https://www.instructables.com/id/A-Simple-Android-UI-Joystick/

    private static final String TAG = "Joypad";

    private float centerX;
    private float centerY;
    private float baseRadius;
    private float hatRadius;

    private float userInputX;
    private float userInputY;

    private boolean isSticky = false;

    public Joypad(Context context) {
        super(context);
        this.setZOrderOnTop(true);
        this.getHolder().addCallback(this);
        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
    }

    public Joypad(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setZOrderOnTop(true);
        this.getHolder().addCallback(this);
        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
    }

    public Joypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setZOrderOnTop(true);
        this.getHolder().addCallback(this);
        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
    }

    public Joypad(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.setZOrderOnTop(true);
        this.getHolder().addCallback(this);
        this.getHolder().setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        setupDimensions();
        drawJoypad(centerX, centerY);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if (!view.equals(this)) {
            return false;
        }

        if (!isSticky && motionEvent.getAction() == MotionEvent.ACTION_UP) {

            setUserInputValues(0.0f, 0.0f);
            drawJoypad(centerX, centerY);

        } else {

            float localX = motionEvent.getX() - centerX;
            float localY = motionEvent.getY() - centerY;

            float inputRadius = localX * localX + localY * localY;
            inputRadius = (float) Math.sqrt(inputRadius);

            if (inputRadius > baseRadius) {
                float adjustmentFactor = baseRadius / inputRadius;
                localX = localX * adjustmentFactor;
                localY = localY * adjustmentFactor;
            }

            setUserInputValues(localX, localY);
            drawJoypad(centerX + localX, centerY + localY);

        }

        return true;
    }

    public void setIsSticky(boolean isSticky) {
        this.isSticky = isSticky;
    }

    public float getUserInputXPosition() {
        return userInputX;
    }

    public float getUserInputYPosition() {
        return userInputY;
    }

    public float getUserInputXPercentage() {
        return getUserInputXPosition() / baseRadius;
    }

    public float getUserInputYPercentage() {
        return getUserInputYPosition() / baseRadius;
    }

    private void setUserInputValues(float inputX, float inputY) {
        userInputX = inputX;
        userInputY = inputY;
        Log.d(TAG, String.format("user input value: %f, %f", getUserInputXPosition(), getUserInputYPosition()));
        Log.d(TAG, String.format("user input percentage: %f, %f", getUserInputXPercentage(), getUserInputYPercentage()));
    }

    private void drawJoypad(float positionX, float positionY) {

        if (!this.getHolder().getSurface().isValid()) {
            return;
        }

        Canvas canvas = this.getHolder().lockCanvas();
        Paint color = new Paint();

        // Clear the background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Draw joypad base
        color.setARGB(255, 50, 50, 50);
        canvas.drawCircle(centerX, centerY, baseRadius, color);

        // Draw the joypad head
        color.setARGB(255 , 0,0,255);
        canvas.drawCircle(positionX, positionY, hatRadius, color);

        this.getHolder().unlockCanvasAndPost(canvas);
    }

    private void setupDimensions() {
        centerX = getWidth()/2;
        centerY = getHeight()/2;
        baseRadius = Math.min(getWidth(), getHeight()) / 3;
        hatRadius = Math.min(getWidth(), getHeight()) / 6;
    }
}
