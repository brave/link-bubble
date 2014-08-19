package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.linkbubble.R;
import com.linkbubble.Settings;

public class BubblePlateView extends ImageView {

    public BubblePlateView(Context context) {
        this(context, null);
    }

    public BubblePlateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubblePlateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            setImageDrawable(getResources().getDrawable(R.drawable.bubble_plate_light));
            return;
        }

        setImageDrawable(getResources().getDrawable(Settings.get().useDarkTheme() ? R.drawable.bubble_plate_dark : R.drawable.bubble_plate_light));
    }
}
