package com.linkbubble.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
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

import java.util.Vector;

public class CanvasView extends FrameLayout {

    private static final String TAG = "CanvasView";

    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private Vector<BubbleTargetView> mTargets = new Vector<BubbleTargetView>();
    private ImageView mTopMaskView;
    private ImageView mBottomMaskView;

    private final float mMaxAlpha = 1.0f;
    private final float mFadeTime = 0.3f;
    private final float mAlphaDelta = mMaxAlpha / mFadeTime;
    //private final float mAlphaDelta = 1.33f * (mMaxAlpha / (Constant.BUBBLE_ANIM_TIME / 1000.f));

    private float mCurrentAlpha = 0.0f;
    private float mTargetAlpha = 0.0f;

    private float mCurrentAlphaContentView = 1.0f;
    private float mTargetAlphaContentView = 1.0f;

    private float mAnimTime;
    private float mAnimPeriod;
    private float mInitialY;
    private float mTargetY;

    private int mContentViewY;

    private boolean mEnabled;

    private ContentView mContentView;

    private Paint mTargetOffsetDebugPaint;
    private Paint mTargetTractorDebugPaint;
    private Rect mTargetDebugRect;

    private Util.ClipResult mClipResult = new Util.ClipResult();
    private Util.Point mClosestPoint = new Util.Point();
    private Rect mTractorRegion = new Rect();

    private ImageView mStatusBarCoverView;

    private ExpandedActivity.MinimizeExpandedActivityEvent mMinimizeExpandedActivityEvent = new ExpandedActivity.MinimizeExpandedActivityEvent();

    public CanvasView(Context context) {
        super(context);

        MainApplication.registerForBus(context, this);

        mEnabled = true;
        mContentViewY = Config.mScreenHeight - Config.mContentOffset;
        mTargetY = mContentViewY;
        int canvasMaskHeight = getResources().getDimensionPixelSize(R.dimen.canvas_mask_height);

        if (Constant.COVER_STATUS_BAR) {
            int statusBarHeight = Util.getSystemStatusBarHeight(context);

            mStatusBarCoverView = new ImageView(context);
            mStatusBarCoverView.setImageResource(R.drawable.masked_status_bar);
            mStatusBarCoverView.setScaleType(ImageView.ScaleType.FIT_XY);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.x = 0;
            lp.y = -statusBarHeight;
            lp.height = statusBarHeight;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            lp.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            lp.format = PixelFormat.TRANSPARENT;

            mStatusBarCoverView.setLayoutParams(lp);

            MainController.addRootWindow(mStatusBarCoverView, lp);
        }

        Resources resources = getResources();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (Constant.TOP_CANVAS_MASK) {
            mTopMaskView = new ImageView(context);
            mTopMaskView.setImageResource(R.drawable.masked_background_half);
            mTopMaskView.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams topMaskLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, canvasMaskHeight);
            topMaskLP.gravity = Gravity.TOP;
            mTopMaskView.setLayoutParams(topMaskLP);
            addView(mTopMaskView);
        }

