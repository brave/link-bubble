/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.linkbubble.util.YouTubeEmbedHelper;


public class OpenEmbedButton extends ContentViewButton implements View.OnClickListener {

    private static final String TAG = "OpenEmbedButton";

    private OnOpenEmbedClickListener mOnOpenEmbedClickListener;

    private YouTubeEmbedHelper mYouTubeEmbedHelper = null;

    interface OnOpenEmbedClickListener {
        void onYouTubeEmbedOpened();
    }

    public OpenEmbedButton(Context context) {
        this(context, null);
    }

    public OpenEmbedButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpenEmbedButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOnClickListener(this);
    }

    void setOnOpenEmbedClickListener(OnOpenEmbedClickListener listener) {
        mOnOpenEmbedClickListener = listener;
    }

    boolean configure(YouTubeEmbedHelper youTubeEmbedHelper) {
        mYouTubeEmbedHelper = youTubeEmbedHelper;
        if (mYouTubeEmbedHelper != null && mYouTubeEmbedHelper.size() > 0) {
            if (mYouTubeEmbedHelper.mYouTubeResolveInfo != null) {
                Drawable d = mYouTubeEmbedHelper.mYouTubeResolveInfo.loadIcon(getContext().getPackageManager());
                if (d != null) {
                    setImageDrawable(d);
                    setTag(mYouTubeEmbedHelper.mYouTubeResolveInfo);
                    Log.d(TAG, "YouTube embed");
                    setVisibility(VISIBLE);
                    return true;
                }
            }
        }

        setVisibility(GONE);
        return false;
    }


    @Override
    public void onClick(View v) {
        if (mYouTubeEmbedHelper.onOpenInAppButtonClick()) {
            if (mOnOpenEmbedClickListener != null) {
                mOnOpenEmbedClickListener.onYouTubeEmbedOpened();
            }
        }
    }

}
