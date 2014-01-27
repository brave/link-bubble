package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.physics.Draggable;
import com.linkbubble.util.ScaleUpAnimHelper;

/**
 * Created by gw on 12/10/13.
 */
public class BadgeView extends TextView {

    int mCount;
    ScaleUpAnimHelper mAnimHelper;

    public BadgeView(Context context) {
        this(context, null);
    }

    public BadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BadgeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mCount = 0;
        mAnimHelper = new ScaleUpAnimHelper(this);
    }

    public void show() {
        mAnimHelper.show();

        Draggable activeDraggable = MainController.get().getBubbleDraggable();
        if (activeDraggable != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            int x = activeDraggable.getDraggableHelper().getXPos();
            if (x > Config.mScreenCenterX) {
                lp.gravity = Gravity.TOP|Gravity.LEFT;
            } else {
                lp.gravity = Gravity.TOP|Gravity.RIGHT;
            }
        }
    }

    public void hide() {
        mAnimHelper.hide();
    }

    public void setCount(int count) {
        mCount = count;
        setText(Integer.toString(count));
    }
}