        if (Constant.BOTTOM_CANVAS_MASK) {
            mBottomMaskView = new ImageView(context);
            mBottomMaskView.setImageResource(R.drawable.masked_background_half);
            mBottomMaskView.setScaleType(ImageView.ScaleType.FIT_XY);
            mBottomMaskView.setRotation(180);
            FrameLayout.LayoutParams bottomMaskLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, canvasMaskHeight);
            bottomMaskLP.gravity = Gravity.BOTTOM;
            mBottomMaskView.setLayoutParams(bottomMaskLP);
            addView(mBottomMaskView);
        }

        int closeBubbleTargetY = getResources().getDimensionPixelSize(R.dimen.close_bubble_target_y);
        CloseTabTargetView closeTabTargetView = (CloseTabTargetView) inflater.inflate(R.layout.view_close_tab_target, null);
        closeTabTargetView.configure(this, context, null, Constant.BubbleAction.Close,
                0, BubbleTargetView.HorizontalAnchor.Center,
                closeBubbleTargetY, BubbleTargetView.VerticalAnchor.Bottom,
                resources.getDimensionPixelSize(R.dimen.close_bubble_target_x_offset), closeBubbleTargetY,
                resources.getDimensionPixelSize(R.dimen.close_bubble_target_tractor_offset_x), closeBubbleTargetY);
        mTargets.add(closeTabTargetView);

        int consumeTargetY = resources.getDimensionPixelSize(R.dimen.bubble_target_y);
        int consumeDefaultX = resources.getDimensionPixelSize(R.dimen.consume_bubble_target_default_x);
        int consumeXOffset = resources.getDimensionPixelSize(R.dimen.consume_bubble_target_x_offset);
        int consumeTractorBeamX = resources.getDimensionPixelSize(R.dimen.consume_bubble_target_tractor_beam_x);

        BubbleTargetView leftConsumeTarget = (BubbleTargetView) inflater.inflate(R.layout.view_consume_bubble_target, null);
        Drawable leftConsumeDrawable = Settings.get().getConsumeBubbleIcon(Constant.BubbleAction.ConsumeLeft);
        leftConsumeTarget.configure(this, context, leftConsumeDrawable, Constant.BubbleAction.ConsumeLeft,
                consumeDefaultX, BubbleTargetView.HorizontalAnchor.Left,
                consumeTargetY, BubbleTargetView.VerticalAnchor.Top,
                consumeXOffset, consumeTargetY, consumeTractorBeamX, consumeTargetY);
        mTargets.add(leftConsumeTarget);

        BubbleTargetView rightConsumeTarget = (BubbleTargetView) inflater.inflate(R.layout.view_consume_bubble_target, null);
        Drawable rightConsumeDrawable = Settings.get().getConsumeBubbleIcon(Constant.BubbleAction.ConsumeRight);
        rightConsumeTarget.configure(this, context, rightConsumeDrawable, Constant.BubbleAction.ConsumeRight,
                consumeDefaultX, BubbleTargetView.HorizontalAnchor.Right,
                consumeTargetY, BubbleTargetView.VerticalAnchor.Top,
                consumeXOffset, consumeTargetY, consumeTractorBeamX, consumeTargetY);
        mTargets.add(rightConsumeTarget);

        applyAlpha();

        /*
        int statusBarHeight = Util.getSystemStatusBarHeight(context);
        int navBarHeight = Util.getSystemNavigationBarHeight(context);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d("blerg", "height:" + height + ", statusBarHeight:" + statusBarHeight + ", navBarHeight:" + navBarHeight);

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = -statusBarHeight;
        mWindowManagerParams.height = height;//WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        */

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = 0;
        mWindowManagerParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManagerParams.setTitle("LinkBubble: CanvasView");
        MainController.addRootWindow(this, mWindowManagerParams);

        if (Constant.DEBUG_SHOW_TARGET_REGIONS) {
            mTargetDebugRect = new Rect();

            mTargetOffsetDebugPaint = new Paint();
            mTargetOffsetDebugPaint.setColor(0x80800000);

            mTargetTractorDebugPaint = new Paint();
            mTargetTractorDebugPaint.setColor(0x80008000);
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (Constant.DEBUG_SHOW_TARGET_REGIONS) {
            for (BubbleTargetView bubbleTargetView : mTargets) {
                bubbleTargetView.getTractorDebugRegion(mTargetDebugRect);
                canvas.drawRect(mTargetDebugRect, mTargetTractorDebugPaint);

                bubbleTargetView.getOffsetDebugRegion(mTargetDebugRect);
                canvas.drawRect(mTargetDebugRect, mTargetOffsetDebugPaint);
            }
        }
    }

    private void applyAlpha() {
        Util.Assert(mCurrentAlpha >= 0.0f && mCurrentAlpha <= 1.0f, "alpha out of range: " + mCurrentAlpha);

        if (mStatusBarCoverView != null) {
            mStatusBarCoverView.setAlpha(mCurrentAlpha);
        }

        if (mTopMaskView != null) {
            mTopMaskView.setAlpha(mCurrentAlpha);
        }
        if (mBottomMaskView != null) {
            mBottomMaskView.setAlpha(mCurrentAlpha);
        }

        if (!mEnabled || (mCurrentAlpha == 0.0f && mContentViewY == (int)(mTargetY))) {

            try {
                clearFocus();
            } catch(Exception e) {
                Log.d(TAG, "handled exception while clearing focus");
            }

            setVisibility(GONE);
            if (mStatusBarCoverView != null) {
                mStatusBarCoverView.setVisibility(GONE);
            }
        } else {
            setVisibility(VISIBLE);
            if (mStatusBarCoverView != null) {
                mStatusBarCoverView.setVisibility(VISIBLE);
            }
        }

        if (mContentView != null) {
            Util.Assert(mCurrentAlphaContentView >= 0.0f && mCurrentAlphaContentView <= 1.0f, "mCurrentAlphaContentView out of range:" + mCurrentAlphaContentView);
            mContentView.setAlpha(mCurrentAlphaContentView);
        }
    }

    private void setContentView(TabView bubble) {
        if (mContentView != null) {

            // The webview can throw an exception when trying to remove focus inside of removeView.
            // To prevent a crash we try to manually unfocus first, within a try/catch to reset ViewGroup::mFocused.
            // Prevents crash: https://fabric.io/brave6/android/apps/com.linkbubble.playstore/issues/55dccdeee0d514e5d640ab55
            try {
                mContentView.clearFocus();
            } catch(Exception e) {
                Log.d(TAG, "handled exception while clearing focus");
            }

            removeView(mContentView);
            mContentView.setAlpha(1.0f);
            mCurrentAlphaContentView = 1.0f;
            mTargetAlphaContentView = 1.0f;
            mContentView.onCurrentContentViewChanged(false);
        }

        ContentView contentView = bubble != null ? bubble.getContentView() : null;
        //if (bubble != null) {
        //    int bubbleIndex = MainController.get().getTabIndex(bubble);
        //    Log.d("CanvasView", "setContentView() - index:" + bubbleIndex);
        //}

        //Log.d("blerg", "setContentView(): from " + (mContentView != null ? "valid" : "none") + " to " + (contentView != null ? "valid" : "none"));

        mContentView = contentView;
        if (mContentView != null) {
            FrameLayout.LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            p.topMargin = Config.mContentOffset;
            addView(mContentView, p);
            mContentView.onCurrentContentViewChanged(true);
            mContentView.requestFocus();
            mContentView.setTranslationY(mContentViewY);
        }
    }

    private void showContentView() {
        if (mContentView != null) {
            mCurrentAlphaContentView = 1.0f;
            mTargetAlphaContentView = 1.0f;
            mContentView.setAlpha(1.0f);
        }
    }

    private void hideContentView() {
        //Util.Assert(mContentView != null);
        mCurrentAlphaContentView = mContentView != null ? mContentView.getAlpha() : 1.f;
        mTargetAlphaContentView = 0.0f;
        MainController.get().scheduleUpdate();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCurrentTabChanged(MainController.CurrentTabChangedEvent e) {
        setContentView(e.mTab);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        fadeIn();
        if (mBottomMaskView != null) {
            mBottomMaskView.setVisibility(VISIBLE);
        }
        mContentViewY = Config.mScreenHeight - Config.mContentOffset;
        hideContentView();
        MainController.get().showBadge(false);
        if (mContentView != null) {
            mContentView.onBeginBubbleDrag();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        fadeOut();
        MainController.get().showBadge(true);
        MainApplication.postEvent(getContext(), mMinimizeExpandedActivityEvent);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginCollapseTransition(MainController.BeginCollapseTransitionEvent e) {
        if (mContentView != null) {
            mContentView.onAnimateOffscreen();
            mAnimPeriod = e.mPeriod;
            mAnimTime = 0.0f;
            mInitialY = mContentViewY;
            mTargetY = Config.mScreenHeight - Config.mContentOffset;
            MainController.get().scheduleUpdate();
            fadeOut();
        } else {
            mAnimPeriod = 0.0f;
            mAnimTime = 0.0f;
        }

        MainApplication.postEvent(getContext(), mMinimizeExpandedActivityEvent);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginExpandTransition(MainController.BeginExpandTransitionEvent e) {
        fadeIn();

        if (mContentView != null) {
            showContentView();
            mContentView.onAnimateOnScreen();
            mAnimPeriod = e.mPeriod;
            mAnimTime = 0.0f;
            mInitialY = mContentViewY;
            mTargetY = 0.0f;
            MainController.get().scheduleUpdate();
        } else {
            mAnimPeriod = 0.0f;
            mAnimTime = 0.0f;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndCollapseTransition(MainController.EndCollapseTransitionEvent e) {
        fadeOut();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onOrientationChanged(MainController.OrientationChangedEvent e) {
        for (int i=0 ; i < mTargets.size() ; ++i) {
            BubbleTargetView bt = mTargets.get(i);
            bt.OnOrientationChanged();
        }
        if (mContentView != null) {
            mContentView.onOrientationChanged();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginAnimateFinalTabAway(MainController.BeginAnimateFinalTabAwayEvent event) {
        fadeOut();
        hideContentView();
        setContentView(event.mTab);
        MainController.BeginCollapseTransitionEvent collapseTransitionEvent = new MainController.BeginCollapseTransitionEvent();
        collapseTransitionEvent.mPeriod = (Constant.BUBBLE_ANIM_TIME / 1000.f) * 0.666667f;
        onBeginCollapseTransition(collapseTransitionEvent);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHideContentEvent(MainController.HideContentEvent event) {
        setContentView(null);
        mContentViewY = Config.mScreenHeight - Config.mContentOffset;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onConsumeBubblesChanged(Settings.OnConsumeBubblesChangedEvent event) {
        for (int i = 0; i < mTargets.size(); ++i) {
            mTargets.get(i).onConsumeBubblesChanged();
        }
    }

    private void fadeIn() {
        mTargetAlpha = mMaxAlpha;
        MainController.get().scheduleUpdate();
    }

    private void fadeOut() {
        mTargetAlpha = 0.0f;
        MainController.get().scheduleUpdate();
    }

    public void destroy() {
        for (BubbleTargetView bt : mTargets) {
            bt.destroy();
        }

        MainApplication.unregisterForBus(getContext(), this);

        MainController.removeRootWindow(this);
        if (mStatusBarCoverView != null) {
            MainController.removeRootWindow(mStatusBarCoverView);
        }

        // Note: sometimes this element leaks. Seems to be result of this: http://goo.gl/Ite5F9
    }

    public void update(float dt) {

        boolean updateAnimTime;
        if (Constant.DYNAMIC_ANIM_STEP) {
            updateAnimTime = mAnimPeriod > 0.0f;
        } else {
            updateAnimTime = mAnimPeriod > 0.0f && mAnimTime <= mAnimPeriod;
        }

        if (updateAnimTime) {
            float t = Util.clamp(0.0f, mAnimTime / mAnimPeriod, 1.0f);
            mContentViewY = (int) (mInitialY + (mTargetY - mInitialY) * t);
            if (mContentView != null) {
                mContentView.setTranslationY(mContentViewY);
            }
            if (mAnimTime < mAnimPeriod) {
                MainController.get().scheduleUpdate();
            } else if (mTargetY == 0.0f) {
                if (mBottomMaskView != null) {
                    mBottomMaskView.setVisibility(GONE);
                }
            }
            mAnimTime += dt;
        }

        if (mCurrentAlpha < mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha + mAlphaDelta * dt, mMaxAlpha);
            MainController.get().scheduleUpdate();
        } else if (mCurrentAlpha > mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha - mAlphaDelta * dt, mMaxAlpha);
            MainController.get().scheduleUpdate();
        }

        if (mCurrentAlphaContentView < mTargetAlphaContentView) {
            mCurrentAlphaContentView = Util.clamp(0.0f, mCurrentAlphaContentView + mAlphaDelta * dt, 1.0f);
            MainController.get().scheduleUpdate();
        } else if (mCurrentAlphaContentView > mTargetAlphaContentView) {
            mCurrentAlphaContentView = Util.clamp(0.0f, mCurrentAlphaContentView - mAlphaDelta * dt, 1.0f);
            MainController.get().scheduleUpdate();
        }

        applyAlpha();

        for (int i=0 ; i < mTargets.size() ; ++i) {
            mTargets.get(i).update(dt);
        }
    }

    public BubbleTargetView getSnapTarget(float x0, float y0, float x1, float y1, Util.Point p) {

        BubbleTargetView closestTargetView = null;
        float closestDistance = 9e9f;

        for (BubbleTargetView tv : mTargets) {

            tv.getTractorDebugRegion(mTractorRegion);
            Circle targetCircle = tv.GetDefaultCircle();

            if (Util.clipLineSegmentToRectangle(x0, y0, x1, y1, mTractorRegion.left, mTractorRegion.top, mTractorRegion.right, mTractorRegion.bottom, mClipResult)) {
                Util.closestPointToLineSegment(mClipResult.x0, mClipResult.y0, mClipResult.x1, mClipResult.y1, targetCircle.mX, targetCircle.mY, mClosestPoint);

                float d = Util.distance(x0, y0, mClosestPoint.x, mClosestPoint.y);
                if (d < closestDistance) {
                    p.x = mClosestPoint.x;
                    p.y = mClosestPoint.y;
                    closestTargetView = tv;
                }
            }
        }

        return closestTargetView;
    }

    public BubbleTargetView getSnapTarget(Circle bubbleCircle, float radiusScaler) {

        for (int i=0 ; i < mTargets.size() ; ++i) {
            BubbleTargetView bt = mTargets.get(i);

            if (bt.shouldSnap(bubbleCircle, radiusScaler)) {
                return bt;
            }
        }

        return null;
    }
}