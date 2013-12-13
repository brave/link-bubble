package com.chrislacy.linkbubble;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gw on 2/10/13.
 */
public class Util {
    public static void Assert(boolean condition) {
        Assert(condition, "Unknown Error");
    }

    public static void Assert(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static float clamp(float v0, float v, float v1) {
        return Math.max(v0, Math.min(v, v1));
    }

    public static int clamp(int v0, int v, int v1) {
        return Math.max(v0, Math.min(v, v1));
    }

    static public boolean isDefaultBrowser(String currentPackageName, PackageManager packageManager) {

        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
        final ResolveInfo info = packageManager.resolveActivity(i, 0);
        if (info != null) {
            if (info.activityInfo.applicationInfo.packageName.equals(currentPackageName)) {
                return true;
            }
        }
        return false;
    }
}
