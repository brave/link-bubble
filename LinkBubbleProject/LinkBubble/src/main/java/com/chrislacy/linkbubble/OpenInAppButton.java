package com.chrislacy.linkbubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.List;

public class OpenInAppButton extends FrameLayout {
    public OpenInAppButton(Context context) {
        this(context, null);
    }

    public OpenInAppButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpenInAppButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    boolean configure(List<ContentView.AppForUrl> appsForUrl) {
        int appsForUrlSize = appsForUrl != null ? appsForUrl.size() : 0;
        if (appsForUrlSize == 1) {
            ContentView.AppForUrl appForUrl = appsForUrl.get(0);
            Drawable d = appForUrl.mResolveInfo.loadIcon(getContext().getPackageManager());
            if (d != null) {
                setBackground(d);
                setVisibility(VISIBLE);
                setTag(appForUrl);
                return true;
            }
        } else if (appsForUrlSize > 1) {

        }

        return false;
    }
}
