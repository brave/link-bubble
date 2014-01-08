package com.linkbubble.ui;


import android.content.Context;
import android.util.AttributeSet;

public class BubbleFlowItemView extends BubbleView {

    protected ContentView mContentView;

    public BubbleFlowItemView(Context context) {
        this(context, null);
    }

    public BubbleFlowItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFlowItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ContentView getContentView() {
        return mContentView;
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

}
