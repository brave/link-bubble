package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.linkbubble.R;

import java.net.URL;


public class ProgressIndicatorView extends ImageView {

    private ProgressIndicatorDrawable mProgressDrawable;
    private String mUrl;
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

        mProgressDrawable = new ProgressIndicatorDrawable(getResources().getColor(R.color.color_progress_default),
                getResources().getDimensionPixelSize(R.dimen.bubble_progress_size),
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
        String urlAsString = url.toString();

        float currentProgress = mProgressDrawable.getProgress();

        // If the url is the same, and currently we're at 100%, and this progress is < 100%,
        // don't change the visual arc as it just looks messy.
        if (progress != 0 && currentProgress >= .999f && progressN < .999f && mUrl.equals(urlAsString)) {
            return;
        }

        mUrl = urlAsString;
        mProgressDrawable.setProgress(progress);
    }
}
