package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
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
import com.linkbubble.util.Util;
import com.linkbubble.physics.Circle;
import com.squareup.otto.Subscribe;

import java.util.Vector;

public class CanvasView extends FrameLayout {

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private Vector<BubbleTargetView> mTargets = new Vector<BubbleTargetView>();
    private ImageView mTopMaskView;
    private ImageView mBottomMaskView;

    private final float mMaxAlpha = 1.0f;
    private final float mFadeTime = 0.3f;
    private final float mAlphaDelta = mMaxAlpha / mFadeTime;

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

    public CanvasView(Context context) {
        super(context);

        MainApplication.registerForBus(context, this);

        mEnabled = true;
        mContentViewY = Config.mScreenHeight - Config.mContentOffset;

        applyAlpha();

        mTargets.add(new BubbleTargetView(this, context, R.drawable.close_indicator, Config.BubbleAction.Destroy,
                0, BubbleTargetView.HorizontalAnchor.Center,
                35, BubbleTargetView.VerticalAnchor.Bottom,
                30, 35, 200, 35));
        mTargets.add(new BubbleTargetView(this, context, Config.BubbleAction.ConsumeLeft,
                70, BubbleTargetView.HorizontalAnchor.Left,
                33, BubbleTargetView.VerticalAnchor.Top,
                15, 33, 150, 33));
        mTargets.add(new BubbleTargetView(this, context, Config.BubbleAction.ConsumeRight,
                70, BubbleTargetView.HorizontalAnchor.Right,
                33, BubbleTargetView.VerticalAnchor.Top,
                15, 33, 150, 33));

        Settings.setConsumeBubblesChangedEventHandler(new Settings.ConsumeBubblesChangedEventHandler() {
            @Override
            public void onConsumeBubblesChanged() {
                for (int i = 0; i < mTargets.size(); ++i) {
                    mTargets.get(i).onConsumeBubblesChanged();
                }
            }
        });

        int canvasMaskHeight = getResources().getDimensionPixelSize(R.dimen.canvas_mask_height);

        mTopMaskView = new ImageView(context);
        mTopMaskView.setImageResource(R.drawable.masked_background_half);
        mTopMaskView.setScaleType(ImageView.ScaleType.FIT_XY);
        FrameLayout.LayoutParams topMaskLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, canvasMaskHeight);
        topMaskLP.gravity = Gravity.TOP;
        mTopMaskView.setLayoutParams(topMaskLP);
        addView(mTopMaskView);

        mBottomMaskView = new ImageView(context);
        mBottomMaskView.setImageResource(R.drawable.masked_background_half);
        mBottomMaskView.setScaleType(ImageView.ScaleType.FIT_XY);
        mBottomMaskView.setRotation(180);
        FrameLayout.LayoutParams bottomMaskLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, canvasMaskHeight);
        bottomMaskLP.gravity = Gravity.BOTTOM;
        mBottomMaskView.setLayoutParams(bottomMaskLP);
        addView(mBottomMaskView);

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = 0;
        mWindowManagerParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManagerParams.setTitle("LinkBubble: CanvasView");
        mWindowManager.addView(this, mWindowManagerParams);
    }

    private void applyAlpha() {
        Util.Assert(mCurrentAlpha >= 0.0f && mCurrentAlpha <= 1.0f);

        int color = ((int)(180 * mCurrentAlpha) << 24);
        if (Config.USE_CONTENT_ACTIVITY) {
            MainController.get().updateBackgroundColor(color);
        } else {
            setAlpha(mCurrentAlpha);
            //setBackgroundColor(color);
        }

        if (!mEnabled || mCurrentAlpha == 0.0f) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }

        if (mContentView != null) {
            Util.Assert(mCurrentAlphaContentView >= 0.0f && mCurrentAlphaContentView <= 1.0f);
            mContentView.setAlpha(mCurrentAlphaContentView);
        }
    }

    private void setContentView(TabView bubble) {
        if (mContentView != null) {
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
    public void onCurrentBubbleChanged(MainController.CurrentTabChangedEvent e) {
        setContentView(e.mTab);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        fadeIn();
        mBottomMaskView.setVisibility(VISIBLE);
        mContentViewY = Config.mScreenHeight - Config.mContentOffset;
        hideContentView();
        MainController.get().showBadge(false);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        fadeOut();
        MainController.get().showBadge(true);
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
        } else {
            mAnimPeriod = 0.0f;
            mAnimTime = 0.0f;
        }
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

        mWindowManager.removeView(this);
    }

    public void update(float dt) {

        if (mAnimPeriod > 0.0f && mAnimTime <= mAnimPeriod) {
            float t = Util.clamp(0.0f, mAnimTime / mAnimPeriod, 1.0f);
            mContentViewY = (int) (mInitialY + (mTargetY - mInitialY) * t);
            mContentView.setTranslationY(mContentViewY);
            if (mAnimTime < mAnimPeriod) {
                MainController.get().scheduleUpdate();
            } else if (mTargetY == 0.0f) {
                mBottomMaskView.setVisibility(GONE);
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