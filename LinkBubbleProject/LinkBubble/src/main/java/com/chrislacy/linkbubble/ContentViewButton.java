package com.chrislacy.linkbubble;


import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class ContentViewButton extends FrameLayout {

    boolean mIsTouched;

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
            if (getBackground() == null) {
                //setBackgroundColor(sTouchedColor);
            } else {
                getBackground().setColorFilter(sTouchedColor, PorterDuff.Mode.DARKEN);
            }
            invalidate();
        } else if (isTouched == false && mIsTouched != isTouched) {
            //setBackgroundColor(0x00000000);
            if (getBackground() != null) {
                getBackground().clearColorFilter();
            }
            invalidate();
        }

        mIsTouched = isTouched;
    }
}
