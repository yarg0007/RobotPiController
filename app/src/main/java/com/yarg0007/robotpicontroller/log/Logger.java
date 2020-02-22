package com.yarg0007.robotpicontroller.log;

import android.util.Log;

public class Logger {

    private boolean disabledForTesting = false;

    public void setDisabledForTesting(boolean disabled) {
        this.disabledForTesting = disabled;
    }

    public void e(String tag, String message) {
        if (!disabledForTesting) {
            Log.e(tag, message);
        }
    }

    public void d(String tag, String message) {
        if (!disabledForTesting) {
            Log.d(tag, message);
        }
    }
}
