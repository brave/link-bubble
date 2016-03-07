/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.TranslateAnimationEx;
import com.linkbubble.util.Util;
import com.linkbubble.util.VerticalGestureListener;

import java.util.ArrayList;
import java.util.List;


public class BubbleFlowView extends HorizontalScrollView {

    private static final String TAG = "BubbleFlowView";
    private static final boolean DEBUG = true;
    private static final int INVALID_POINTER = -1;
    private static final float MIN_SCALE = .7f;

    public interface Listener {
        void onCenterItemClicked(BubbleFlowView sender, View view);
        void onCenterItemLongClicked(BubbleFlowView sender, View view);
        void onCenterItemSwiped(VerticalGestureListener.GestureDirection gestureDirection);
        // Note: only called when scrolling has finished
        void onCenterItemChanged(BubbleFlowView sender, View view);
    }

    public interface AnimationEventListener {
        void onAnimationEnd(BubbleFlowView sender);
    }

    public interface TouchInterceptor {
        boolean onTouchActionDown(MotionEvent event);
        boolean onTouchActionMove(MotionEvent event);
        boolean onTouchActionUp(MotionEvent event);
    }

    private boolean mDoingCollapse;
    protected List<View> mViews;
    protected FrameLayout mContent;
    private boolean mIsExpanded;
    private int mWidth;
    protected int mItemWidth;
    protected int mItemHeight;
    private float mFullScaleX;
    private float mMinScaleX;
    private int mEdgeMargin;
    private int mIndexOnActionDown;
    private boolean mFlingCalled;
    protected boolean mSlideOffAnimationPlaying;

    private Listener mBubbleFlowListener;
    private TouchInterceptor mTouchInterceptor;
    private int mActiveTouchPointerId = INVALID_POINTER;
    private boolean mInterceptingTouch = false;
    private int mLastMotionY;

    private GestureDetector mVerticalGestureDetector;
    private VerticalGestureListener mVerticalGestureListener = new VerticalGestureListener();
    private long mLastVerticalGestureTime;

    private int mStillTouchFrameCount;
    private int mCenterViewTouchPointerId = INVALID_POINTER;
    private float mCenterViewDownX;
    private float mCenterViewDownY;
    private static final int LONG_PRESS_FRAMES = 6;
    private View mTouchView;
    private boolean mLongPress;

    public BubbleFlowView(Context context) {
        this(context, null);
    }

    public BubbleFlowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFlowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mViews = new ArrayList<View>();

