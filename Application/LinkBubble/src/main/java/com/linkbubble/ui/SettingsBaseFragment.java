package com.linkbubble.ui;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.ScrollView;
import android.widget.TextView;

import com.linkbubble.BuildConfig;
import com.linkbubble.Config;
import com.linkbubble.R;
import com.linkbubble.util.Util;

public class SettingsBaseFragment extends PreferenceFragment {

    void setPreferenceIcon(Preference preference, int iconResId) {
        setPreferenceIcon(preference, getResources().getDrawable(iconResId));
    }

    /*
     * Ensure icons display at the correct size for the device resolution. Prevents icons with
     * non-standard sizes from causing text to be justified at wrong position.
     * This was an issue with "Share picker" (too small) and preference_theme_* (too large) on Nexus S
     */
    void setPreferenceIcon(Preference preference, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            //getResources().getDrawableForDensity()
            //getResources().getDrawableForDensity()
            Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
            ActivityManager activityManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
            int iconSize = (int) (activityManager.getLauncherLargeIconSize() * .67f);
            //int iconSize = getResources().getDimensionPixelSize(R.dimen.settings_icon_size);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int largest = Math.max(w, h);
            if (largest > 0) {
                if (largest > iconSize) {
                    Bitmap b = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true);
                    drawable = new BitmapDrawable(getResources(), b);
                } else if (largest < iconSize) {
                    Bitmap b = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true);
                    drawable = new BitmapDrawable(getResources(), b);
                }
            }
        }

        preference.setIcon(drawable);
    }
}
