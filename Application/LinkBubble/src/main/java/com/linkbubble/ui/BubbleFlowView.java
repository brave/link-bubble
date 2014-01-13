package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import com.linkbubble.R;

import java.util.ArrayList;
import java.util.List;


public class BubbleFlowView extends HorizontalScrollView {

    public interface OnScrollChangedListener {
        void onScrollChanged(BubbleFlowView bubbleFlowView, int x, int y, int oldx, int oldy);
    }

    List<View> mViews;
    FrameLayout mContent;
    boolean mExpanded;
    int mWidth;
    int mItemWidth;
    int mItemHeight;
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

        mExpanded = true;
    }

    public void setBubbleFlowViewListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListener = onScrollChangedListener;
    }

    void configure(int width, int itemWidth, int itemHeight) {
        mWidth = width;
        mItemWidth = itemWidth;
        mItemHeight = itemHeight;
    }

    void add(View view) {
        FrameLayout.LayoutParams lp = new LayoutParams(mItemWidth, mItemHeight, Gravity.TOP|Gravity.LEFT);
        lp.leftMargin = mViews.size() * mItemWidth;
        mContent.addView(view, lp);
        mContent.invalidate();

        view.setBackgroundColor(mViews.size() % 2 == 0 ? 0x66660066 : 0x66666600);

        TextView debugIndexTextView = (TextView) view.findViewById(R.id.debug_index);
        if (debugIndexTextView != null) {
            debugIndexTextView.setText("" + mViews.size());
            debugIndexTextView.setVisibility(VISIBLE);
        }

        mViews.add(view);

        ViewGroup.LayoutParams contentLP = mContent.getLayoutParams();
        contentLP.width = mViews.size() * mItemWidth + mItemWidth;
        mContent.setLayoutParams(contentLP);
    }

    void remove(View view) {
        mViews.remove(view);

        mContent.removeView(view);
        mContent.invalidate();
    }

    int getCenterIndex() {
        int centerX = (mWidth/2) + getScrollX();
        int closestXAbsDelta = Integer.MAX_VALUE;
        int closestIndex = -1;
        for (int i = 0; i < mViews.size(); i++) {
            int x = (i * mItemWidth) + (mItemWidth/2);
            int absDelta = Math.abs(x-centerX);
            if (absDelta < closestXAbsDelta) {
                closestXAbsDelta = absDelta;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    void expand() {

    }

    void shrink() {

    }

    boolean isExpanded() {
        return mExpanded;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(this, x, y, oldX, oldY);
        }
    }

    String getDebugString() {
        return "count:" + mViews.size() + ", center index:" + getCenterIndex() + ", width:" + mContent.getWidth()
                + ", scrollX:" + getScrollX() + ", total:" + (getScrollX() + mWidth);
    }
}
