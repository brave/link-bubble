/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.animation.Animator;
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
import com.linkbubble.webrender.WebRenderer;
import com.squareup.otto.Subscribe;

import java.util.Vector;

public class CanvasView extends FrameLayout {

    private static final String TAG = "CanvasView";

    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private Vector<BubbleTargetView> mTargets = new Vector<BubbleTargetView>();
    private ImageView mTopMaskView;
    private ImageView mBottomMaskView;

    boolean mExpanded;
    boolean mDragging;
    int mTargetAlpha;
    int mContentViewTargetAlpha;

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

        setVisibility(GONE);

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = 0;
        mWindowManagerParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManagerParams.setTitle("LinkBubble: CanvasView");
        //to do debug
        MainController.addRootWindow(this, mWindowManagerParams);
        //

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

    private void applyAlpha(final int targetAlpha) {
        mTargetAlpha = targetAlpha;
        setVisibility(VISIBLE);
        if (mStatusBarCoverView != null) {
            mStatusBarCoverView.setVisibility(VISIBLE);
            mStatusBarCoverView.animate().alpha(targetAlpha).setDuration(Constant.CANVAS_FADE_ANIM_TIME).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mStatusBarCoverView != null) {
                        if (targetAlpha == 0) {
                            mStatusBarCoverView.setVisibility(GONE);
                            mStatusBarCoverView.setAlpha(0);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }

        if (mTopMaskView != null) {
            mTopMaskView.setVisibility(VISIBLE);
            mTopMaskView.animate().alpha(targetAlpha).setDuration(Constant.CANVAS_FADE_ANIM_TIME).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mTopMaskView != null) {
                        if (targetAlpha == 0) {
                            mTopMaskView.setVisibility(GONE);
                            mTopMaskView.setAlpha(0);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
        if (mBottomMaskView != null) {
            //mBottomMaskView.setVisibility(VISIBLE);
            ///mBottomMaskView.setAlpha(mCurrentAlpha);
            mBottomMaskView.animate().alpha(targetAlpha).setDuration(Constant.CANVAS_FADE_ANIM_TIME).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mBottomMaskView != null) {
                        if (targetAlpha == 0) {
                            mBottomMaskView.setVisibility(GONE);
                            mBottomMaskView.setAlpha(0);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
        applyContentViewAlpha(mTargetAlpha);
    }

    private void applyContentViewAlpha(final int targetAlpha) {
        mContentViewTargetAlpha = targetAlpha;
        if (mContentView != null) {
            mContentView.animate().cancel();
            mContentView.setVisibility(VISIBLE);
            setVisibility(VISIBLE);
            if (mContentViewTargetAlpha != 0 && !mExpanded) {
                mContentView.setAlpha(0f);
            }

            mContentView.animate().alpha(targetAlpha).setDuration(Constant.CANVAS_FADE_ANIM_TIME).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mContentView != null) {
                        if (mContentViewTargetAlpha == 0) {
                            mContentView.setAlpha(0f);
                            mContentView.setVisibility(GONE);
                            if (!mExpanded) {
                                removeView(mContentView);
                            }
                        } else {
                            mContentView.setAlpha(1f);
                            mContentView.setVisibility(VISIBLE);
                        }
                    }
                    // If we also have target alpha 0 then hide the view so clicks behind will work.
                    if (mTargetAlpha == 0 && !mDragging) {
                        setVisibility(GONE);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

            if (targetAlpha == 0) {
                try {
                    clearFocus();
                } catch (Exception e) {
                    Log.d(TAG, "handled exception while clearing focus");
                }
            }
        }
    }

    private void setContentView(TabView bubble, boolean unhideNotification) {
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
            if (mExpanded) {
                applyContentViewAlpha(1);
            }
            mContentView.onCurrentContentViewChanged(false);
        }

        ContentView contentView = bubble != null ? bubble.getContentView() : null;
        //if (bubble != null) {
        //    int bubbleIndex = MainController.get().getTabIndex(bubble);
        //    Log.d("CanvasView", "setContentView() - index:" + bubbleIndex);
        //}

        //Log.d("blerg", "setContentView(): from " + (mContentView != null ? "valid" : "none") + " to " + (contentView != null ? "valid" : "none"));

        mContentView = contentView;
        if (unhideNotification) {
            return;
        }
        if (mContentView != null) {
            FrameLayout.LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            p.topMargin = Config.mContentOffset;
            addView(mContentView, p);
            mContentView.onCurrentContentViewChanged(true);
            mContentView.requestFocus();
        }
    }

    private void showContentView() {
        applyContentViewAlpha(1);
    }

    private void hideContentView() {
        applyContentViewAlpha(0);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCurrentTabResume(MainController.CurrentTabResumeEvent e) {
        if (null == e.mTab) {
            return;
        }
        ContentView contentView = e.mTab.getContentView();
        if (null == contentView) {
            return;
        }
        WebRenderer webRenderer = contentView.getWebRenderer();
        if (null == webRenderer) {
            return;
        }
        webRenderer.resumeOnSetActive();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCurrentTabPause(MainController.CurrentTabPauseEvent e) {
        if (null == e.mTab) {
            return;
        }
        ContentView contentView = e.mTab.getContentView();
        if (null == contentView) {
            return;
        }
        WebRenderer webRenderer = contentView.getWebRenderer();
        if (null == webRenderer) {
            return;
        }
        webRenderer.pauseOnSetInactive();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onCurrentTabChanged(MainController.CurrentTabChangedEvent e) {
        setContentView(e.mTab, e.mUnhideNotification);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        mDragging = true;
        if (mExpanded) {
            fadeOut();
        } else {
            setVisibility(VISIBLE);
            fadeIn();
            if (mBottomMaskView != null) {
                mBottomMaskView.setVisibility(VISIBLE);
            }
            hideContentView();
            MainController.get().showBadge(false);
            if (mContentView != null) {
                mContentView.onBeginBubbleDrag();
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        mDragging = false;
        mExpanded = false;
        fadeOut();
        removeView(mContentView);
        setVisibility(GONE);
        MainController.get().showBadge(true);
        MainApplication.postEvent(getContext(), mMinimizeExpandedActivityEvent);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginCollapseTransition(MainController.BeginCollapseTransitionEvent e) {
        mExpanded = false;
        if (mContentView != null) {
            mContentView.onAnimateOffscreen();
            fadeOut();
        }

        MainApplication.postEvent(getContext(), mMinimizeExpandedActivityEvent);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginExpandTransition(MainController.BeginExpandTransitionEvent e) {
        mExpanded = true;
        fadeIn();

        if (mContentView != null) {
            if (null == mContentView.getParent()) {
                addView(mContentView);
            }
            mContentView.onAnimateOnScreen();
            showContentView();
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
        setContentView(event.mTab, false);
        MainController.BeginCollapseTransitionEvent collapseTransitionEvent = new MainController.BeginCollapseTransitionEvent();
        collapseTransitionEvent.mPeriod = (Constant.BUBBLE_ANIM_TIME / 1000.f) * 0.666667f;
        onBeginCollapseTransition(collapseTransitionEvent);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHideContentEvent(MainController.HideContentEvent event) {
        setContentView(null, false);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onConsumeBubblesChanged(Settings.OnConsumeBubblesChangedEvent event) {
        for (int i = 0; i < mTargets.size(); ++i) {
            mTargets.get(i).onConsumeBubblesChanged();
        }
    }

    private void fadeIn() {
        applyAlpha(1);
    }

    private void fadeOut() {
        applyAlpha(0);
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