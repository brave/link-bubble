package com.linkbubble.util;

import android.app.Application;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Analytics {

    public static final String GA_PROPERTY_ID = "UA-49396039-1";

    static Tracker sTracker;

    public static void init(Application application) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(application);
        sTracker = analytics.newTracker(GA_PROPERTY_ID);
        GoogleAnalytics.getInstance(application).setLocalDispatchPeriod(15);
    }

    public static final String OPENED_URL_FROM_SETTINGS = "LinkBubble-Settings";
    public static final String OPENED_URL_FROM_NEW_TAB = "LinkBubble-NewTab";
    public static final String OPENED_URL_FROM_NEW_WINDOW = "LinkBubble-NewWindow";
    public static final String OPENED_URL_FROM_RESTORE = "LinkBubble-Restore";
    public static final String OPENED_URL_FROM_HISTORY = "LinkBubble-History";

    public static void trackOpenUrl(String openedFromAppName) {
        if (openedFromAppName != null) {
            StatHat.get().ezPostCount("opened_from~" + openedFromAppName, 1);
            StatHat.get().ezPostCount("tab_opened", 1);
        }
    }

    public static void trackTimeSaved(long time) {
        if (time > -1) {
            StatHat.get().ezPostValue("time_saved", (double) (time) / 1000.f);
        }
    }

    public static final String UPGRADE_PROMPT_SINGLE_APP = "single_app";
    public static final String UPGRADE_PROMPT_SINGLE_APP_SET = "single_app_set";
    public static final String UPGRADE_PROMPT_SINGLE_TAB_OPEN_URL = "single_tab_open_url";
    public static final String UPGRADE_PROMPT_SINGLE_TAB_REDIRECT = "single_tab_redirect";

    public static void trackUpgradePromptDisplayed(String promptType) {
        StatHat.get().ezPostCount("upgrade_prompt_displayed~" + promptType, 1);
    }

    public static void trackUpgradePromptClicked(String promptType) {
        StatHat.get().ezPostCount("upgrade_prompt_clicked~" + promptType, 1);
    }

    public static void trackScreenView(String screenName) {
        sTracker.setScreenName(screenName);
        sTracker.send(new HitBuilders.AppViewBuilder().build());
    }
}
