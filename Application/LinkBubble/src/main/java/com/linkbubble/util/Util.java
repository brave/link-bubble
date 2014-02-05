package com.linkbubble.util;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import com.linkbubble.Constant;
import com.linkbubble.R;

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

    public static String getPrettyTimeElapsed(Resources resources, long time, String separator) {
        float timeAsSeconds = (float)time / 1000.f;
        if (timeAsSeconds < 60) {
            return String.format("%.1f", timeAsSeconds) + separator + resources.getString(R.string.time_seconds);
        } else if (timeAsSeconds < 60 * 60) {
            return String.format("%.1f", timeAsSeconds / 60.f) + separator + resources.getString(R.string.time_minutes);
        } else {
            return String.format("%.1f", timeAsSeconds / 60.f / 60.f) + separator + resources.getString(R.string.time_hours);
        }
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

    private static final int OUTCODE_INSIDE = 0;
    private static final int OUTCODE_LEFT = 1;
    private static final int OUTCODE_RIGHT = 2;
    private static final int OUTCODE_BOTTOM = 4;
    private static final int OUTCODE_TOP = 8;

    public static class ClipResult {
        public int x0, y0, x1, y1;
    }

    public static class Point {
        public int x, y;
    }

    private static int computeOutCode(float x, float y, float xmin, float ymin, float xmax, float ymax)
    {
        int code = OUTCODE_INSIDE;

        if (x < xmin)           // to the left of clip window
            code |= OUTCODE_LEFT;
        else if (x > xmax)      // to the right of clip window
            code |= OUTCODE_RIGHT;
        if (y < ymin)           // below the clip window
            code |= OUTCODE_BOTTOM;
        else if (y > ymax)      // above the clip window
            code |= OUTCODE_TOP;

        return code;
    }

    // Naive Java port of Cohen Sutherland clipping algorithm.
    // See: http://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm

    public static boolean clipLineSegmentToRectangle(float x0, float y0, float x1, float y1,
                                                     float xmin, float ymin, float xmax, float ymax,
                                                     ClipResult clipResult) {

        // compute outcodes for P0, P1, and whatever point lies outside the clip rectangle
        int outcode0 = computeOutCode(x0, y0, xmin, ymin, xmax, ymax);
        int outcode1 = computeOutCode(x1, y1, xmin, ymin, xmax, ymax);
        boolean accept = false;

        while (true) {
            if ((outcode0 | outcode1) == 0) { // Bitwise OR is 0. Trivially accept and get out of loop
                accept = true;
                break;
            } else if ((outcode0 & outcode1) != 0) { // Bitwise AND is not 0. Trivially reject and get out of loop
                break;
            } else {
                // failed both tests, so calculate the line segment to clip
                // from an outside point to an intersection with clip edge
                float x, y;

                // At least one endpoint is outside the clip rectangle; pick it.
                int outcodeOut = (outcode0 != 0) ? outcode0 : outcode1;

                // Now find the intersection point;
                // use formulas y = y0 + slope * (x - x0), x = x0 + (1 / slope) * (y - y0)
                if ((outcodeOut & OUTCODE_TOP) != 0) {           // point is above the clip rectangle
                    x = x0 + (x1 - x0) * (ymax - y0) / (y1 - y0);
                    y = ymax;
                } else if ((outcodeOut & OUTCODE_BOTTOM) != 0) { // point is below the clip rectangle
                    x = x0 + (x1 - x0) * (ymin - y0) / (y1 - y0);
                    y = ymin;
                } else if ((outcodeOut & OUTCODE_RIGHT) != 0) {  // point is to the right of clip rectangle
                    y = y0 + (y1 - y0) * (xmax - x0) / (x1 - x0);
                    x = xmax;
                } else {   // point is to the left of clip rectangle
                    Util.Assert((outcodeOut & OUTCODE_LEFT) != 0);
                    y = y0 + (y1 - y0) * (xmin - x0) / (x1 - x0);
                    x = xmin;
                }

                // Now we move outside point to intersection point to clip
                // and get ready for next pass.
                if (outcodeOut == outcode0) {
                    x0 = x;
                    y0 = y;
                    outcode0 = computeOutCode(x0, y0, xmin, ymin, xmax, ymax);
                } else {
                    x1 = x;
                    y1 = y;
                    outcode1 = computeOutCode(x1, y1, xmin, ymin, xmax, ymax);
                }
            }
        }

        if (accept) {
            // Following functions are left for implementation by user based on
            // their platform (OpenGL/graphics.h etc.)
            clipResult.x0 = (int) (x0 + 0.5f);
            clipResult.y0 = (int) (y0 + 0.5f);
            clipResult.x1 = (int) (x1 + 0.5f);
            clipResult.y1 = (int) (y1 + 0.5f);
        }

        return accept;
    }

    public static void closestPointToLineSegment(float ax, float ay, float bx, float by, float px, float py, Point p) {
        float apX = px - ax;
        float apY = py - ay;

        float abX = bx - ax;
        float abY = by - ay;

        float abSq = abX * abX + abY * abY;
        float dot = apX * abX + apY * abY;
        float t = dot / abSq;

        t = Util.clamp(0.0f, t, 1.0f);

        p.x = (int) (0.5f + ax + abX * t);
        p.y = (int) (0.5f + ay + abY * t);
    }

    public static boolean isLinkBubbleResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo != null
            && resolveInfo.activityInfo != null
                && resolveInfo.activityInfo.packageName.equals(Constant.PACKAGE_NAME)) {
            return true;
        }

        return false;
    }
}
