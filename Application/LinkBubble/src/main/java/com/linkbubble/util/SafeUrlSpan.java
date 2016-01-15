/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

public class SafeUrlSpan extends URLSpan {
    public SafeUrlSpan(String url) {
        super(url);
    }

    @Override
    public void onClick(View widget) {
        try {
            Uri uri = Uri.parse(getURL());
            Context context = widget.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
        }
    }

    public static void fixUrlSpans(TextView tv) {
        SpannableString current=(SpannableString)tv.getText();
        URLSpan[] spans=
                current.getSpans(0, current.length(), URLSpan.class);

        for (URLSpan span : spans) {
            int start=current.getSpanStart(span);
            int end=current.getSpanEnd(span);

            current.removeSpan(span);
            current.setSpan(new SafeUrlSpan(span.getURL()), start, end, 0);
        }
    }
}