        mContent = new FrameLayout(context);
        mContent.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT));

        addView(mContent);

        mIsExpanded = true;

        setOnTouchListener(mOnTouchListener);

        mVerticalGestureDetector = new GestureDetector(mVerticalGestureListener);
    }

    public void setBubbleFlowViewListener(Listener listener) {
        mBubbleFlowListener = listener;
    }

    public boolean update() {
        boolean result = false;

        if (mSlideOffAnimationPlaying) {
            result = true;
        }

        if (mTouchView != null) {
            if (mStillTouchFrameCount > -1) {
                ++mStillTouchFrameCount;
                if (DEBUG) {
                    Log.d(TAG, "[longpress] update(): mStillTouchFrameCount:" + mStillTouchFrameCount);
                }

                if (mStillTouchFrameCount == LONG_PRESS_FRAMES) {
                    if (mBubbleFlowListener != null) {
                        mLongPress = true;
                        mBubbleFlowListener.onCenterItemLongClicked(BubbleFlowView.this, mTouchView);
                    }
                }

                // Check mContent rather than mViews, because it's possible for mViews to be empty yet mContent have a child
                // (e.g, in the instance the final Bubble is animating off screen).
                if (mContent.getChildCount() > 0) {
                    result = true;
                }
            }
            return result;
        }

        return false;
    }

    public void setTouchInterceptor(TouchInterceptor touchInterceptor) {
        mTouchInterceptor = touchInterceptor;
        if (mTouchInterceptor == null) {
            mInterceptingTouch = false;
        }
    }

    void configure(int width, int itemWidth, int itemHeight) {
        mWidth = width;
        mItemWidth = itemWidth;
        mItemHeight = itemHeight;
        mEdgeMargin = (width - itemWidth) / 2;

        mFullScaleX = mItemWidth * .3f;
        mMinScaleX = mItemWidth * 1.2f;
    }

    public void add(View view, boolean insertNextToCenterItem) {

        //view.setBackgroundColor(mViews.size() % 2 == 0 ? 0xff660066 : 0xff666600);

        view.setOnClickListener(mViewOnClickListener);
        view.setOnTouchListener(mViewOnTouchListener);

        int centerIndex = getCenterIndex();
        int insertAtIndex = insertNextToCenterItem ? centerIndex + 1 : mViews.size();

        if (view.getParent() != null) {
            ((ViewGroup)view.getParent()).removeView(view);
        }

        /*FrameLayout.LayoutParams lp = new LayoutParams(mItemWidth, mItemHeight, Gravity.TOP|Gravity.LEFT);
        lp.leftMargin = mEdgeMargin + insertAtIndex * mItemWidth;
        mContent.addView(view, lp);
        mContent.invalidate();*/

        /*if (insertNextToCenterItem) {
            mViews.add(centerIndex+1, view);
        } else {
            mViews.add(view);
        }*/

        //updatePositions();
        /*updateScales(getScrollX());

        if (insertNextToCenterItem) {
            TranslateAnimation slideOnAnim = new TranslateAnimation(0, 0, -mItemHeight, 0);
            slideOnAnim.setDuration(Constant.BUBBLE_FLOW_ANIM_TIME);
            slideOnAnim.setFillAfter(true);
            view.startAnimation(slideOnAnim);

            for (int i = centerIndex + 2; i < mViews.size(); i++) {
                View viewToShift = mViews.get(i);
                TranslateAnimation slideRightAnim = new TranslateAnimation(-mItemWidth, 0, 0, 0);
                slideRightAnim.setDuration(Constant.BUBBLE_FLOW_ANIM_TIME);
                slideRightAnim.setFillAfter(true);
                viewToShift.startAnimation(slideRightAnim);
            }
        }*/

        //ViewGroup.LayoutParams contentLP = mContent.getLayoutParams();
        //contentLP.width = (mViews.size() * mItemWidth) + mItemWidth + (2 * mEdgeMargin);
        //mContent.setLayoutParams(contentLP);
    }

    // Called when the item has actually been removed. Will be instantly when no animation occurs,
    // or if animating, at the completion of the animation.
    protected interface OnRemovedListener {
        void onRemoved(View view);
    }

    void remove(final int index, boolean animateOff, boolean removeFromList) {
        remove(index, animateOff, removeFromList, null);
    }

    protected void remove(final int index, boolean animateOff, boolean removeFromList, final OnRemovedListener onRemovedListener) {
        if (index < 0 || index >= mViews.size()) {
            return;
        }
        final View view = mViews.get(index);

        if (animateOff) {
            if (removeFromList == false) {
                throw new RuntimeException("removeFromList must be true if animating off");
            }
            TranslateAnimation slideOffAnim = new TranslateAnimation(0, 0, 0, -mItemHeight);
            slideOffAnim.setDuration(Constant.BUBBLE_FLOW_ANIM_TIME);
            slideOffAnim.setFillAfter(true);
            slideOffAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mContent.removeView(view);

                    // Cancel the current animation on the views so the offset no longer applies
                    for (int i = 0; i < mViews.size(); i++) {
                        View view = mViews.get(i);
                        Animation viewAnimation = view.getAnimation();
                        if (viewAnimation != null) {
                            viewAnimation.cancel();
                            view.setAnimation(null);
                        }
                    }
                    updatePositions();
                    updateScales(getScrollX());
                    mSlideOffAnimationPlaying = false;

                    if (onRemovedListener != null) {
                        onRemovedListener.onRemoved(view);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            invalidate();       // This fixes #284 - it's a hack, but it will do for now.
            view.startAnimation(slideOffAnim);
            mSlideOffAnimationPlaying = true;

            mViews.remove(view);

            int viewsSize = mViews.size();
            if (index < viewsSize) {
                for (int i = index; i < viewsSize; i++) {
                    final View viewToShift = mViews.get(i);
                    TranslateAnimationEx slideAnim = new TranslateAnimationEx(0, -mItemWidth, 0, 0, new TranslateAnimationEx.TransformationListener() {
                        @Override
                        public void onApplyTransform(float interpolatedTime, Transformation t, float dx, float dy) {
                            float centerX = getScrollX() + (mWidth/2) - (mItemWidth/2);
                            updateScaleForView(viewToShift, centerX, viewToShift.getX() + dx);
                        }
                    });
                    slideAnim.setDuration(Constant.BUBBLE_FLOW_ANIM_TIME);
                    slideAnim.setFillAfter(true);
                    viewToShift.startAnimation(slideAnim);
                }
            } else if (viewsSize > 0) {
                for (int i = 0; i < index; i++) {
                    final View viewToShift = mViews.get(i);
                    TranslateAnimationEx slideAnim = new TranslateAnimationEx(0, mItemWidth, 0, 0, new TranslateAnimationEx.TransformationListener() {
                        @Override
                        public void onApplyTransform(float interpolatedTime, Transformation t, float dx, float dy) {
                            float centerX = getScrollX() + (mWidth/2) - (mItemWidth/2);
                            updateScaleForView(viewToShift, centerX, viewToShift.getX() + dx);
                        }
                    });
                    slideAnim.setDuration(Constant.BUBBLE_FLOW_ANIM_TIME);
                    slideAnim.setFillAfter(true);
                    viewToShift.startAnimation(slideAnim);
                }
            }
        } else {
            mContent.removeView(view);
            if (removeFromList) {
                mViews.remove(view);
                updatePositions();
                updateScales(getScrollX());
                mContent.invalidate();
            }
            if (onRemovedListener != null) {
                onRemovedListener.onRemoved(view);
            }
        }
    }

    void updatePositions() {
        int size = mViews.size();
        for (int i = 0; i < size; i++) {
            View view = mViews.get(i);
            FrameLayout.LayoutParams lp = (LayoutParams) view.getLayoutParams();
            lp.leftMargin = mEdgeMargin + (i * mItemWidth);

            if (size-1 == i) {
                lp.rightMargin = mEdgeMargin;
            } else {
                lp.rightMargin = 0;
            }
        }
    }

    void updateScales(int scrollX) {
        float centerX = scrollX + (mWidth/2) - (mItemWidth/2);

        int size = mViews.size();
        for (int i = 0; i < size; i++) {
            updateScaleForView(mViews.get(i), centerX, ((i * mItemWidth) + mEdgeMargin));
        }
    }

    void updateScaleForView(View view, float centerX, float viewX) {
        float xDelta = Math.abs(centerX - viewX);
        float targetScale;
        if (xDelta < mFullScaleX) {
            targetScale = 1.f;
        } else if (xDelta > mMinScaleX) {
            targetScale = MIN_SCALE;
        } else {
            float ratio = 1.f - ((xDelta - mFullScaleX) / (mMinScaleX - mFullScaleX));
            targetScale = MIN_SCALE + (ratio * (1.f- MIN_SCALE));
        }
        float scaleDelta = Math.abs(getScaleX() - targetScale);
        view.setScaleX(targetScale);
        view.setScaleY(targetScale);
        if (scaleDelta > .001f) {
            view.invalidate();
        }
    }

    public int getItemCount() {
        return mViews.size();
    }

    public int getIndexOfView(View view) {
        return mViews.indexOf(view);
    }

    int getCenterIndex() {
        int centerX = (mWidth/2) + getScrollX();
        int closestXAbsDelta = Integer.MAX_VALUE;
        int closestIndex = -1;
        for (int i = 0; i < mViews.size(); i++) {
            int x = mEdgeMargin + (i * mItemWidth) + (mItemWidth/2);
            int absDelta = Math.abs(x-centerX);
            if (absDelta < closestXAbsDelta) {
                closestXAbsDelta = absDelta;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    public void setCenterIndex(int index) {
        setCenterIndex(index, true);
    }

    public void setCenterIndex(int index, boolean animate) {
        int scrollToX = mEdgeMargin + (index * mItemWidth) - (mWidth/2) + (mItemWidth/2);
        startScrollFinishedCheckTask(scrollToX);
        if (animate) {
            smoothScrollTo(scrollToX, 0);
        } else {
            scrollTo(scrollToX, 0);
        }
    }

    public void setCenterItem(View view) {
        int index = mViews.indexOf(view);
        if (index > -1) {
            setCenterIndex(index);
        }
    }

    private static final int DEFAULT_ANIM_TIME = 300;

    public boolean expand() {
        // Note: if this function changes to not pass default arguments along, be sure to update BubbleFlowDraggable's expand() override(s) accordingly.
        return expand(DEFAULT_ANIM_TIME, null);
    }

    public boolean expand(long time, final AnimationEventListener animationEventListener) {
        CrashTracking.log("BubbleFlowView.expand(" + time + "), mIsExpanded:" + mIsExpanded);
        if (mIsExpanded) {
            return false;
        }

        mDoingCollapse = false;

        mStillTouchFrameCount = -1;
        if (DEBUG) {
            //Log.d(TAG, "[longpress] expand(): mStillTouchFrameCount=" + mStillTouchFrameCount);
        }

        int size = mViews.size();
        int centerIndex = getCenterIndex();
        if (centerIndex == -1) {    // fixes #343
            return false;
        }
        View centerView = mViews.get(centerIndex);
        boolean addedAnimationListener = false;
        for (int i = 0; i < size; i++) {
            View view = mViews.get(i);
            if (centerView != view) {
                int xOffset = (int) (centerView.getX() - ((i * mItemWidth) + mEdgeMargin));
                TranslateAnimation anim = new TranslateAnimation(xOffset, 0, 0, 0);
                anim.setDuration(time);
                anim.setFillAfter(true);
                if (addedAnimationListener == false) {
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (animationEventListener != null) {
                                animationEventListener.onAnimationEnd(BubbleFlowView.this);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    addedAnimationListener = true;
                }
                view.startAnimation(anim);
            }
        }

        if (centerIndex == 0 && mViews.size() == 1) {
            if (animationEventListener != null) {
                animationEventListener.onAnimationEnd(this);
            }
        }

        bringTabViewToFront(centerView);
        mIsExpanded = true;
        return true;
    }

    private void bringTabViewToFront(View tabView) {
        tabView.clearAnimation();
        tabView.bringToFront();
        mContent.requestLayout();
        mContent.invalidate();
    }

    public void collapse() {
        collapse(DEFAULT_ANIM_TIME, null);
    }

    public void collapse(long time, AnimationEventListener animationEventListener) {
        CrashTracking.log("BubbleFlowView.collapse(): time:" + time + ", mIsExpanded:" + mIsExpanded);
        if (mIsExpanded == false) {
            return;
        }

        mDoingCollapse = true;
        mIsExpanded = false;
        mStillTouchFrameCount = -1;
        if (DEBUG) {
            //Log.d(TAG, "[longpress] collapse(): mStillTouchFrameCount=" + mStillTouchFrameCount);
        }

        int size = mViews.size();
        int centerIndex = getCenterIndex();
        if (centerIndex == -1) {
            return;
        }
        View centerView = mViews.get(centerIndex);

        // There was previously a collapse animation to match the expand animation, but for
        // perf reasons it was removed so that it wouldn't need to track the currently dragging bubble.
        if (animationEventListener != null) {
          animationEventListener.onAnimationEnd(this);
        }

        bringTabViewToFront(centerView);
    }

    private AnimationEventListener mCollapseEndAnimationEventListener;
    public boolean forceCollapseEnd() {
        boolean result = false;
        if (mCollapseEndAnimationEventListener != null && mDoingCollapse) {
            mCollapseEndAnimationEventListener.onAnimationEnd(BubbleFlowView.this);
            result = true;
        }
        mCollapseEndAnimationEventListener = null;
        mDoingCollapse = false;

        return result;
    }

    boolean isExpanded() {
        return mIsExpanded;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);

        mStillTouchFrameCount = -1;
        if (DEBUG) {
            //Log.d(TAG, "[longpress] onScrollChanged(): mStillTouchFrameCount=" + mStillTouchFrameCount);
        }

        updateScales(x);
    }

    private static final int SCROLL_FINISHED_CHECK_TIME = 33;
    private int mScrollFinishedCheckerInitialXPosition = -1;
    private Runnable mScrollFinishedChecker = new Runnable() {

        public void run() {
            int scrollX = getScrollX();
            if(mScrollFinishedCheckerInitialXPosition - scrollX == 0){
                mScrollFinishedCheckerInitialXPosition = -1;
                if (mBubbleFlowListener != null) {
                    int currentCenterIndex = getCenterIndex();
                    if (currentCenterIndex > -1) {
                        mBubbleFlowListener.onCenterItemChanged(BubbleFlowView.this, mViews.get(currentCenterIndex));
                    }
                }
            }else{
                mScrollFinishedCheckerInitialXPosition = scrollX;
                postDelayed(mScrollFinishedChecker, SCROLL_FINISHED_CHECK_TIME);
            }
        }
    };

    public void startScrollFinishedCheckTask(int targetXPosition){
        mScrollFinishedCheckerInitialXPosition = targetXPosition;
        postDelayed(mScrollFinishedChecker, SCROLL_FINISHED_CHECK_TIME);
    }

    public boolean isAnimatingToCenterIndex() {
        return mScrollFinishedCheckerInitialXPosition > -1 ? true : false;
    }

    /*
     * Override the fling functionality by manually setting the target index to animate towards.
     * This allows us to ensure a view is always centered in the middle of the BubbleFlowView
     */
    @Override
    public void fling(int velocityX) {
        mFlingCalled = true;
        String debugMessage = "fling() - velocityX:" + velocityX;

        int currentIndex = getCenterIndex();
        int targetIndex;
        int absVelocityX = Math.abs(velocityX);
        if (absVelocityX > 8000) {
            //super.fling(velocityX);
            if (velocityX < 0) {
                targetIndex = 0;
            } else {
                targetIndex = mViews.size();
            }
        } else {
            if (absVelocityX > 6000) {
                if (velocityX < 0) {
                    targetIndex = currentIndex - 6;
                } else {
                    targetIndex = currentIndex + 6;
                }
            } else if (absVelocityX > 4500) {
                if (velocityX < 0) {
                    targetIndex = currentIndex - 2;
                } else {
                    targetIndex = currentIndex + 2;
                }
            } else if (absVelocityX > 2000) {
                if (velocityX < 0) {
                    targetIndex = currentIndex - 1;
                } else {
                    targetIndex = currentIndex + 1;
                }
            } else {
                if (velocityX < 0 && (currentIndex == mIndexOnActionDown)) {
                    targetIndex = mIndexOnActionDown - 1;
                    debugMessage += ", [babyFling] mIndexOnActionDown: " + mIndexOnActionDown + ", target: " + targetIndex;
                } else if (velocityX > 0 && (currentIndex == mIndexOnActionDown)) {
                    targetIndex = mIndexOnActionDown + 1;
                    debugMessage += ", [babyFling] mIndexOnActionDown: " + mIndexOnActionDown + ", target: " + targetIndex;
                } else {
                    debugMessage += ", [babyFling] mIndexOnActionDown: " + mIndexOnActionDown + ", currentIndex: " + currentIndex;
                    targetIndex = currentIndex;
                }
            }
        }

        if (targetIndex < 0) {
            targetIndex = 0;
        } else if (targetIndex >= mViews.size()) {
            targetIndex = mViews.size() - 1;
        }
        debugMessage += ", delta:" + (targetIndex-currentIndex);
        setCenterIndex(targetIndex);

        if (DEBUG) {
            Log.d(TAG, debugMessage);
        }
    }

    /*
     * BubbleFlowView extends HorizontalScrollView, which does NOT intercept touch events when the delta is on the Y axis only.
     * We need to detect y input delta when passing input via the TouchInterceptor, thus override this function to ensure
     * true is returned in this case (but only if mTouchInterceptor != null).
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        final int action = event.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && mInterceptingTouch && mTouchInterceptor != null) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                if (mActiveTouchPointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = event.findPointerIndex(mActiveTouchPointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActiveTouchPointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int y = (int) event.getY(pointerIndex);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > 0) {
                    mLastMotionY = y;
                    // Here is the crux of it all...
                    if (mTouchInterceptor != null) {
                        mInterceptingTouch = true;
                    }
                }

            case MotionEvent.ACTION_DOWN:
                // ACTION_DOWN always refers to pointer index 0.
                mLastMotionY = (int) event.getY();
                mActiveTouchPointerId = event.getPointerId(0);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mInterceptingTouch && mLongPress) {
                    final float bubblePeriod = (float) Constant.BUBBLE_FLOW_ANIM_TIME / 1000.f;
                    final float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension
                    MainController.get().expandBubbleFlow((long) (contentPeriod * 1000), false);
                }
                mActiveTouchPointerId = INVALID_POINTER;
                mInterceptingTouch = false;
                break;
        }

        if (super.onInterceptTouchEvent(event)) {
            return true;
        }

        return mInterceptingTouch;
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            final int action = ev.getAction();

            int maskedAction = action & MotionEvent.ACTION_MASK;
            switch (maskedAction) {
                case MotionEvent.ACTION_DOWN:
                    if (mTouchInterceptor != null && mTouchInterceptor.onTouchActionDown(ev)) {
                        return true;
                    }

                    mActiveTouchPointerId = ev.getPointerId(0);
                    mLastMotionY = (int) ev.getX();
                    mIndexOnActionDown = getCenterIndex();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mTouchInterceptor != null && mTouchInterceptor.onTouchActionMove(ev)) {
                        return true;
                    }

                    // Sometimes ACTION_DOWN is not called, so ensure mIndexOnActionDown is set
                    if (mIndexOnActionDown == -1) {
                        mActiveTouchPointerId = ev.getPointerId(0);
                        mIndexOnActionDown = getCenterIndex();
                    }

                    final int activePointerIndex = ev.findPointerIndex(mActiveTouchPointerId);
                    if (activePointerIndex == -1) {
                        Log.e(TAG, "Invalid pointerId=" + mActiveTouchPointerId + " in onTouchEvent");
                        break;
                    }

                    mLastMotionY = (int) ev.getY(activePointerIndex);
                    break;

                case MotionEvent.ACTION_UP:
                    if (mTouchInterceptor != null && mTouchInterceptor.onTouchActionUp(ev)) {
                        return true;
                    }

                    mFlingCalled = false;
                    BubbleFlowView.this.onTouchEvent(ev);
                    if (mFlingCalled == false) {
                        setCenterIndex(getCenterIndex());
                        if (DEBUG) {
                            Log.d(TAG, "No fling - back to middle!");
                        }
                    }
                    mIndexOnActionDown = -1;
                    mActiveTouchPointerId = INVALID_POINTER;
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    mActiveTouchPointerId = INVALID_POINTER;
                    break;

            }

            return false;
        }
    };

    private OnClickListener mViewOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // If we just registered a vertical gesture, don't trigger a click also.
            long delta = System.currentTimeMillis() - mLastVerticalGestureTime;
            if (delta < 33) {
                return;
            }

            int index = mViews.indexOf(v);
            if (index > -1) {
                int currentCenterIndex = getCenterIndex();
                if (currentCenterIndex != index) {
                    setCenterIndex(index);
                } else {
                    if (mBubbleFlowListener != null) {
                        mBubbleFlowListener.onCenterItemClicked(BubbleFlowView.this, v);
                    }
                }
            }
        }
    };

    OnTouchListener mViewOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            boolean result = false;
            if (mViews.indexOf(view) == getCenterIndex()) {

                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mTouchView = view;
                    mLongPress = false;
                    mStillTouchFrameCount = 0;
                    mCenterViewTouchPointerId = event.getPointerId(0);
                    mCenterViewDownX = event.getX();
                    mCenterViewDownY = event.getY();

                    if (DEBUG) {
                        Log.d(TAG, "[longpress] onTouch() DOWN: mStillTouchFrameCount=" + mStillTouchFrameCount);
                    }
                    if (MainController.get() != null) {
                        MainController.get().scheduleUpdate();
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    mTouchView = null;
                    mLongPress = false;
                    mStillTouchFrameCount = -1;
                    if (DEBUG) {
                        Log.d(TAG, "[longpress] onTouch() UP: mStillTouchFrameCount=" + mStillTouchFrameCount);
                    }
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (mCenterViewTouchPointerId != INVALID_POINTER) {
                        final int pointerIndex = event.findPointerIndex(mCenterViewTouchPointerId);
                        if (pointerIndex != -1) {
                            float x = event.getX(pointerIndex);
                            float y = event.getY(pointerIndex);
                            float absXDelta = Math.abs(mCenterViewDownX - x);
                            float absYDelta = Math.abs(mCenterViewDownY - y);

                            int viewsSize = mViews.size();
                            // If there's only 1 view, we don't need to worry about not consuming the input that should go towards
                            // making the BubbleFlow scroll between its items, so just start working towards making this a long press.
                            if (viewsSize == 1) {
                                int distance = Config.dpToPx(6);
                                if (absXDelta*absXDelta + absYDelta*absYDelta > distance*distance) {    // save a squareroot call
                                    if (DEBUG) {
                                        Log.d(TAG, "[longpress] onTouch() MOVE: delta:" + Util.distance(0, 0, absXDelta, absYDelta) + " > " + distance);
                                    }
                                    mStillTouchFrameCount = LONG_PRESS_FRAMES-1;
                                } else {
                                    mStillTouchFrameCount++;
                                }
                            } else if (viewsSize > 1) {
                                if (mStillTouchFrameCount >= 0) {
                                    if (absYDelta > 8.f) {
                                        mStillTouchFrameCount = LONG_PRESS_FRAMES-1;
                                        if (DEBUG) {
                                            Log.e(TAG, "[longpress] onTouch() MOVE: [FORCE], absYDelta:" + absYDelta);
                                        }
                                    } else if (absXDelta > 3.f) {
                                        mStillTouchFrameCount = -1;
                                        if (DEBUG) {
                                            Log.e(TAG, "[longpress] onTouch() MOVE: [CANCEL] mStillTouchFrameCount=" + mStillTouchFrameCount
                                                    + ", absXDelta:" + absXDelta + ", absYDelta:" + absYDelta);
                                        }
                                    } else {
                                        if (DEBUG) {
                                            Log.d(TAG, "[longpress] onTouch() MOVE: absXDelta:" + absXDelta + ", absYDelta:" + absYDelta);
                                        }
                                    }

                                }
                            }
                        }
                    }
                }

                result = mVerticalGestureDetector.onTouchEvent(event);
                VerticalGestureListener.GestureDirection gestureDirection = mVerticalGestureListener.getLastGestureDirection();
                if (gestureDirection == VerticalGestureListener.GestureDirection.Down
                        || gestureDirection == VerticalGestureListener.GestureDirection.Up) {
                    mLastVerticalGestureTime = System.currentTimeMillis();
                    mVerticalGestureListener.resetLastGestureDirection();
                    if (mBubbleFlowListener != null) {
                        mBubbleFlowListener.onCenterItemSwiped(gestureDirection);
                    }
                }
            }
            return result;
        }
    };
}
