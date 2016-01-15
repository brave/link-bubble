/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.linkbubble.R;

import java.net.URL;

public class ProgressIndicator extends FrameLayout {

    private int mMax;
    private int mProgress;
    private URL mUrl;

    private ProgressArcView mProgressArcView;

    public ProgressIndicator(Context context) {
        this(context, null);
    }

    public ProgressIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        mMax = 100;
        mProgress = 0;

        mProgressArcView = new ProgressArcView(getContext());
        int bubbleProgressSize = getResources().getDimensionPixelSize(R.dimen.bubble_progress_size);
        FrameLayout.LayoutParams arcLP = new LayoutParams(bubbleProgressSize, bubbleProgressSize);
        arcLP.gravity = Gravity.CENTER;
        addView(mProgressArcView, arcLP);
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
        invalidate();
    }

    public URL getUrl() {
        return mUrl;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress, URL url) {
        mUrl = url;
        mProgress = progress;
        mProgressArcView.setProgress(progress, mMax, url);
    }

    public void setColor(int color) {
        mProgressArcView.setColor(color);
    }


    private static class ProgressArcView extends View {
        private Paint mPaint;
        private RectF mOval;
        private float mProgress;
        private String mUrl;

        private int mColor;
        void setColor(int color) {
            mColor = color;
            mPaint.setColor(mColor);
        }

        public ProgressArcView(Context context) {
            super(context);

            Resources resources = context.getResources();
            int strokeWidth = resources.getDimensionPixelSize(R.dimen.bubble_progress_stroke);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(mColor);
            mPaint.setStrokeWidth(strokeWidth);

            int size = resources.getDimensionPixelSize(R.dimen.bubble_progress_size) - strokeWidth;
            float offset = (float)(strokeWidth)/2.f;
            mOval = new RectF(offset, offset, size+offset, size+offset);

            mPaint.setStyle(Paint.Style.STROKE);
        }

        void setProgress(int progress, int maxProgress, URL url) {
            float progressN = (float)progress / (float)maxProgress;
            String urlAsString = url.toString();

            // If the url is the same, and currently we're at 100%, and this progress is < 100%,
            // don't change the visual arc as it just looks messy.
            if (progress != 0 && mProgress >= .999f && progressN < .999f && mUrl.equals(urlAsString)) {
                return;
            }

            mUrl = urlAsString;

            mProgress = progressN;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float sweep = 360.f * mProgress;
            canvas.drawArc(mOval, -90, sweep, false, mPaint);
        }
    }
}
