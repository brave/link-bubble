/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.linkbubble.BuildConfig;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Util {
    public static void Assert(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static String[] whitelistedBrowsers = {
        "com.boatbrowser.free",
        "com.boatbrowser.tablet",
        "com.boatgo.browser",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fennec_aurora",
        "org.mozilla.fennec",
        "com.android.chrome",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.mini.android",
        "com.opera.browser.beta",
        "com.opera.mini.native.beta",
        "com.chrome.dev",
        "com.chrome.beta",
        "com.ksmobile.cb",
        "com.UCMobile.intl",
        "mobi.mgeek.TunnyBrowser",
        "com.explore.web.browser",
        "explore.web.browser",
        "com.ghostery.android.ghostery",
        "jp.ddo.pigsty.HabitBrowser",
        "org.adblockplus.browser",
        "com.apusapps.browser",
        "com.apusapps.browser.turbo",
        "com.onedepth.search",
        "com.sec.android.app.sbrowser",
        "com.flynx",
        "com.uc.browser.en",
        "mobi.browser.flashfox",
        "acr.browser.barebones",
        "acr.browser.lightning",
        "com.ineedyourservice.RBrowser",
        "com.lastpass.lpandroid",
        "com.linkbubble.playstore",
        "com.linkbubble.playstore.dev",
        "com.brave.playstore",          // Made that temporary(remove when merge to master)
        "com.brave.playstore.dev",       // Made that temporary(remove when merge to master)
        "arun.com.chromer",
        "com.jiubang.browser",
        "com.uc.browser.hd",
        "com.ilegendsoft.mercury",
        "com.UCMobile",
        "com.tencent.mtt.intl",
        "com.tencent.mtt",
        "com.cloudmosa.puffinFree",
        "com.mx.browser",
        "net.fast.web.browser",
        "com.wisesharksoftware.browser",
        "org.hola",
        "com.kk.jd.browser2",
        "com.rsbrowser.browser",
        "org.chromium.chrome",
        "com.ineedyourservice.RBrowser",
        "jp.co.fenrir.android.sleipnir",
        "jp.co.fenrir.android.sleipnir_test",
        "tugapower.codeaurora.browser",
        "com.fevdev.nakedbrowser",
        "com.fevdev.nakedbrowserpro",
        "com.yandex.browser",
        "com.yandex.browser.alpha",
        "com.yandex.browser.beta",
        "com.flyperinc.flyperlink",
        "com.wSpeedBrowser4G",
        "com.wSpeedBrowsermini",
        "com.mokee.yubrowser",
        "org.mozilla.fennec",
        "org.gnu.icecat",
        "devian.tubemate.home"
    };

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
            SimpleDateFormat formatted = new SimpleDateFormat("dd MMM yy", Locale.US);
            result = formatted.format(olderDate);
        } else if (diffInDays > 0) {
            if (diffInDays == 1) {
                result = "1d";
            } else if (diffInDays < 8) {
                result = diffInDays + "d";
            } else {
                SimpleDateFormat formatted = new SimpleDateFormat("dd MMM", Locale.US);
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
                    Util.Assert((outcodeOut & OUTCODE_LEFT) != 0, "outcodeOut:" + outcodeOut);
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
                && resolveInfo.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID)) {
            return true;
        }

        return false;
    }

    static Random sRandom = null;
    public static int randInt(int min, int max) {

        if (sRandom == null) {
            sRandom = new Random();
        }

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = sRandom.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    static public ResolveInfo getDefaultBrowser(PackageManager packageManager) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(Config.SET_DEFAULT_BROWSER_URL));
        return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    static public void replaceViewAtPosition(View viewToReplace, View replaceWith) {
        ViewGroup parent = (ViewGroup) viewToReplace.getParent();
        int index = parent.indexOfChild(viewToReplace);
        parent.removeView(viewToReplace);
        parent.addView(replaceWith, index);
    }

    private static int sIconWidth = -1;
    private static int sIconHeight = -1;
    private static int sIconTextureWidth = -1;
    private static int sIconTextureHeight = -1;

    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }
    static int sColors[] = { 0xffff0000, 0xff00ff00, 0xff0000ff };
    static int sColorIndex = 0;

    private static void initStatics(Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        final float density = metrics.density;

        sIconWidth = sIconHeight = (int) resources.getDimension(R.dimen.app_icon_size);
        sIconTextureWidth = sIconTextureHeight = sIconWidth;
    }

    /**
     * Returns a bitmap suitable for the all apps view. Used to convert pre-ICS
     * icon bitmaps that are stored in the database (which were 74x74 pixels at hdpi size)
     * to the proper size (48dp)
     */
    static Bitmap createIconBitmap(Bitmap icon, Context context) {
        int textureWidth = sIconTextureWidth;
        int textureHeight = sIconTextureHeight;
        int sourceWidth = icon.getWidth();
        int sourceHeight = icon.getHeight();
        if (sourceWidth > textureWidth && sourceHeight > textureHeight) {
            // Icon is bigger than it should be; clip it (solves the GB->ICS migration case)
            return Bitmap.createBitmap(icon,
                    (sourceWidth - textureWidth) / 2,
                    (sourceHeight - textureHeight) / 2,
                    textureWidth, textureHeight);
        } else if (sourceWidth == textureWidth && sourceHeight == textureHeight) {
            // Icon is the right size, no need to change it
            return icon;
        } else {
            // Icon is too small, render to a larger bitmap
            final Resources resources = context.getResources();
            return createIconBitmap(new BitmapDrawable(resources, icon), context);
        }
    }

    /**
     * Returns a bitmap suitable for the all apps view.
     */
    static Bitmap createIconBitmap(Drawable icon, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth;
            int height = sIconHeight;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (width < sourceWidth || height < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                } else if (sourceWidth < width && sourceHeight < height) {
                    // Don't scale up the icon
                    width = sourceWidth;
                    height = sourceHeight;
                }
            }

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            final Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);

            final int left = (textureWidth-width) / 2;
            final int top = (textureHeight-height) / 2;

            @SuppressWarnings("all") // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left+width, top+height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left+width, top+height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }

    static public String getDefaultLauncherPackage(PackageManager packageManager) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            if (resolveInfo.activityInfo.packageName.equals("android") == false) {
                //setDefaultLauncherPreference.setSummary(R.string.not_default_youtube_app);
                return resolveInfo.activityInfo.packageName;
            }
        }

        return null;
    }

    static public Intent getSendIntent(String packageName, String className, String urlAsString) {
        // TODO: Retrieve the class name below from the app in case Twitter ever change it.
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setClassName(packageName, className);
        if (packageName.equals(Constant.POCKET_PACKAGE_NAME)) {
            // Stop pocket spawning when links added
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(Intent.EXTRA_TEXT, urlAsString);
        String title = MainApplication.sTitleHashMap != null ? MainApplication.sTitleHashMap.get(urlAsString) : null;
        if (title != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
        }
        return intent;
    }

    static public void setLocale(Context context, String code) {
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getApplicationContext().getResources().updateConfiguration(config, null);
    }

    /*
     * Manually theme divider and title text with @color/apptheme_color
     */
    static public void showThemedDialog(Dialog dialog) {
        dialog.show();

        Resources resources = dialog.getContext().getResources();
        int color = resources.getColor(Settings.get().getDarkThemeEnabled() ? R.color.color_primary_bright : R.color.color_primary);

        int dividerId = resources.getIdentifier("android:id/titleDivider", null, null);
        if (dividerId > 0) {
            View divider = dialog.findViewById(dividerId);
            if (divider != null) {
                divider.setBackgroundColor(color);
            }
        }

        int titleTextViewId = resources.getIdentifier("android:id/alertTitle", null, null);
        if (titleTextViewId > 0) {
            TextView textView = (TextView) dialog.findViewById(titleTextViewId);
            if (textView != null) {
                textView.setTextColor(color);
            }
        }
    }

    private static Integer sDensityDpi;
    /*
     * Use lower density icon on AlertDialogs as large icons look silly
     */
    private static int getAlertIconDensityDpi(Context context) {
        if (sDensityDpi == null) {
            sDensityDpi = context.getResources().getDisplayMetrics().densityDpi;
        }

        switch (sDensityDpi) {
            case DisplayMetrics.DENSITY_LOW:
            case DisplayMetrics.DENSITY_MEDIUM:
                return DisplayMetrics.DENSITY_LOW;
            case DisplayMetrics.DENSITY_TV:
            case DisplayMetrics.DENSITY_HIGH:
                return DisplayMetrics.DENSITY_MEDIUM;
            case DisplayMetrics.DENSITY_XHIGH:
                return DisplayMetrics.DENSITY_HIGH;
            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_XXXHIGH:
                return DisplayMetrics.DENSITY_XHIGH;
        }

        return sDensityDpi;
    }

    public static Drawable getAlertIcon(Context context) {
        return context.getResources().getDrawableForDensity(R.drawable.ic_launcher, getAlertIconDensityDpi(context));
    }

    public static boolean isValidBrowserPackageName(String packageName) {
        if (packageName.equals(BuildConfig.APPLICATION_ID) || packageName.contains("com.digitalashes.tappath")) {
            return false;
        }
        if (BuildConfig.APPLICATION_ID.contains("com.linkbubble") && packageName.contains("com.linkbubble")) {
            return false;
        }
        else if (BuildConfig.APPLICATION_ID.contains("com.brave") && packageName.contains("com.brave")) {
            return false;
        }

        return Arrays.asList(whitelistedBrowsers).contains(packageName);
    }

    public static Integer getSystemActionBarHeight(Context context) {
        // Calculate ActionBar height
        try {
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            {
                return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            }
        } catch (Exception e) {}

        return null;
    }

    public static Integer getSystemStatusBarHeight(Context context) {
        Integer result = null;
        try {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId);
            }
        } catch (Exception e) {}

        return result;
    }

    public static Integer getSystemNavigationBarHeight(Context context) {
        Integer result = null;
        try {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId);
            }
        } catch (Exception e) {}

        return result;
    }

    public static List<ResolveInfo> getLauncherAppForApplicationIds(Context context,
                                                                    String applicationId) {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(applicationId);
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        if (resolveInfos != null && resolveInfos.size() > 0) {
            return resolveInfos;
        }
        return null;
    }

    public static ResolveInfo getLauncherAppForApplicationId(Context context,
                                                             String applicationId) {
        List<ResolveInfo> result = getLauncherAppForApplicationIds(context, applicationId);
        if (result != null && result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    static public Drawable getTintableDrawable(Context context, @DrawableRes int resId) {
        Drawable d = context.getResources().getDrawable(resId);
        d = DrawableCompat.wrap(d);
        return d;
    }

    /**
     * From http://stackoverflow.com/a/5261472/328679
     */
    public static String getDefaultUserAgentString(Context context) {
        if (Build.VERSION.SDK_INT >= 17) {
            return WebSettings.getDefaultUserAgent(context);
        }

        try {
            Constructor<WebSettings> constructor = WebSettings.class.getDeclaredConstructor(Context.class, WebView.class);
            constructor.setAccessible(true);
            try {
                WebSettings settings = constructor.newInstance(context, null);
                return settings.getUserAgentString();
            } finally {
                constructor.setAccessible(false);
            }
        } catch (Exception e) {
            return Config.sIsTablet ? Constant.USER_AGENT_CHROME_TABLET : Constant.USER_AGENT_CHROME_PHONE;
        }
    }

    // Remove http or https from url
    public static String getUrlWithoutHttpHttpsWww(Context context, String url) {
        if (url.startsWith(context.getString(R.string.http_prefix))) {
            url = url.substring(context.getString(R.string.http_prefix).length());
        }
        else if (url.startsWith(context.getString(R.string.https_prefix))) {
            url = url.substring(context.getString(R.string.https_prefix).length());
        }

        if (url.startsWith(context.getString(R.string.www_prefix))) {
            url = url.substring(context.getString(R.string.www_prefix).length());
        }

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    public static String getUrlWithPrefix(Context context, String url) {
        if (!url.startsWith(context.getString(R.string.http_prefix)) &&
                !url.startsWith(context.getString(R.string.https_prefix)))
            return context.getString(R.string.http_prefix) + url;

        return url;
    }

    public static boolean isValidURL(Context context, String url) {

        boolean result = true;

        try {
            URL validURL = new URL(url);
        }
        catch (MalformedURLException e) {
            result = false;
        }

        return result;
    }
}
