package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.util.Util;
import com.linkbubble.physics.Circle;
import com.squareup.otto.Subscribe;

/**
 * Created by gw on 21/11/13.
 */
public class BubbleTargetView extends RelativeLayout {
    private Context mContext;
    private ImageView mCircleView;
    private ImageView mImage;
    private CanvasView mCanvasView;

    private float mXFraction0;
    private float mXFraction1;
    private float mYFraction0;
    private float mYFraction1;
    private float mButtonWidth;
    private float mButtonHeight;
    private float mSnapWidth;
    private float mSnapHeight;
    private Circle mSnapCircle;
    private Circle mDefaultCircle;
    private Config.BubbleAction mAction;

    private FrameLayout.LayoutParams mCanvasLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private int mHomeX;
    private int mHomeY;

    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private float mAnimPeriod;
    private float mAnimTime;
    private boolean mEnableMove;
    private boolean mIsSnapping;
    private float mTimeSinceSnapping;
    private float mTransitionTimeLeft;

    private final float TRANSITION_TIME = 0.15f;

    private void setTargetPos(int x, int y, float t) {
        if (x != mTargetX || y != mTargetY) {
            mInitialX = mCanvasLayoutParams.leftMargin;
            mInitialY = mCanvasLayoutParams.topMargin;

            mTargetX = x;
            mTargetY = y;

            mAnimPeriod = t;
            mAnimTime = 0.0f;

            MainController.get().scheduleUpdate();
        }
    }

    public BubbleTargetView(CanvasView canvasView, Context context, Config.BubbleAction action, float xf0, float yf0, float xf1, float yf1) {
        super(context);
        Init(canvasView, context, Settings.get().getConsumeBubbleIcon(action), action, xf0, yf0, xf1, yf1);
    }

