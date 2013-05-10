package com.chrislacy.linkload;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.WebView;

public class ContentWebView extends WebView {

    LinkLoadOverlayView mOverlayView;

    public ContentWebView(Context context) {
        super(context);
    }

    public ContentWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContentWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void setOverlayView(LinkLoadOverlayView overlayView) {
        mOverlayView = overlayView;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (mOverlayView != null) {
                return mOverlayView.onBackDown();
            }
        }

        return super.onKeyDown(keyCode, event);
    }
}
