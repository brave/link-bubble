package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import com.linkbubble.R;

import java.util.ArrayList;
import java.util.List;


public class BubbleFlowView extends HorizontalScrollView {

    private static final String TAG = "BubbleFlowView";
    private static final boolean DEBUG = false;

    public interface OnScrollChangedListener {
        void onScrollChanged(BubbleFlowView bubbleFlowView, int x, int y, int oldx, int oldy);
    }

    private List<View> mViews;
    private FrameLayout mContent;
    private boolean mIsExpanded;
    private int mWidth;
    private int mItemWidth;
    private int mItemHeight;
    private int mEdgeMargin;
    private int mIndexOnActionDown;
    private boolean mFlingCalled;

    private OnScrollChangedListener mOnScrollChangedListener;

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
    }

    public void setBubbleFlowViewListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListener = onScrollChangedListener;
    }

    void configure(int width, int itemWidth, int itemHeight) {
        mWidth = width;
        mItemWidth = itemWidth;
        mItemHeight = itemHeight;
        mEdgeMargin = (width - itemWidth) / 2;
    }

    void add(View view) {
        FrameLayout.LayoutParams lp = new LayoutParams(mItemWidth, mItemHeight, Gravity.TOP|Gravity.LEFT);
        lp.leftMargin = mEdgeMargin + mViews.size() * mItemWidth;
        mContent.addView(view, lp);
        mContent.invalidate();

        view.setBackgroundColor(mViews.size() % 2 == 0 ? 0x66660066 : 0x66666600);

        TextView debugIndexTextView = (TextView) view.findViewById(R.id.debug_index);
        if (debugIndexTextView != null) {
            debugIndexTextView.setText("" + mViews.size());
            debugIndexTextView.setVisibility(VISIBLE);
        }

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mViews.indexOf(v);
                if (index > -1) {
                    setCenterIndex(index);
                }
            }
        });

        mViews.add(view);

        updatePositions();

        ViewGroup.LayoutParams contentLP = mContent.getLayoutParams();
        contentLP.width = (mViews.size() * mItemWidth) + mItemWidth + (2 * mEdgeMargin);
        mContent.setLayoutParams(contentLP);
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

    void remove(int index) {
        View view = mViews.get(index);
        mViews.remove(view);
        mContent.removeView(view);
        updatePositions();
        mContent.invalidate();
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

    void setCenterIndex(int index) {
        int scrollToX = mEdgeMargin + (index * mItemWidth) - (mWidth/2) + (mItemWidth/2);
        smoothScrollTo(scrollToX, 0);
    }

    private static final int ANIM_DURATION = 500;

    void expand() {
        if (mIsExpanded) {
            return;
        }

        int size = mViews.size();
        int centerIndex = getCenterIndex();
        View centerView = mViews.get(centerIndex);
        for (int i = 0; i < size; i++) {
            View view = mViews.get(i);
            if (centerView != view) {
                int xOffset = (int) (centerView.getX() - ((i * mItemWidth) + mEdgeMargin));
                TranslateAnimation anim = new TranslateAnimation(xOffset, 0, 0, 0);
                anim.setDuration(ANIM_DURATION);
                anim.setFillAfter(true);
                view.startAnimation(anim);
            }
        }
        // Remove and re-add to ensure view is drawn last.
        mContent.removeView(centerView);
        mContent.addView(centerView);
        mIsExpanded = true;
    }

    void shrink() {
        if (mIsExpanded == false) {
            return;
        }
        int size = mViews.size();
        View centerView = mViews.get(getCenterIndex());
        for (int i = 0; i < size; i++) {
            View view = mViews.get(i);
            if (centerView != view) {
                int xOffset = (int) (centerView.getX() - ((i * mItemWidth) + mEdgeMargin));
                TranslateAnimation anim = new TranslateAnimation(0, xOffset, 0, 0);
                anim.setDuration(ANIM_DURATION);
                anim.setFillAfter(true);
                view.startAnimation(anim);
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
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(this, x, y, oldX, oldY);
        }

        updateScales(x);
    }

    void updateScales(int x) {

        float centerX = x + (mWidth/2) - (mItemWidth/2);
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

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            final int action = ev.getAction();

            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mIndexOnActionDown = getCenterIndex();
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Sometimes ACTION_DOWN is not called, so ensure mIndexOnActionDown is set
                    if (mIndexOnActionDown == -1) {
                        mIndexOnActionDown = getCenterIndex();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mFlingCalled = false;
                    BubbleFlowView.this.onTouchEvent(ev);
                    if (mFlingCalled == false) {
                        setCenterIndex(getCenterIndex());
                        if (DEBUG) {
                            Log.d(TAG, "No fling - back to middle!");
                        }
                    }
                    mIndexOnActionDown = -1;
                    return true;
            }

            return false;
        }
    };

    String getDebugString() {
        return "count:" + mViews.size() + ", center index:" + getCenterIndex() + ", width:" + mContent.getWidth()
                + ", scrollX:" + getScrollX() + ", total:" + (getScrollX() + mWidth);
    }
}
