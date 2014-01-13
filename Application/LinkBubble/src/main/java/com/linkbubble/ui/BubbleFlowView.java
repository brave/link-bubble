package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;
import java.util.List;


public class BubbleFlowView extends HorizontalScrollView {

    List<View> mViews;
    FrameLayout mContent;
    boolean mExpanded;
    int mWidth;
    int mItemWidth;
    int mItemHeight;

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
        //mContent.setBackgroundColor(0x6600ff00);

        addView(mContent);

        mExpanded = true;
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

        mViews.add(view);

        view.setBackgroundColor(mViews.size() % 2 == 0 ? 0x66660066 : 0x66666600);

        ViewGroup.LayoutParams contentLP = mContent.getLayoutParams();
        contentLP.width = mViews.size() * mItemWidth + mItemWidth;
        mContent.setLayoutParams(contentLP);
    }

    void remove(View view) {
        mViews.remove(view);

        mContent.removeView(view);
        mContent.invalidate();
    }

    void expand() {

    }

    void shrink() {

    }

    boolean isExpanded() {
        return mExpanded;
    }
}