    public BubbleTargetView(CanvasView canvasView, Context context, int resId, Config.BubbleAction action, float xf0, float yf0, float xf1, float yf1) {
        super(context);
        Drawable d = context.getResources().getDrawable(resId);
        Init(canvasView, context, d, action, xf0, yf0, xf1, yf1);
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

    private void Init(CanvasView canvasView, Context context, Drawable d, Config.BubbleAction action, float xf0, float yf0, float xf1, float yf1) {
        mCanvasView = canvasView;
        mEnableMove = false;
        mContext = context;
        mAction = action;
        mXFraction0 = xf0;
        mYFraction0 = yf0;
        mXFraction1 = xf1;
        mYFraction1 = yf1;

        float xFraction = (mXFraction0 + mXFraction1) * 0.5f;
        float yFraction = (mYFraction0 + mYFraction1) * 0.5f;

        MainApplication.registerForBus(mContext, this);

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
        mSnapCircle = new Circle(Config.mScreenWidth * xFraction, Config.mScreenHeight * yFraction, mSnapWidth * 0.5f);

        Drawable defaultDrawable = mContext.getResources().getDrawable(R.drawable.target_default);
        float defaultWidth = defaultDrawable.getIntrinsicWidth();
        float defaultHeight = defaultDrawable.getIntrinsicHeight();
        Util.Assert(defaultWidth > 0 && defaultHeight > 0 && defaultWidth == defaultHeight);
        mDefaultCircle = new Circle(Config.mScreenWidth * xFraction, Config.mScreenHeight * yFraction, defaultWidth * 0.5f);

        switch (action) {
            case ConsumeLeft:
                mHomeX = (int) -mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case ConsumeRight:
                mHomeX = Config.mScreenWidth + (int) mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case Destroy:
                mHomeX = Config.mScreenCenterX; //mSnapWidth;
                mHomeY = Config.mScreenHeight + (int) mSnapHeight;
                break;
        }

        addView(mCircleView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RelativeLayout.LayoutParams imageLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageLP.leftMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonWidth * 0.5f);
        imageLP.topMargin = (int) (0.5f + mDefaultCircle.mRadius - mButtonHeight * 0.5f);
        addView(mImage, imageLP);

        // Add main relative layout to canvasView
        mCanvasLayoutParams.leftMargin = mHomeX;
        mCanvasLayoutParams.topMargin = mHomeY;
        mCanvasLayoutParams.rightMargin = -100;
        mCanvasLayoutParams.bottomMargin = -100;
        mCanvasView.addView(this, mCanvasLayoutParams);
        setVisibility(GONE);
    }

    public void destroy() {
        MainApplication.unregisterForBus(mContext, this);
    }

    public boolean shouldSnap(Circle bubbleCircle, float radiusScaler) {
        if (mTimeSinceSnapping > 0.5f) {
            Circle snapCircle = GetSnapCircle();

            if (bubbleCircle.Intersects(snapCircle, radiusScaler)) {
                return true;
            }
        }

        return false;
    }

    public void beginSnapping() {
        mIsSnapping = true;
        mAnimPeriod = 0.0f;
        mAnimTime = 0.0f;
    }

    public void endSnapping() {
        mIsSnapping = false;
        mTimeSinceSnapping = 0.0f;
        setTargetPos(mCanvasLayoutParams.leftMargin, mCanvasLayoutParams.topMargin, 0.0f);
    }

    public Config.BubbleAction getAction() {
        return mAction;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        setVisibility(VISIBLE);
        mEnableMove = true;
        mIsSnapping = false;
        mAnimPeriod = 0.0f;
        mAnimTime = 0.0f;
        mTransitionTimeLeft = TRANSITION_TIME;
        mTimeSinceSnapping = 1000.0f;
        MainController.get().scheduleUpdate();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        mEnableMove = false;
        mIsSnapping = false;
        mAnimPeriod = 0.0f;
        mAnimTime = 0.0f;
        setTargetPos(mHomeX, mHomeY, TRANSITION_TIME);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDraggableBubbleMovedEvent(MainController.DraggableBubbleMovedEvent e) {
        if (mEnableMove) {
            int x0 = (int) (mXFraction0 * Config.mScreenWidth - Config.mBubbleWidth * 0.5f);
            int x1 = (int) (mXFraction1 * Config.mScreenWidth - Config.mBubbleWidth * 0.5f);
            int targetX = Util.clamp(x0, e.mX, x1);
            mSnapCircle.mX = targetX + Config.mBubbleWidth * 0.5f;

            int y0 = (int) (mYFraction0 * Config.mScreenHeight - Config.mBubbleHeight * 0.5f);
            int y1 = (int) (mYFraction1 * Config.mScreenHeight - Config.mBubbleHeight * 0.5f);
            int targetY = Util.clamp(y0, e.mY, y1);
            mSnapCircle.mY = targetY + Config.mBubbleHeight * 0.5f;

            mSnapCircle.mY = Util.clamp(0, mSnapCircle.mY, Config.mScreenHeight - mDefaultCircle.mRadius);

            mDefaultCircle.mX = mSnapCircle.mX;
            mDefaultCircle.mY = mSnapCircle.mY;

            int x = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
            int y = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);

            float d = Util.distance(x, y, mCanvasLayoutParams.leftMargin, mCanvasLayoutParams.topMargin);

            float v = Config.mScreenWidth;      // Move 'screenWidth' pixels / second
            float t = d / v;

            float remainingTime = Math.max(t, mTransitionTimeLeft);
            setTargetPos(x, y, remainingTime);
        }
    }

    public void update(float dt) {
        if (!mIsSnapping) {
            mTimeSinceSnapping += dt;
        }

        if (mTransitionTimeLeft > 0.0f) {
            mTransitionTimeLeft -= dt;
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
            mCanvasView.updateViewLayout(this, mCanvasLayoutParams);

            MainController.get().scheduleUpdate();
        } else if (!mEnableMove) {
            setVisibility(GONE);
        }
    }

    public void OnOrientationChanged() {
        mTransitionTimeLeft = 0.0f;
        mAnimTime = 0.0f;
        mAnimPeriod = 0.0f;

        float xf = (mXFraction0 + mXFraction1) * 0.5f;
        float yf = (mYFraction0 + mYFraction1) * 0.5f;

        mSnapCircle.mX = Config.mScreenWidth * xf;
        mSnapCircle.mY = Config.mScreenHeight * xf;

        mDefaultCircle.mX = Config.mScreenWidth * xf;
        mDefaultCircle.mY = Config.mScreenHeight * yf;

        switch (mAction) {
            case ConsumeLeft:
                mHomeX = (int) -mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case ConsumeRight:
                mHomeX = Config.mScreenWidth + (int) mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case Destroy:
                mHomeX = Config.mScreenCenterX;;
                mHomeY = Config.mScreenHeight + (int) mSnapHeight;
                break;
        }

        mCanvasLayoutParams.leftMargin = mHomeX;
        mCanvasLayoutParams.topMargin = mHomeY;
        mCanvasView.updateViewLayout(this, mCanvasLayoutParams);
    }

    public Circle GetSnapCircle() {
        return mSnapCircle;
    }

    public Circle GetDefaultCircle() {
        return mDefaultCircle;
    }
}
