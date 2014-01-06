package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CondensedTextView extends TextView {

    Typeface sCustomTypeface = null;

    public CondensedTextView(Context context) {
        this(context, null);
    }

    public CondensedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CondensedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode() == false) {
            if (sCustomTypeface == null) {
                sCustomTypeface = Typeface.createFromAsset(context.getAssets(), "RobotoCondensed-Regular.ttf");
            }
            setTypeface(sCustomTypeface);
        }
    }
}
