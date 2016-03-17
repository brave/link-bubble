/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.webrender;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    private OnScrollChangedCallback mOnScrollChangedCallback;

    public CustomWebView(Context context) {
        super(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int newX, int newY, int oldX, int oldY)
    {
        super.onScrollChanged(newX, newY, oldX, oldY);
        if(mOnScrollChangedCallback != null) {
            mOnScrollChangedCallback.onScroll(newX, newY, oldX, oldY);
        }
    }

    public OnScrollChangedCallback getOnScrollChangedCallback()
    {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(final OnScrollChangedCallback onScrollChangedCallback)
    {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }


    public static interface OnScrollChangedCallback
    {
        void onScroll(int newX, int newY, int oldX, int oldY);
    }
}
