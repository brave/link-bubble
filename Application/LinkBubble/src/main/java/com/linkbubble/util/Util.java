package com.linkbubble.util;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

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
                result = "1d";
            } else if (diffInDays < 8) {
                result = diffInDays + "d";
            } else {
                SimpleDateFormat formatted = new SimpleDateFormat("dd MMM");
                result = formatted.format(olderDate);
            }
        } else {
            int diffInHours = (int) ((newerDate.getTime() - olderDate.getTime()) / (1000 * 60 * 60));
            if (diffInHours > 0) {
                if (diffInHours == 1) {
                    result = "1h";
                } else {
                    result = diffInHours + "h";
                }
            } else {
                int diffInMinutes = (int) ((newerDate.getTime() - olderDate
                        .getTime()) / (1000 * 60));
                if (diffInMinutes > 0) {
                    if (diffInMinutes == 1) {
                        result = "1m";
                    } else {
                        result = diffInMinutes + "m";
                    }
                } else {
                    int diffInSeconds = (int) ((newerDate.getTime() - olderDate
                            .getTime()) / (1000));
                    if (diffInSeconds < 5) {
                        result = "now";
                    } else {
                        result = diffInSeconds + "s";
                    }
                }
            }
        }

        return result;
    }


    public static String downloadJSONAsString(String url, int timeout) {
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-length", "0");
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.connect();
            int status = connection.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static float distance(float x0, float y0, float x1, float y1) {
        float xd = x1 - x0;
        float yd = y1 - y0;
        float d = (float) Math.sqrt(xd*xd + yd*yd);
        return d;
    }

    public static String getDefaultFaviconUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
    }
}
