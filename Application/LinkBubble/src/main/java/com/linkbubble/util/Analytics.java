package com.linkbubble.util;

public class Analytics {

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
            StatHat.get().ezPostValue("time_saved", (double)(time) / 1000.f);
        }
    }

}
