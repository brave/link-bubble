/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.linkbubble.R;
import com.linkbubble.Settings;

public class ContentViewButton extends FrameLayout {

    boolean mIsTouched;
    private int mMaxIconSize;
    private ImageView mImageView;

    static final int sTouchedColor = 0x555d5d5e;

    public ContentViewButton(Context context) {
        this(context, null);
    }

    public ContentViewButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContentViewButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOnTouchListener(mButtonOnTouchListener);

        mImageView = new ImageView(context);
        mImageView.setScaleType(ImageView.ScaleType.CENTER);
        addView(mImageView);
    }

    private OnTouchListener mButtonOnTouchListener = new OnTouchListener() {

        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    setIsTouched(true);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    setIsTouched(false);
                    break;
                }
            }
            return false;
        }
    };

    void setIsTouched(boolean isTouched) {

        if (isTouched && mIsTouched != isTouched) {
            setBackgroundColor(sTouchedColor);
            invalidate();
        } else if (isTouched == false && mIsTouched != isTouched) {
            setBackgroundColor(0);
            invalidate();
        }

        mIsTouched = isTouched;
    }

    int getMaxIconSize() {
        if (mMaxIconSize == 0) {
            mMaxIconSize = getResources().getDimensionPixelSize(R.dimen.content_view_button_max_height);
        }
        return mMaxIconSize;
    }

    public void setImageDrawable(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            int maxIconSize = getMaxIconSize();

            BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
            int width = bitmapDrawable.getBitmap().getWidth();
            int height = bitmapDrawable.getBitmap().getHeight();
            if (width > 0 && height > 0 && (width > maxIconSize || height > maxIconSize)) {
                int newHeight;
                int newWidth;
                if (width > height) {
                    newWidth = maxIconSize;
                    newHeight = (int)((float)(height / width) * maxIconSize);
                } else if (width < height) {
                    newHeight = maxIconSize;
                    newWidth = (int)((float)(width / height) * maxIconSize);
                    if (0 == newWidth) {
                        newWidth = newHeight;
                    }
                } else {
                    newWidth = newHeight = maxIconSize;
                }

                // Potential fix for user exceptions below saying that width and height must be > 0
                newWidth = Math.max(1, newWidth);
                newHeight = Math.max(1, newHeight);

                try {
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), newWidth, newHeight, true);
                    drawable = new BitmapDrawable(getResources(), resizedBitmap);
                } catch (java.lang.OutOfMemoryError ex) {

                }
            }
        }

        mImageView.setImageDrawable(drawable);
    }

    public void updateTheme(Integer color) {
        Drawable d = mImageView.getDrawable();
        if (d != null) {
            int textColor;
            if (color == null || !Settings.get().getThemeToolbar()) {
                textColor = Settings.get().getThemedTextColor();
            } else {
                textColor = Settings.COLOR_WHITE;
            }
            DrawableCompat.setTint(d, textColor);
        }
    }
}
