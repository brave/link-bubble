package com.linkbubble;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;

/**
 * Created by gw on 2/10/13.
 */
public class Config {

    public static DisplayMetrics mDm = new DisplayMetrics();

    public static int mScreenCenterX;
    public static int mScreenHeight;
    public static int mScreenWidth;

    public static int mBubbleSnapLeftX;
    public static int mBubbleSnapRightX;
    public static int mBubbleMinY;
    public static int mBubbleMaxY;

    public static float mBubbleWidth;
    public static float mBubbleHeight;

    public static int mContentViewBubbleY;
    public static int mContentViewBubbleX;

    public static int mContentOffset;

    public static int sDensityDpi;

    public static boolean sIsTablet;

    public static float ANIMATE_TO_SNAP_TIME = 0.1f;
    public static float CLOSE_ALL_BUBBLES_DELAY = 0.67f;

    public static void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(mDm);

        int bubbleSize = Settings.get().getBubbleSize();
        mBubbleWidth = mBubbleHeight = context.getResources().getDimensionPixelSize(bubbleSize == 0 ? R.dimen.bubble_size_small : R.dimen.bubble_size);

        mScreenCenterX = (int) (mDm.widthPixels * 0.5f);
        mScreenHeight = mDm.heightPixels - getStatusBarHeight(context);
        mScreenWidth = mDm.widthPixels;

        mBubbleSnapLeftX = (int) (-mBubbleWidth * 0.2f);
        mBubbleSnapRightX = (int) (mDm.widthPixels - mBubbleWidth * 0.8f);
        mBubbleMinY = 0; //(mContentOffset + mBubbleHeight * 0.15f);
        mBubbleMaxY = (int) (mDm.heightPixels - mBubbleHeight); //(mDm.heightPixels - 1.15f * mBubbleHeight);

        mContentViewBubbleX = (int) (mDm.widthPixels - mBubbleWidth - mBubbleWidth * 0.5f);
        mContentViewBubbleY = context.getResources().getDimensionPixelSize(R.dimen.content_bubble_y_offset);

        mContentOffset = context.getResources().getDimensionPixelSize(R.dimen.content_offset);

        sDensityDpi = mDm.densityDpi;

        sIsTablet = context.getResources().getBoolean(R.bool.is_tablet);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 33;        // Guess 33 if we can't find the resource as this is what the value is on a N7.
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static float getContentViewX(int bubbleIndex, int bubbleCount) {

        float spaceUsed = bubbleCount * Config.mBubbleWidth + (bubbleCount-1) * Config.mBubbleWidth * 0.2f;
        float x0 = Config.mScreenCenterX - spaceUsed * 0.5f;
        return x0 + bubbleIndex * Config.mBubbleWidth * 1.2f;

        /*
        if (bubbleIndex == 0) {
            return Config.mScreenCenterX;
        } else if ((bubbleIndex & 1) == 0) {
            return Config.mScreenCenterX + (bubbleIndex/2) * Config.mBubbleWidth * 1.2f;
        } else {
            return Config.mScreenCenterX - (1+bubbleIndex/2) * Config.mBubbleWidth * 1.2f;
        }*/
    }

    public static int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDm);
    }

    public static final String SET_DEFAULT_BROWSER_URL = "http://linkbubble.com/configure";

    public static final String YOUTUBE_WATCH_PREFIX = "http://www.youtube.com/watch?v=";
    public static final String YOUTUBE_EMBED_PATH_SUFFIX = "embed/";
    public static final String YOUTUBE_EMBED_PREFIX = "//www.youtube.com/" + YOUTUBE_EMBED_PATH_SUFFIX;

    public static final String YOUTUBE_API_KEY = "AIzaSyChiS6yef7AIe5p0JvJGnHrHmmimehIuDs";
    public static final String YOUTUBE_API_THUMBNAILS_LOW_QUALITY = "thumbnails(default)";
    public static final String YOUTUBE_API_THUMBNAILS_HIGH_QUALITY = "thumbnails(default,medium)";

    private static long sMaxMemory = -1;
    public static boolean isLowMemoryDevice() {
        if (sMaxMemory == -1) {
            sMaxMemory = Runtime.getRuntime().maxMemory();
            Log.d("LinkBubble", "maxMemory=" + (sMaxMemory/1024/1024) + "MB");
        }

        if (sMaxMemory <= 32 * 1024 * 1024) {
            return true;
        }

        return false;
    }
}
