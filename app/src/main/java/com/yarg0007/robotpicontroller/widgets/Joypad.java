package com.yarg0007.robotpicontroller.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class Joypad extends SurfaceView {

    // Working from: https://www.instructables.com/id/A-Simple-Android-UI-Joystick/

    public Joypad(Context context) {
        super(context);
    }

    public Joypad(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Joypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Joypad(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
