package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
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

    public static Drawable mBrowserIcon;

    public static int mContentViewBubbleY;
    public static int mContentViewBubbleX;

    public static int mContentOffset;

    public static int MAX_BUBBLES;

    public static void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(mDm);

        Drawable d = context.getResources().getDrawable(R.drawable.bubble_active);
        mBubbleWidth = d.getIntrinsicWidth();
        mBubbleHeight = d.getIntrinsicHeight();

        d = context.getResources().getDrawable(R.drawable.target_snap);

        mScreenCenterX = (int) (mDm.widthPixels * 0.5f);
        mScreenHeight = mDm.heightPixels;
        mScreenWidth = mDm.widthPixels;

        int maxBubbles1 = (int) ((mDm.widthPixels - mBubbleWidth - mBubbleWidth * 0.5f) / (Config.mBubbleWidth * 1.2f));
        int maxBubbles2 = (int) ((mDm.heightPixels - mBubbleWidth - mBubbleWidth * 0.5f) / (Config.mBubbleWidth * 1.2f));
        MAX_BUBBLES = Math.min(maxBubbles1, maxBubbles2);

        mBubbleSnapLeftX = (int) (-mBubbleWidth * 0.2f);
        mBubbleSnapRightX = (int) (mDm.widthPixels - mBubbleWidth * 0.8f);
        mBubbleMinY = (int) 0; //(mContentOffset + mBubbleHeight * 0.15f);
        mBubbleMaxY = (int) (mDm.heightPixels - 2 * mBubbleHeight); //(mDm.heightPixels - 1.15f * mBubbleHeight);

        mContentViewBubbleX = (int) (mDm.widthPixels - mBubbleWidth - mBubbleWidth * 0.5f);
        mContentViewBubbleY = (int) (mBubbleHeight * 0.15f);

        mContentOffset = (int) (mBubbleHeight * 1.2f);

        PackageManager pk = context.getPackageManager();
        try {
            mBrowserIcon = pk.getApplicationIcon("com.android.browser");
        } catch (Exception e) {
            mBrowserIcon = context.getResources().getDrawable(R.drawable.target_snap);
        }
    }

    public static float getContentViewX(int bubbleIndex) {
        float x = Config.mContentViewBubbleX - Config.mBubbleWidth * 1.2f * bubbleIndex;
        return x;
    }

    public static int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDm);
    }
}
