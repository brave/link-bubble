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

public class Canvas extends RelativeLayout {

    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private BubbleTarget mDeleteTarget;
    private Vector<BubbleTarget> mTargets = new Vector<BubbleTarget>();

    private final float mMaxAlpha = 0.9f;
    private final float mFadeTime = 0.2f;
    private final float mAlphaDelta = mMaxAlpha / mFadeTime;

    private float mCurrentAlpha = 0.0f;
    private float mTargetAlpha = 0.0f;

    private boolean mEnabled;

    private ContentView mContentView;

    public class TargetInfo {

        public TargetInfo() {
            mAction = Config.BubbleAction.None;
            mTargetX = -1;
            mTargetY = -1;
        }

        Config.BubbleAction mAction;
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
        private Config.BubbleAction mAction;

        public BubbleTarget(Config.BubbleAction action, float xFraction, float yFraction) {
            Init(Settings.get().getConsumeBubbleIcon(action), action, xFraction, yFraction);
        }

        public BubbleTarget(int resId, Config.BubbleAction action, float xFraction, float yFraction) {
            Drawable d = mContext.getResources().getDrawable(resId);
            Init(d, action, xFraction, yFraction);
        }

        private void Init(Drawable d, Config.BubbleAction action, float xFraction, float yFraction) {
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
            addView(mTargetLayout, targetLayoutLP);
        }

        private void update(Bubble bubble) {
            if (!bubble.isSnapping()) {
                float xf = (bubble.getXPos() + Config.mBubbleWidth * 0.5f) / Config.mScreenWidth;
                xf = 2.0f * Util.clamp(0.0f, xf, 1.0f) - 1.0f;
                Util.Assert(xf >= -1.0f && xf <= 1.0f);

                mSnapCircle.mX = Config.mScreenWidth * mXFraction + xf * Config.mScreenWidth * 0.1f;

                int bubbleYC = (int) (bubble.getYPos() + Config.mBubbleHeight * 0.5f);
                int bubbleY0 = (int) (Config.mScreenHeight * 0.75f);
                int bubbleY1 = (int) (Config.mScreenHeight * 0.90f);

                int targetY0 = (int) (Config.mScreenHeight * mYFraction);
                int targetY1 = (int) (Config.mScreenHeight * (mYFraction + 0.05f));

                if (bubbleYC < bubbleY0) {
                    mSnapCircle.mY = Config.mScreenHeight * mYFraction;
                } else if (bubbleYC < bubbleY1) {
                    float yf = (float)(bubbleYC - bubbleY0) / (float)(bubbleY1 - bubbleY0);
                    mSnapCircle.mY = Config.mScreenHeight * mYFraction + yf * (targetY1 - targetY0);
                } else {
                    mSnapCircle.mY = bubbleYC;
                }

                mDefaultCircle.mX = mSnapCircle.mX;
                mDefaultCircle.mY = mSnapCircle.mY;

                RelativeLayout.LayoutParams imageLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                imageLP.leftMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonWidth * 0.5f);
                imageLP.topMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonHeight * 0.5f);
                mTargetLayout.updateViewLayout(mImage, imageLP);

                // Add main relative layout to canvas
                RelativeLayout.LayoutParams targetLayoutLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                targetLayoutLP.leftMargin = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
                targetLayoutLP.topMargin = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);
                updateViewLayout(mTargetLayout, targetLayoutLP);
            }
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
            updateViewLayout(mTargetLayout, targetLayoutLP);
        }

        public Config.BubbleAction GetAction() {
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
        mEnabled = true;

        applyAlpha();

        mDeleteTarget = new BubbleTarget(android.R.drawable.ic_delete, Config.BubbleAction.Destroy, 0.5f, 0.85f);
        mTargets.add(mDeleteTarget);

        mTargets.add(new BubbleTarget(Config.BubbleAction.ConsumeLeft, 0.2f, 0.2f));
        mTargets.add(new BubbleTarget(Config.BubbleAction.ConsumeRight, 0.8f, 0.2f));

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

    public void setContentView(ContentView cv) {
        if (mContentView != null) {
            removeView(mContentView);
        }
        mContentView = cv;
        if (mContentView != null) {
            RelativeLayout.LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            p.topMargin = Config.mContentOffset;
            addView(mContentView, p);
        }
    }

    private void applyAlpha() {
        Util.Assert(mCurrentAlpha >= 0.0f && mCurrentAlpha <= 1.0f);

        int color = ((int)(255 * mCurrentAlpha) << 24);
        setBackgroundColor(color);

        if (!mEnabled || mCurrentAlpha == 0.0f) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    public void setContentViewTranslation(float ty) {
        Util.Assert(mContentView != null);
        mContentView.setTranslationY(ty);
    }

    public void showContentView() {
        Util.Assert(mContentView != null);
        mContentView.setVisibility(VISIBLE);
    }

    public void hideContentView() {
        Util.Assert(mContentView != null);
        mContentView.setVisibility(GONE);
    }

    public void enable(boolean enable) {
        mEnabled = enable;
        applyAlpha();
    }

    public void fadeIn() {
        mTargetAlpha = mMaxAlpha;
        MainController.scheduleUpdate();
    }

    public void fadeOut() {
        mTargetAlpha = 0.0f;
        MainController.scheduleUpdate();
    }

    public void update(float dt, Bubble frontBubble) {
        if (mCurrentAlpha < mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha + mAlphaDelta * dt, mMaxAlpha);
            MainController.scheduleUpdate();
        } else if (mCurrentAlpha > mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha - mAlphaDelta * dt, mMaxAlpha);
            MainController.scheduleUpdate();
        }
        applyAlpha();

        if (frontBubble != null) {
            mDeleteTarget.update(frontBubble);
        }
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