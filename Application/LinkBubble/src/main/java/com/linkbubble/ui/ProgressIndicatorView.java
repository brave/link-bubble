package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.linkbubble.R;

import java.net.URL;


public class ProgressIndicatorView extends ImageView {

    private ProgressIndicatorDrawable mProgressDrawable;

    public ProgressIndicatorView(Context context) {
        this(context, null);
    }

    public ProgressIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.cpbStyle);
    }

    public ProgressIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            return;
        }

        mProgressDrawable = new ProgressIndicatorDrawable(getResources().getColor(R.color.color_neutral),
                getResources().getDimensionPixelSize(R.dimen.bubble_progress_stroke));
        setImageDrawable(mProgressDrawable);
    }

    public void setColor(int rgb) {
        mProgressDrawable.setColor(rgb);
    }

    URL mUrl;
    public int getProgress() {
        return (int)(mProgressDrawable.getProgress() * 100);
    }

    public void setProgress(int progress, URL url) {
        mUrl = url;
        mProgressDrawable.setProgress(progress);
    }
}
