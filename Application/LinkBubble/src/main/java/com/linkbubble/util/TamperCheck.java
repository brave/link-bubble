package com.linkbubble.util;


import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.ui.Prompt;

public class TamperCheck {

    public static boolean checkForTamper(Context context, Prompt.OnPromptEventListener listener) {
        if (Tamper.isTweaked(context)) {
            String text = context.getResources().getString(R.string.tampered_apk);
            Drawable icon = null;
            final ResolveInfo storeResolveInfo = MainApplication.getStoreViewResolveInfo(context);
            if (storeResolveInfo != null) {
                icon = storeResolveInfo.loadIcon(context.getPackageManager());
            }
            Prompt.show(text, icon, Prompt.LENGTH_LONG, listener);
            return true;
        }

        return false;
    }

}
