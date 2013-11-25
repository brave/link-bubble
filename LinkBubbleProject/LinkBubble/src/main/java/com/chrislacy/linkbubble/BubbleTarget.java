package com.chrislacy.linkbubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by gw on 21/11/13.
 */
public class BubbleTarget extends RelativeLayout {
    private Context mContext;
    private ImageView mCircleView;
    private ImageView mImage;
    private Canvas mCanvas;

    private float mXFraction;
    private float mYFraction;
    private float mButtonWidth;
    private float mButtonHeight;
    private float mSnapWidth;
    private float mSnapHeight;
    private Circle mSnapCircle;
    private Circle mDefaultCircle;
    private Config.BubbleAction mAction;

    private RelativeLayout.LayoutParams mCanvasLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private float mAnimPeriod;
    private float mAnimTime;

    private void setTargetPos(int x, int y, float t) {
        if (x != mTargetX || y != mTargetY) {
            mInitialX = mCanvasLayoutParams.leftMargin;
            mInitialY = mCanvasLayoutParams.topMargin;

            mTargetX = x;
            mTargetY = y;

            mAnimPeriod = t;
            mAnimTime = 0.0f;

            MainController.scheduleUpdate();
        }
    }

    public BubbleTarget(Canvas canvas, Context context, Config.BubbleAction action, float xFraction, float yFraction) {
        super(context);
        Init(canvas, context, Settings.get().getConsumeBubbleIcon(action), action, xFraction, yFraction);
    }

    public BubbleTarget(Canvas canvas, Context context, int resId, Config.BubbleAction action, float xFraction, float yFraction) {
        super(context);
        Drawable d = context.getResources().getDrawable(resId);
        Init(canvas, context, d, action, xFraction, yFraction);
    }

    public void onConsumeBubblesChanged() {
        Drawable d = null;

        switch (mAction) {
            case ConsumeLeft:
            case ConsumeRight:
                d = Settings.get().getConsumeBubbleIcon(mAction);
                break;
            default:
                break;
        }

        if (d != null) {

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

            mImage.setImageDrawable(d);

            RelativeLayout.LayoutParams imageLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imageLP.leftMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonWidth * 0.5f);
            imageLP.topMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonHeight * 0.5f);
            updateViewLayout(mImage, imageLP);
        }
    }

    private void Init(Canvas canvas, Context context, Drawable d, Config.BubbleAction action, float xFraction, float yFraction) {
        mCanvas = canvas;
        mContext = context;
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
        mSnapWidth = snapDrawable.getIntrinsicWidth();
        mSnapHeight = snapDrawable.getIntrinsicHeight();
        Util.Assert(mSnapWidth > 0 && mSnapHeight > 0 && mSnapWidth == mSnapHeight);
        mSnapCircle = new Circle(Config.mScreenWidth * mXFraction, Config.mScreenHeight * mYFraction, mSnapWidth * 0.5f);

        Drawable defaultDrawable = mContext.getResources().getDrawable(R.drawable.target_default);
        float defaultWidth = defaultDrawable.getIntrinsicWidth();
        float defaultHeight = defaultDrawable.getIntrinsicHeight();
        Util.Assert(defaultWidth > 0 && defaultHeight > 0 && defaultWidth == defaultHeight);
        mDefaultCircle = new Circle(Config.mScreenWidth * mXFraction, Config.mScreenHeight * mYFraction, defaultWidth * 0.5f);

        addView(mCircleView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RelativeLayout.LayoutParams imageLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageLP.leftMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonWidth * 0.5f);
        imageLP.topMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonHeight * 0.5f);
        addView(mImage, imageLP);

        // Add main relative layout to canvas
        mCanvasLayoutParams.leftMargin = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
        mCanvasLayoutParams.topMargin = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);
        mCanvasLayoutParams.rightMargin = -100;
        mCanvasLayoutParams.bottomMargin = -100;
        mCanvas.addView(this, mCanvasLayoutParams);
    }

    public void update(float dt, Bubble bubble) {
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

            setTargetPos((int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius),
                    (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius), 0.03f);
        }

        if (mAnimTime < mAnimPeriod) {
            Util.Assert(mAnimPeriod > 0.0f);

            mAnimTime = Util.clamp(0.0f, mAnimTime + dt, mAnimPeriod);

            float tf = mAnimTime / mAnimPeriod;
            float interpolatedFraction;
            interpolatedFraction = mLinearInterpolator.getInterpolation(tf);
            Util.Assert(interpolatedFraction >= 0.0f && interpolatedFraction <= 1.0f);

            int x = (int) (mInitialX + (mTargetX - mInitialX) * interpolatedFraction);
            int y = (int) (mInitialY + (mTargetY - mInitialY) * interpolatedFraction);

            mCanvasLayoutParams.leftMargin = x;
            mCanvasLayoutParams.topMargin = y;
            mCanvas.updateViewLayout(this, mCanvasLayoutParams);

            MainController.scheduleUpdate();
        }
    }

    public void OnOrientationChanged() {
        mSnapCircle.mX = Config.mScreenWidth * mXFraction;
        mSnapCircle.mY = Config.mScreenHeight * mYFraction;

        mDefaultCircle.mX = Config.mScreenWidth * mXFraction;
        mDefaultCircle.mY = Config.mScreenHeight * mYFraction;

        // Add main relative layout to canvas
        RelativeLayout.LayoutParams targetLayoutLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        targetLayoutLP.leftMargin = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
        targetLayoutLP.topMargin = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);
        mCanvas.updateViewLayout(this, targetLayoutLP);
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
