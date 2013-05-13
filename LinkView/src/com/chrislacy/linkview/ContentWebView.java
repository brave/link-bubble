package com.chrislacy.linkview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.WebView;

public class ContentWebView extends WebView {

    OnKeyDownListener mListener;

    public ContentWebView(Context context) {
        super(context);
    }

    public ContentWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContentWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnKeyDownListener {
        boolean onKeyDown(int keyCode, KeyEvent event);
    };

    public void setOnKeyDownListener(OnKeyDownListener listener) {
        mListener = listener;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mListener != null) {
            if (mListener.onKeyDown(keyCode, event)) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }
}
