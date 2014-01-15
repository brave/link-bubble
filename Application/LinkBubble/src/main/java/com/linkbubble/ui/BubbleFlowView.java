package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.R;
import com.linkbubble.util.VerticalGestureListener;

import java.util.ArrayList;
import java.util.List;


public class BubbleFlowView extends HorizontalScrollView {

    private static final String TAG = "BubbleFlowView";
    private static final boolean DEBUG = false;
    private static final int INVALID_POINTER = -1;

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

    protected List<View> mViews;
    protected FrameLayout mContent;
    private boolean mIsExpanded;
    private int mWidth;
    protected int mItemWidth;
    protected int mItemHeight;
    private int mEdgeMargin;
    private int mIndexOnActionDown;
    private boolean mFlingCalled;

    private Listener mBubbleFlowListener;
    private TouchInterceptor mTouchInterceptor;
    private int mActiveTouchPointerId = INVALID_POINTER;
    private boolean mInterceptingTouch = false;
    private int mLastMotionY;

    private GestureDetector mVerticalGestureDetector;
    private VerticalGestureListener mVerticalGestureListener = new VerticalGestureListener();
    private long mLastVerticalGestureTime;

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
    }

    void add(View view, boolean insertNextToCenterItem) {

        //view.setBackgroundColor(mViews.size() % 2 == 0 ? 0xff660066 : 0xff666600);

        TextView debugIndexTextView = (TextView) view.findViewById(R.id.debug_index);
        if (debugIndexTextView != null) {
            debugIndexTextView.setText("" + mViews.size());
            debugIndexTextView.setVisibility(VISIBLE);
        }

        view.setOnClickListener(mViewOnClickListener);
        view.setOnLongClickListener(mViewOnLongClickListener);
        view.setOnTouchListener(mViewOnTouchListener);

        int centerIndex = getCenterIndex();
        int insertAtIndex = insertNextToCenterItem ? centerIndex + 1 : mViews.size();

        FrameLayout.LayoutParams lp = new LayoutParams(mItemWidth, mItemHeight, Gravity.TOP|Gravity.LEFT);
        lp.leftMargin = mEdgeMargin + insertAtIndex * mItemWidth;
        mContent.addView(view, lp);
        mContent.invalidate();

        if (insertNextToCenterItem) {
            mViews.add(centerIndex+1, view);
        } else {
            mViews.add(view);
        }

        updatePositions();
        updateScales(getScrollX());

        if (insertNextToCenterItem) {
            TranslateAnimation slideOnAnim = new TranslateAnimation(0, 0, -mItemHeight, 0);
            slideOnAnim.setDuration(Constant.BUBBLE_ANIM_TIME);
            slideOnAnim.setFillAfter(true);
            view.startAnimation(slideOnAnim);

            for (int i = centerIndex + 2; i < mViews.size(); i++) {
                View viewToShift = mViews.get(i);
                TranslateAnimation slideRightAnim = new TranslateAnimation(-mItemWidth, 0, 0, 0);
                slideRightAnim.setDuration(Constant.BUBBLE_ANIM_TIME);
                slideRightAnim.setFillAfter(true);
                viewToShift.startAnimation(slideRightAnim);
            }
        }

        ViewGroup.LayoutParams contentLP = mContent.getLayoutParams();
        contentLP.width = (mViews.size() * mItemWidth) + mItemWidth + (2 * mEdgeMargin);
        mContent.setLayoutParams(contentLP);
    }

    void remove(int index) {
        View view = mViews.get(index);
        mViews.remove(view);
        mContent.removeView(view);
        updatePositions();
        updateScales(getScrollX());
        mContent.invalidate();
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
        float fullScaleX = mItemWidth * .3f;
        float minScaleX = mItemWidth * 1.2f;

        float minScale = .7f;
        //int centerIndex = getCenterIndex();
        int size = mViews.size();
        for (int i = 0; i < size; i++) {
            View view = mViews.get(i);
            float xDelta = Math.abs(centerX - ((i * mItemWidth) + mEdgeMargin));
            float targetScale;
            if (xDelta < fullScaleX) {
                targetScale = 1.f;
            } else if (xDelta > minScaleX) {
                targetScale = minScale;
            } else {
                float ratio = 1.f - ((xDelta - fullScaleX) / (minScaleX - fullScaleX));
                targetScale = minScale + (ratio * (1.f-minScale));
            }
            view.setScaleX(targetScale);
            view.setScaleY(targetScale);
        }
    }

    int getCount() {
        return mViews.size();
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
        int scrollToX = mEdgeMargin + (index * mItemWidth) - (mWidth/2) + (mItemWidth/2);
        smoothScrollTo(scrollToX, 0);
    }

    private static final int DEFAULT_ANIM_TIME = 300;

    public void expand() {
        expand(DEFAULT_ANIM_TIME, null);
    }

    public void expand(long time, final AnimationEventListener animationEventListener) {
        if (mIsExpanded) {
            return;
        }

        int size = mViews.size();
        int centerIndex = getCenterIndex();
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

        // Remove and re-add to ensure view is drawn last.
        mContent.removeView(centerView);
        mContent.addView(centerView);
        mIsExpanded = true;
    }

    public void collapse() {
        collapse(DEFAULT_ANIM_TIME, null);
    }

    public void collapse(long time, final AnimationEventListener animationEventListener) {
        if (mIsExpanded == false) {
            return;
        }
        int size = mViews.size();
        int centerIndex = getCenterIndex();
        View centerView = mViews.get(centerIndex);
        boolean addedAnimationListener = false;
        for (int i = 0; i < size; i++) {
            View view = mViews.get(i);
            if (centerView != view) {
                int xOffset = (int) (centerView.getX() - ((i * mItemWidth) + mEdgeMargin));
                TranslateAnimation anim = new TranslateAnimation(0, xOffset, 0, 0);
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

        // Remove and re-add to ensure view is drawn last.
        mContent.removeView(centerView);
        mContent.addView(centerView);
        mIsExpanded = false;
    }

    boolean isExpanded() {
        return mIsExpanded;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);

        updateScales(x);
    }

    private static final int SCROLL_FINISHED_CHECK_TIME = 33;
    private int mScrollFinishedCheckerInitialXPosition;
    private Runnable mScrollFinishedChecker = new Runnable() {

        public void run() {
            if(mScrollFinishedCheckerInitialXPosition - getScrollX() == 0){
                if (mBubbleFlowListener != null) {
                    int currentCenterIndex = getCenterIndex();
                    mBubbleFlowListener.onCenterItemChanged(BubbleFlowView.this, mViews.get(currentCenterIndex));
                }
            }else{
                mScrollFinishedCheckerInitialXPosition = getScrollX();
                postDelayed(mScrollFinishedChecker, SCROLL_FINISHED_CHECK_TIME);
            }
        }
    };

    public void startScrollFinishedCheckTask(){
        mScrollFinishedCheckerInitialXPosition = getScrollX();
        postDelayed(mScrollFinishedChecker, SCROLL_FINISHED_CHECK_TIME);
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
                final int yDiff = (int) Math.abs(y - mLastMotionY);
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
                    startScrollFinishedCheckTask();
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
                    startScrollFinishedCheckTask();
                } else {
                    if (mBubbleFlowListener != null) {
                        mBubbleFlowListener.onCenterItemClicked(BubbleFlowView.this, v);
                    }
                }
            }
        }
    };

    OnLongClickListener mViewOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int index = mViews.indexOf(v);
            if (index > -1) {
                int currentCenterIndex = getCenterIndex();
                if (currentCenterIndex != index) {
                    setCenterIndex(index);
                    startScrollFinishedCheckTask();
                    return true;
                } else {
                    if (mBubbleFlowListener != null) {
                        mBubbleFlowListener.onCenterItemLongClicked(BubbleFlowView.this, v);
                        return true;
                    }
                }
            }
            return false;
        }
    };

    OnTouchListener mViewOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            boolean result = false;
            if (mViews.indexOf(view) == getCenterIndex()) {
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

    String getDebugString() {
        return "count:" + mViews.size() + ", center index:" + getCenterIndex() + ", width:" + mContent.getWidth()
                + ", scrollX:" + getScrollX() + ", total:" + (getScrollX() + mWidth);
    }
}
