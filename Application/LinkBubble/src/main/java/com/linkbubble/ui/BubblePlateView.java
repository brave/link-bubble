/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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

        setImageDrawable(getResources().getDrawable(Settings.get().getDarkThemeEnabled() ? R.drawable.bubble_plate_dark : R.drawable.bubble_plate_light));
    }
}
