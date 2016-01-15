/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.app.Application;

public class Analytics {

    public static final String GA_PROPERTY_ID = "UA-49396039-1";

    public static void init(Application application) {
    }

    private static final String CATEGORY = "Usage";

    public static final String OPENED_URL_FROM_SETTINGS = "LinkBubble-Settings";
    public static final String OPENED_URL_FROM_NEW_TAB = "LinkBubble-NewTab";
    public static final String OPENED_URL_FROM_NEW_WINDOW = "LinkBubble-NewWindow";
    public static final String OPENED_URL_FROM_RESTORE = "LinkBubble-Restore";
    public static final String OPENED_URL_FROM_HISTORY = "LinkBubble-History";

    public static void trackOpenUrl(String openedFromAppName) {
        /*
        if (openedFromAppName != null) {

            sTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(CATEGORY)
                    .setAction("URL opened from")
                    .setLabel(openedFromAppName)
                    .build());
        }*/
    }

    public static void trackTimeSaved(long time) {
        /*
        if (time > -1) {
            sTracker.send(new HitBuilders.TimingBuilder()
                    .setCategory(CATEGORY)
                    .setValue(time)
                    .setVariable("time saved")
                    .build());
        }*/
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
        /*
        sTracker.setScreenName(screenName);
        sTracker.send(new HitBuilders.AppViewBuilder().build());
        */
    }
}
