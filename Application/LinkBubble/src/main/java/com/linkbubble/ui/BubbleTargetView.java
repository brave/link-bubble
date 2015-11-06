package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Circle;
import com.linkbubble.util.Util;
import com.squareup.otto.Subscribe;

/**
 * Created by gw on 21/11/13.
 */
public class BubbleTargetView extends FrameLayout {
    private ImageView mImage;
    private CanvasView mCanvasView;

    public enum Interpolator {
        Linear,
        Overshoot
    }

    private HorizontalAnchor mHAnchor;
    private VerticalAnchor mVAnchor;
    private int mDefaultX;
    private int mDefaultY;
    private int mMaxOffsetX;
    private int mMaxOffsetY;
    private int mTractorOffsetX;
    private int mTractorOffsetY;
    private float mSnapWidth;
    private float mSnapHeight;
    private Circle mSnapCircle;
    private Circle mDefaultCircle;
    private Constant.BubbleAction mAction;

    private FrameLayout.LayoutParams mCanvasLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private int mHomeX;
    private int mHomeY;

    public enum HorizontalAnchor {
        Left,
        Center,
        Right
    }

    public enum VerticalAnchor {
        Top,
        Bottom
    }

    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator(1.5f);
    private boolean mIsSnapping;
    private boolean mIsLongHovering;
    private static boolean sEnableTractor;
    private float mTimeSinceSnapping;

    private final float TRANSITION_TIME = 0.15f;

    public BubbleTargetView(Context context) {
        this(context, null);
    }

