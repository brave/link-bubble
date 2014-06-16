package com.linkbubble.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.linkbubble.R;

public class ArticleModeButton extends ContentViewButton {

    enum State {
        Article,
        Web
    }

    private State mState;

    public ArticleModeButton(Context context) {
        this(context, null);
    }

    public ArticleModeButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArticleModeButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setState(State state) {
        switch (state) {
            case Article:
                setImageDrawable(getResources().getDrawable(R.drawable.ic_action_list));
                break;

            case Web:
                setImageDrawable(getResources().getDrawable(R.drawable.ic_action_globe));
                break;
        }

        mState = state;
    }

    public State getState() {
        return mState;
    }

    public void toggleState() {
        setState(mState == State.Article ? State.Web : State.Article);
    }
}
