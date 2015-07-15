package com.linkbubble.ui;


import android.content.Context;
import android.util.AttributeSet;

import com.linkbubble.R;
import com.linkbubble.Settings;

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
                setImageDrawable(getResources().getDrawable(R.drawable.ic_subject_white_24dp));
                break;

            case Web:
                setImageDrawable(getResources().getDrawable(R.drawable.ic_public_white_24dp));
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