    public BubbleTargetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTargetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mImage = (ImageView) findViewById(R.id.image_view);
    }

    protected float getRadius() {
        int tabSize = getResources().getDimensionPixelSize(R.dimen.bubble_icon_size);
        return tabSize * 0.5f;
    }

    private int getXPos() {
        switch (mHAnchor) {
            case Left:
                return mDefaultX;
            case Right:
                return Config.mScreenWidth - mDefaultX;
            case Center:
                return (int) (Config.mScreenWidth * 0.5f + mDefaultX);
        }

        Util.Assert(false, "Anchor not handled - " + mHAnchor);
        return 0;
    }

    private int getYPos() {
        switch (mVAnchor) {
            case Top:
                return mDefaultY;
            case Bottom:
                return Config.mScreenHeight - mDefaultY;
        }

        Util.Assert(false, "Anchor not handled - " + mVAnchor);
        return 0;
    }

    public void setTargetCenter(int x, int y) {
        setTargetPos((int) (x - mSnapWidth * 0.5f), (int) (y - mSnapHeight * 0.5f));
    }

    public void setTargetPos(int x, int y) {
        mCanvasLayoutParams.leftMargin = x;
        mCanvasLayoutParams.topMargin = y;
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
            mImage.setImageDrawable(d);
        }
    }

    public void configure(CanvasView canvasView, Context context, Drawable d, Constant.BubbleAction action, int defaultX, HorizontalAnchor hAnchor,
                      int defaultY, VerticalAnchor vAnchor, int maxOffsetX, int maxOffsetY,
                      int tractorOffsetX, int tractorOffsetY) {
        mCanvasView = canvasView;
        mAction = action;

        mHAnchor = hAnchor;
        mVAnchor = vAnchor;
        mDefaultX = defaultX;
        mDefaultY = defaultY;
        mMaxOffsetX = maxOffsetX;
        mMaxOffsetY = maxOffsetY;
        mTractorOffsetX = tractorOffsetX;
        mTractorOffsetY = tractorOffsetY;

        registerForBus();

        if (d != null && mImage != null) {
            mImage.setImageDrawable(d);
        }

        int bubbleIconSize = getResources().getDimensionPixelSize(R.dimen.bubble_icon_size);
        mSnapWidth = bubbleIconSize;
        mSnapHeight = bubbleIconSize;
        Util.Assert(mSnapWidth > 0 && mSnapHeight > 0 && mSnapWidth == mSnapHeight, "mSnapWidth:" + mSnapWidth + ", mSnapHeight:" + mSnapHeight);
        mSnapCircle = new Circle(getXPos(), getYPos(), mSnapWidth * 0.5f);

        float r = getRadius();
        Util.Assert(r > 0.0f, "r:" + r);
        mDefaultCircle = new Circle(getXPos(), getYPos(), r);

        switch (action) {
            case ConsumeLeft:
                mHomeX = (int) -mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case ConsumeRight:
                mHomeX = Config.mScreenWidth + (int) mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case Close:
                mHomeX = Config.mScreenCenterX; //mSnapWidth;
                mHomeY = Config.mScreenHeight + (int) mSnapHeight;
                break;
        }

        // Add main relative layout to canvasView
        mCanvasLayoutParams.leftMargin = mHomeX;
        mCanvasLayoutParams.topMargin = mHomeY;
        mCanvasLayoutParams.rightMargin = -100;
        mCanvasLayoutParams.bottomMargin = -100;
        mCanvasView.addView(this, mCanvasLayoutParams);
        setVisibility(GONE);
    }

    public void getOffsetDebugRegion(Rect r) {
        int xMaxOffset = mMaxOffsetX;
        int yMaxOffset = mMaxOffsetY;

        if (sEnableTractor) {
            xMaxOffset = mTractorOffsetX;
            yMaxOffset = mTractorOffsetY;
        }

        int x0 = (int) (0.5f + getXPos() - xMaxOffset - Config.mBubbleWidth * 0.5f);
        int x1 = (int) (0.5f + getXPos() + xMaxOffset + Config.mBubbleWidth * 0.5f);

        int y0 = (int) (0.5f + getYPos() - yMaxOffset - Config.mBubbleHeight * 0.5f);
        int y1 = (int) (0.5f + getYPos() + yMaxOffset + Config.mBubbleHeight * 0.5f);

        r.left = x0;
        r.right = x1;
        r.top = y0;
        r.bottom = y1;
    }

    public void getTractorDebugRegion(Rect r) {
        int xMaxOffset = mTractorOffsetX;
        int yMaxOffset = mTractorOffsetY;

        int x0 = (int) (0.5f + getXPos() - xMaxOffset - Config.mBubbleWidth * 0.5f);
        int x1 = (int) (0.5f + getXPos() + xMaxOffset + Config.mBubbleWidth * 0.5f);

        int y0 = (int) (0.5f + getYPos() - yMaxOffset - Config.mBubbleHeight * 0.5f);
        int y1 = (int) (0.5f + getYPos() + yMaxOffset + Config.mBubbleHeight * 0.5f);

        r.left = x0;
        r.right = x1;
        r.top = y0;
        r.bottom = y1;
    }

    public void destroy() {
        unregisterForBus();
    }

    protected void registerForBus() {
        MainApplication.registerForBus(getContext(), this);
    }

    protected void unregisterForBus() {
        MainApplication.unregisterForBus(getContext(), this);
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

    public static void enableTractor() {
        sEnableTractor = true;
    }

    public static void disableTractor() {
        sEnableTractor = false;
    }

    public void beginSnapping() {
        mIsSnapping = true;
    }

    public void endSnapping() {
        mIsSnapping = false;
        mTimeSinceSnapping = 0.0f;
        setTargetPos(mCanvasLayoutParams.leftMargin, mCanvasLayoutParams.topMargin);
    }

    public void beginLongHovering() {
        mIsLongHovering = true;
    }

    public void endLongHovering() {
        mIsLongHovering = false;
    }

    public boolean isLongHovering() {
        return mIsLongHovering;
    }

    public Constant.BubbleAction getAction() {
        return mAction;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setVisibility(VISIBLE);
            }
        }, Constant.TARGET_BUBBLE_APPEAR_TIME);

        mIsSnapping = false;
        mTimeSinceSnapping = 1000.0f;

        mSnapCircle.mX = (0.5f + getXPos());
        mSnapCircle.mY = Util.clamp(0, 0.5f + getYPos() + mMaxOffsetY, Config.mScreenHeight - mDefaultCircle.mRadius);

        mDefaultCircle.mX = mSnapCircle.mX;
        mDefaultCircle.mY = mSnapCircle.mY;

        int x = (int) (0.5f + mDefaultCircle.mX - mDefaultCircle.mRadius);
        int y = (int) (0.5f + mDefaultCircle.mY - mDefaultCircle.mRadius);

        setTargetPos(x, y);
        MainController.get().scheduleUpdate();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setVisibility(GONE);
            }
        }, Constant.TARGET_BUBBLE_APPEAR_TIME);

        mIsSnapping = false;
        setTargetPos(mHomeX, mHomeY);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDraggableBubbleMovedEvent(MainController.DraggableBubbleMovedEvent e) {
    }

    public void update(float dt) {
        if (!mIsSnapping) {
            mTimeSinceSnapping += dt;
        }
    }

    public void OnOrientationChanged() {
        mSnapCircle.mX = getXPos();
        mSnapCircle.mY = getYPos();

        mDefaultCircle.mX = mSnapCircle.mX;
        mDefaultCircle.mY = mSnapCircle.mY;

        switch (mAction) {
            case ConsumeLeft:
                mHomeX = (int) -mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case ConsumeRight:
                mHomeX = Config.mScreenWidth + (int) mSnapWidth;
                mHomeY = (int) -mSnapHeight;
                break;
            case Close:
                mHomeX = Config.mScreenCenterX;
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
