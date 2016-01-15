/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CondensedTextView extends TextView {

    static Typeface sCustomTypeface = null;

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
