package com.linkbubble.util;

/**
 * Created by chrislacy on 5/1/2013.
 */
public class Analytics {

    public static final String OPENED_URL_FROM_SETTINGS = "LinkBubble-Settings";
    public static final String OPENED_URL_FROM_NEW_TAB = "LinkBubble-NewTab";
    public static final String OPENED_URL_FROM_NEW_WINDOW = "LinkBubble-NewWindow";
    public static final String OPENED_URL_FROM_RESTORE = "LinkBubble-Restore";
    public static final String OPENED_URL_FROM_HISTORY = "LinkBubble-History";

    public static void trackOpenUrl(String openedFromAppName) {
        if (openedFromAppName != null) {
            StatHat.ezPostValue("opened_from~" + openedFromAppName, 1.0);
        }
    }

}
