package com.chrislacy.linkbubble;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    /*
	 *
	 */
    public static String getPrettyDate(Date createdAt) {

        return getPrettyDate(createdAt, new Date());
    }

    /*
	 *
	 */
    public static String getPrettyDate(Date olderDate, Date newerDate) {

        String result;

        int diffInDays = (int) ((newerDate.getTime() - olderDate.getTime()) / (1000 * 60 * 60 * 24));
        if (diffInDays > 365) {
            SimpleDateFormat formatted = new SimpleDateFormat("dd MMM yy");
            result = formatted.format(olderDate);
        } else if (diffInDays > 0) {
            if (diffInDays == 1) {
                result = "1 day";
            } else if (diffInDays < 8) {
                result = diffInDays + " days";
            } else {
                SimpleDateFormat formatted = new SimpleDateFormat("dd MMM");
                result = formatted.format(olderDate);
            }
        } else {
            int diffInHours = (int) ((newerDate.getTime() - olderDate.getTime()) / (1000 * 60 * 60));
            if (diffInHours > 0) {
                if (diffInHours == 1) {
                    result = "1 hour ago";
                } else {
                    result = diffInHours + " hours ago";
                }
            } else {
                int diffInMinutes = (int) ((newerDate.getTime() - olderDate
                        .getTime()) / (1000 * 60));
                if (diffInMinutes > 0) {
                    if (diffInMinutes == 1) {
                        result = "1 minute";
                    } else {
                        result = diffInMinutes + " minutes ago";
                    }
                } else {
                    int diffInSeconds = (int) ((newerDate.getTime() - olderDate
                            .getTime()) / (1000));
                    if (diffInSeconds < 5) {
                        result = "now";
                    } else {
                        result = diffInSeconds + " seconds ago";
                    }
                }
            }
        }

        return result;
    }
}
