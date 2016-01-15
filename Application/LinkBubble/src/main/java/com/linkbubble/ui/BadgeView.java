/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.util.ScaleUpAnimHelper;

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

        if (isInEditMode()) {
            setTextColor(R.color.color_text_light);
            setBackground(getResources().getDrawable(R.drawable.badge_plate));
        }

        setBackground(getResources().getDrawable(Settings.get().getDarkThemeEnabled() ? R.drawable.badge_plate_dark : R.drawable.badge_plate));
        setTextColor(Settings.get().getThemedTextColor());

        mCount = 0;
        mAnimHelper = new ScaleUpAnimHelper(this, Constant.BUBBLE_MODE_ALPHA);
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
