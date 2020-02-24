package com.yarg0007.robotpicontroller.log;

import android.util.Log;

public final class Logger {

    private static boolean disabledForTesting = false;

    private Logger() {

    }

    public static void setDisabledForTesting(boolean disabled) {
        disabledForTesting = disabled;
    }

    public static void e(String tag, String message) {
        if (!disabledForTesting) {
            Log.e(tag, message);
        }
    }

    public static void d(String tag, String message) {
        if (!disabledForTesting) {
            Log.d(tag, message);
        }
    }
}
