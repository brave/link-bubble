package com.linkbubble.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.linkbubble.BuildConfig;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.Settings;

public class PackageChangedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        String data = intent.getData() != null ? intent.getData().toString() : null;
        Log.d(DRM.TAG, "action:" + action + ", data:" + data);
        if (Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (Settings.get() != null) {
                Settings.get().updateBrowsers();
            }

            if (data != null && data.contains(BuildConfig.PRO_PACKAGE_NAME)) {
                MainApplication.postEvent(context, new MainApplication.CheckStateEvent());
                Log.d(DRM.TAG, "checkForProVersion() finished");
            }
        }
    }
}
