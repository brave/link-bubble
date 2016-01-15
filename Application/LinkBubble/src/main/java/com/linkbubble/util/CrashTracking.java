/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.linkbubble.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class CrashTracking {

    public static void logHandledException(Throwable throwable) {
        Crashlytics.logException(throwable);
    }

    public static void setInt(String key, int value) {
        Crashlytics.setInt(key, value);
    }

    public static void setDouble(String key, double value) {
        Crashlytics.setDouble(key, value);
    }

    public static void setFloat(String key, float value) {
        Crashlytics.setFloat(key, value);
    }

    public static void setString(String key, String string) {
        Crashlytics.setString(key, string);
    }

    public static void setBool(String key, boolean value) {
        Crashlytics.setBool(key, value);
    }

    public static void log(String message) {
        Crashlytics.log(message);
        if (BuildConfig.DEBUG) {
            Log.d("CrashTracking", message);
        }
    }
}
