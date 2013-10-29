package com.chrislacy.linkbubble;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Created by gw on 12/10/13.
 */
public class Badge extends TextView {

    private Bubble mBubble;

    public Badge(Context context) {
        super(context);

        setBackgroundResource(R.drawable.badge_shape);
        setTextColor(0xffffffff);
        setTypeface(null, Typeface.BOLD);
        setPadding(Config.dpToPx(6), 0, Config.dpToPx(6), 0);
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void setBubbleCount(int count) {
        if (count < 2) {
            if (mBubble != null)
                mBubble.detachBadge();
        } else {
            setText(Integer.toString(count));
            if (mBubble != null)
                mBubble.attachBadge(this);
        }
    }

    public void attach(Bubble bubble) {
        if (mBubble != null) {
            mBubble.detachBadge();
        }

        if (bubble != null) {
            mBubble = bubble;
            mBubble.attachBadge(this);
        }
    }
}
