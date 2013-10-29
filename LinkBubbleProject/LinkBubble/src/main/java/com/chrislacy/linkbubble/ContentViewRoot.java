package com.chrislacy.linkbubble;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class ContentViewRoot extends FrameLayout {

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private LinearLayout mLayout_BubbleBar;
    private LinearLayout mLayout_ContentView;
    private LinearLayout mLayout_Root;

    private ContentView mContentView;

    public ContentViewRoot(Context context) {
        super(context);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = 0;
        mWindowManagerParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSLUCENT;

        mLayout_Root = new LinearLayout(context);
        mLayout_Root.setOrientation(LinearLayout.VERTICAL);

        mLayout_BubbleBar = new LinearLayout(context);
        mLayout_Root.addView(mLayout_BubbleBar, LayoutParams.MATCH_PARENT, Config.mContentOffset);

        mLayout_ContentView = new LinearLayout(context);
        LinearLayout.LayoutParams contentLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f);
        mLayout_Root.addView(mLayout_ContentView, contentLayoutParams);

        addView(mLayout_Root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mLayout_Root.setBackgroundColor(0xe5000000);
    }

    public void enableWebView(boolean enable) {
        mContentView.enableWebView(enable);
    }

    public void setPivot(float x, float y) {
        mLayout_ContentView.setPivotX(x);
        mLayout_ContentView.setPivotY(y);
    }

    public void setScale(float x, float y) {
        mLayout_ContentView.setScaleX(x);
        mLayout_ContentView.setScaleY(y);
    }

    public void switchContent(ContentView contentView) {
        Util.Assert(mContentView != null);
        mLayout_ContentView.removeView(mContentView);
        mContentView = contentView;
        mLayout_ContentView.addView(mContentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void show(ContentView contentView) {
        Util.Assert(contentView != null);
        if (mContentView != contentView) {
            Util.Assert(mContentView == null);
            mContentView = contentView;
            mLayout_ContentView.addView(mContentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mWindowManager.addView(this, mWindowManagerParams);
        }
    }

    public void hide() {
        Util.Assert(mContentView != null);
        mLayout_ContentView.removeView(mContentView);
        mContentView = null;
        mWindowManager.removeView(ContentViewRoot.this);
    }
}