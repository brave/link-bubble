package com.chrislacy.linkbubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Vector;

public class Canvas extends FrameLayout {

    private RelativeLayout mLayout;
    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private Vector<BubbleTarget> mTargets = new Vector<BubbleTarget>();

    private final float mMaxAlpha = 0.9f;
    private final float mFadeTime = 0.2f;
    private final float mAlphaDelta = mMaxAlpha / mFadeTime;

    private float mCurrentAlpha = 0.0f;
    private float mTargetAlpha = 0.0f;

    public enum BubbleAction {
        None,

        Destroy,
        OpenBrowser
    }

    public class TargetInfo {

        public TargetInfo() {
            mAction = BubbleAction.None;
            mTargetX = -1;
            mTargetY = -1;
        }

        BubbleAction mAction;
        int mTargetX;
        int mTargetY;
    }

    private class BubbleTarget {
        private RelativeLayout mTargetLayout;
        private ImageView mCircleView;
        private ImageView mImage;

        private float mXFraction;
        private float mYFraction;
        private float mButtonWidth;
        private float mButtonHeight;
        private Circle mSnapCircle;
        private Circle mDefaultCircle;
        private BubbleAction mAction;

        public BubbleTarget(Drawable d, BubbleAction action, float xFraction, float yFraction) {
            Init(d, action, xFraction, yFraction);
        }

        public BubbleTarget(int resId, BubbleAction action, float xFraction, float yFraction) {
            Drawable d = mContext.getResources().getDrawable(resId);
            Init(d, action, xFraction, yFraction);
        }

        private void Init(Drawable d, BubbleAction action, float xFraction, float yFraction) {
            mTargetLayout = new RelativeLayout(mContext);
            mAction = action;
            mXFraction = xFraction;
            mYFraction = yFraction;

            if (d instanceof BitmapDrawable) {
                Bitmap bm = ((BitmapDrawable)d).getBitmap();
                mButtonWidth = bm.getWidth();
                mButtonHeight = bm.getHeight();
            } else {
                mButtonWidth = d.getIntrinsicWidth();
                mButtonHeight = d.getIntrinsicHeight();
            }
            Util.Assert(mButtonWidth > 0);
            Util.Assert(mButtonHeight > 0);

            mImage = new ImageView(mContext);
            mImage.setImageDrawable(d);

            mCircleView = new ImageView(mContext);
            mCircleView.setImageResource(R.drawable.target_default);

            Drawable snapDrawable = mContext.getResources().getDrawable(R.drawable.target_snap);
            float snapWidth = snapDrawable.getIntrinsicWidth();
            float snapHeight = snapDrawable.getIntrinsicHeight();
            Util.Assert(snapWidth > 0 && snapHeight > 0 && snapWidth == snapHeight);
            mSnapCircle = new Circle(Config.mScreenWidth * mXFraction, Config.mScreenHeight * mYFraction, snapWidth * 0.5f);

            Drawable defaultDrawable = mContext.getResources().getDrawable(R.drawable.target_default);
            float defaultWidth = defaultDrawable.getIntrinsicWidth();
            float defaultHeight = defaultDrawable.getIntrinsicHeight();
            Util.Assert(defaultWidth > 0 && defaultHeight > 0 && defaultWidth == defaultHeight);
            mDefaultCircle = new Circle(Config.mScreenWidth * mXFraction, Config.mScreenHeight * mYFraction, defaultWidth * 0.5f);

            mTargetLayout.addView(mCircleView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            RelativeLayout.LayoutParams imageLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imageLP.leftMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonWidth * 0.5f);
            imageLP.topMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonHeight * 0.5f);
            mTargetLayout.addView(mImage, imageLP);

            // Add main relative layout to canvas
            RelativeLayout.LayoutParams targetLayoutLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            targetLayoutLP.leftMargin = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
            targetLayoutLP.topMargin = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);
            mLayout.addView(mTargetLayout, targetLayoutLP);
        }

        public void OnOrientationChanged() {
            mSnapCircle.mX = Config.mScreenWidth * mXFraction;
            mSnapCircle.mY = Config.mScreenHeight * mYFraction;

            mDefaultCircle.mX = Config.mScreenWidth * mXFraction;
            mDefaultCircle.mY = Config.mScreenHeight * mYFraction;

            RelativeLayout.LayoutParams imageLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imageLP.leftMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonWidth * 0.5f);
            imageLP.topMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonHeight * 0.5f);
            mTargetLayout.updateViewLayout(mImage, imageLP);

            // Add main relative layout to canvas
            RelativeLayout.LayoutParams targetLayoutLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            targetLayoutLP.leftMargin = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
            targetLayoutLP.topMargin = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);
            mLayout.updateViewLayout(mTargetLayout, targetLayoutLP);
        }

        public BubbleAction GetAction() {
            return mAction;
        }

        public Circle GetSnapCircle() {
            return mSnapCircle;
        }

        public Circle GetDefaultCircle() {
            return mDefaultCircle;
        }
    }

    public Canvas(Context context) {
        super(context);

        mContext = context;
        mLayout = new RelativeLayout(context);

        applyAlpha();

        addView(mLayout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mTargets.add(new BubbleTarget(android.R.drawable.ic_delete, BubbleAction.Destroy, 0.5f, 0.85f));
        mTargets.add(new BubbleTarget(Config.mBrowserIcon, BubbleAction.OpenBrowser, 0.2f, 0.2f));
        mTargets.add(new BubbleTarget(Config.mBrowserIcon, BubbleAction.OpenBrowser, 0.8f, 0.2f));

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = 0;
        mWindowManagerParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManager.addView(this, mWindowManagerParams);
    }

    public void onOrientationChanged() {
        for (int i=0 ; i < mTargets.size() ; ++i) {
            BubbleTarget bt = mTargets.get(i);
            bt.OnOrientationChanged();
        }
    }

    private void applyAlpha() {
        Util.Assert(mCurrentAlpha >= 0.0f && mCurrentAlpha <= 1.0f);

        int color = ((int)(255 * mCurrentAlpha) << 24);
        mLayout.setBackgroundColor(color);

        if (mCurrentAlpha == 0.0f) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    public void fadeIn() {
        mTargetAlpha = mMaxAlpha;
        MainController.scheduleUpdate();
    }

    public void fadeOut() {
        mTargetAlpha = 0.0f;
        MainController.scheduleUpdate();
    }

    public void update(float dt) {
        if (mCurrentAlpha < mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha + mAlphaDelta * dt, mMaxAlpha);
            MainController.scheduleUpdate();
        } else if (mCurrentAlpha > mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha - mAlphaDelta * dt, mMaxAlpha);
            MainController.scheduleUpdate();
        }
        applyAlpha();
    }

    public TargetInfo getBubbleAction(Circle bubbleCircle) {
        TargetInfo ti = new TargetInfo();

        for (int i=0 ; i < mTargets.size() ; ++i) {
            BubbleTarget bt = mTargets.get(i);

            Circle snapCircle = bt.GetSnapCircle();
            Circle defaultCircle = bt.GetDefaultCircle();

            if (bubbleCircle.Intersects(snapCircle)) {
                ti.mAction = bt.GetAction();
                ti.mTargetX = (int) defaultCircle.mX;
                ti.mTargetY = (int) defaultCircle.mY;
                break;
            }
        }

        return ti;
    }
}