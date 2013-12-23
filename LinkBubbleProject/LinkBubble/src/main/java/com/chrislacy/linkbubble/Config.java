package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import java.util.List;

/**
 * Created by gw on 2/10/13.
 */
public class Config {

    static boolean USE_CONTENT_ACTIVITY = false;

    public enum BubbleAction {
        None,
        Destroy,
        ConsumeRight,
        ConsumeLeft
    }

    enum ActionType {
        Unknown,
        View,
        Share,
    }

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

    public static int MAX_BUBBLES;

    public static int BUBBLE_HOME_X;
    public static int BUBBLE_HOME_Y;

    public static void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(mDm);

        Drawable d = context.getResources().getDrawable(R.drawable.bubble_active);
        mBubbleWidth = d.getIntrinsicWidth();
        mBubbleHeight = d.getIntrinsicHeight();

        d = context.getResources().getDrawable(R.drawable.target_snap);

        mScreenCenterX = (int) (mDm.widthPixels * 0.5f);
        mScreenHeight = mDm.heightPixels - getStatusBarHeight(context);
        mScreenWidth = mDm.widthPixels;

        int maxBubbles1 = (int) ((mDm.widthPixels - mBubbleWidth - mBubbleWidth * 0.5f) / (Config.mBubbleWidth * 1.2f));
        int maxBubbles2 = (int) ((mDm.heightPixels - mBubbleWidth - mBubbleWidth * 0.5f) / (Config.mBubbleWidth * 1.2f));
        MAX_BUBBLES = Math.min(maxBubbles1, maxBubbles2);
        if (MAX_BUBBLES < 5) {
            MAX_BUBBLES = 5;
        }

        mBubbleSnapLeftX = (int) (-mBubbleWidth * 0.2f);
        mBubbleSnapRightX = (int) (mDm.widthPixels - mBubbleWidth * 0.8f);
        mBubbleMinY = (int) 0; //(mContentOffset + mBubbleHeight * 0.15f);
        mBubbleMaxY = (int) (mDm.heightPixels - mBubbleHeight); //(mDm.heightPixels - 1.15f * mBubbleHeight);

        mContentViewBubbleX = (int) (mDm.widthPixels - mBubbleWidth - mBubbleWidth * 0.5f);
        mContentViewBubbleY = (int) (mBubbleHeight * 0.15f);

        mContentOffset = (int) (mBubbleHeight * 1.2f);

        BUBBLE_HOME_X = Config.mBubbleSnapLeftX;
        BUBBLE_HOME_Y = (int) (Config.mScreenHeight * 0.4f);
    }

    private static int getStatusBarHeight(Context context) {
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

    public static final String GOOGLE_PLAY_STORE_URL_PREFIX = "http://play.google.com/store/apps/details?id=";
    public static final String GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending";
    public static final String GOOGLE_PLAY_STORE_PRO_URL = GOOGLE_PLAY_STORE_URL_PREFIX + "com.chrislacy.actionlauncher.pro";

    public static final String STORE_URL_PREFIX = GOOGLE_PLAY_STORE_URL_PREFIX;
    public static final String STORE_PACKAGE = GOOGLE_PLAY_STORE_PACKAGE;
    public static final String STORE_PRO_URL = GOOGLE_PLAY_STORE_PRO_URL;

    public static Intent getStoreIntent(Context context, String storeProUrl) {
        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(storeProUrl));
        List<ResolveInfo> infos = manager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : infos) {
            IntentFilter filter = info.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                if (info.activityInfo.packageName.equals(STORE_PACKAGE)) {
                    Intent result = new Intent(Intent.ACTION_VIEW);
                    result.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    result.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    result.setData(Uri.parse(storeProUrl));
                    return result;
                }
            }
        }

        return null;
    }

    public static final String SET_DEFAULT_BROSWER_URL = "http://linkbubble.com/configure";
}
