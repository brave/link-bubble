package com.linkbubble.util;

import android.content.Context;
import com.crashlytics.android.Crashlytics;

public class CrashTracking {

    public static void init(Context context) {
        Crashlytics.start(context);
    }

    public static void logHandledException(Throwable throwable) {
        Crashlytics.logException(throwable);
    }

}
