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

    public static int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDm);
    }
}
