package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.linkbubble.Config;
import com.linkbubble.R;
import com.linkbubble.Settings;

import java.net.URL;


public class ProgressIndicatorView extends ImageView {

    private ProgressIndicatorDrawable mProgressDrawable;
    private URL mUrl;
    private float mMaxProgress = 100;

    public ProgressIndicatorView(Context context) {
        this(context, null);
    }

    public ProgressIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            return;
        }

        mProgressDrawable = new ProgressIndicatorDrawable(Settings.get().getThemedDefaultProgressColor(),
                (int) Config.mBubbleWidth - getResources().getDimensionPixelSize(R.dimen.bubble_progress_size_offset),
                getResources().getDimensionPixelSize(R.dimen.bubble_progress_stroke));
        setImageDrawable(mProgressDrawable);
    }

    public void setColor(int rgb) {
        mProgressDrawable.setColor(rgb);
    }

    public int getProgress() {
        return (int)(mProgressDrawable.getProgress() * 100);
    }

    public void setProgress(int progress, URL url) {
        float progressN = (float)progress / mMaxProgress;
        float currentProgress = mProgressDrawable.getProgress();

        // If the url is the same, and currently we're at 100%, and this progress is < 100%,
        // don't change the visual arc as it just looks messy.
        if (progress != 0 && currentProgress >= .999f
                && progressN < .999f
                && (mUrl != null && mUrl.toString().equals(url.toString()))) {
            return;
        }

        if (mUrl == null || mUrl.getHost().equals(url.getHost()) == false) {
            // ensure color is set back to default when the url host changes
            mProgressDrawable.setColor(null);
        }

        mUrl = url;
        mProgressDrawable.setProgress(progress);
    }
}
