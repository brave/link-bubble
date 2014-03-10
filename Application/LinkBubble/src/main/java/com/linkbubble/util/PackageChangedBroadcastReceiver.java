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
        Log.d(DRM.TAG, "action:" + action + ", data:" + intent.getData().toString());
        if (Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (Settings.get() != null) {
                Settings.get().updateBrowsers();
            }

            String data = intent.getData() != null ? intent.getData().toString() : null;
            if (data != null && data.contains(BuildConfig.PRO_LAUNCHER_PACKAGE_NAME) && MainApplication.sDrm != null) {
                MainApplication.checkForProVersion(context.getApplicationContext());
                Log.d(DRM.TAG, "checkForProVersion() finished");
            }
        }
    }
}
