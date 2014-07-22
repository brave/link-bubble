package com.linkbubble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.linkbubble.ui.Prompt;
import com.linkbubble.util.Util;

import java.util.Vector;

public class OpenLinkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals("com.linkbubble.OPEN_LINK")) {
            String urlAsString = intent.getStringExtra("url");

            PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

            boolean showingTamperPrompt = Util.checkForTamper(context, new Prompt.OnPromptEventListener() {
                @Override
                public void onClick() {
                    MainApplication.openAppStore(context, BuildConfig.STORE_FREE_URL);
                }

                @Override
                public void onClose() {
                }
            });

            if (!showingTamperPrompt) {
                // Don't restore tabs if we've already got tabs open, #389
                if (MainController.get() == null) {
                    // Restore open tabs
                    Vector<String> urls = Settings.get().loadCurrentTabs();
                    if (urls.size() > 0 && DRM.allowProFeatures()) {
                        MainApplication.restoreLinks(context, urls.toArray(new String[urls.size()]));
                    }
                }

                boolean showedWelcomeUrl = false;
                if (Settings.get().getWelcomeMessageDisplayed() == false) {
                    if (!(MainController.get() != null && MainController.get().isUrlActive(Constant.WELCOME_MESSAGE_URL))) {
                        MainApplication.openLink(context, Constant.WELCOME_MESSAGE_URL, null);
                        showedWelcomeUrl = true;
                    }
                }

                MainApplication.openLink(context, urlAsString, true, showedWelcomeUrl ? false : true, "TapPath");
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(urlAsString));
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MainApplication.openInBrowser(context, browserIntent, true);
            }
        }
    }
}
