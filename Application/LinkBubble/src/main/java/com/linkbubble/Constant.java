package com.linkbubble;

import android.content.Context;

public class Constant {

    public enum BubbleAction {
        None,
        Close,
        BackButton,
        ConsumeRight,
        ConsumeLeft,
        //LinkDoubleTap,
    }

    public enum ActionType {
        Unknown,
        View,
        Share,
    }

    // If true, transfer the WebView to an Activity. Enables text selection and drop down items to work
    public static boolean ACTIVITY_WEBVIEW_RENDERING = false;

    // Make GW's changes per d46678694ab79ed7a4aec5e293beff9ae9a62382 optional
    public static final boolean DYNAMIC_ANIM_STEP = true;

    public static final boolean PROFILE_FPS = false;

    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    public static final boolean EXPANDED_ACTIVITY_DEBUG = false;

    public static boolean SAVE_CURRENT_TABS = true;

    public static final int TRIAL_TIME = 1000 * 60 * 60 * 24;

    public static boolean ENABLE_FLUSH_CACHE_SERVICE = false;

    public static boolean COVER_STATUS_BAR = false;//(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ? true : false;
    public static boolean BOTTOM_CANVAS_MASK = false;
    public static boolean TOP_CANVAS_MASK = true;

    public static final int BUBBLE_ANIM_TIME = 550;
    public static final int BUBBLE_FLOW_ANIM_TIME = 333;
    public static final int BUBBLE_SLIDE_ON_SCREEN_TIME = BUBBLE_FLOW_ANIM_TIME;

    public static final float BUBBLE_MODE_ALPHA = 1.f;

    public static final int DESIRED_FAVICON_SIZE = 96;

    public static final String WELCOME_MESSAGE_URL = "http://linkbubble.com/device/welcome.html";
    public static final String WELCOME_MESSAGE_DISPLAY_URL = "linkbubble.com/device/welcome";

    // When opening a link in a new tab, there is no reliable way to get the link to be loaded. Use this guy
    // so we can determine when this is occurring, and not pollute the history. #280
    public static final String NEW_TAB_URL = "http://ishouldbeusedbutneverseen55675.com";

    public static final int AUTO_CONTENT_DISPLAY_DELAY = 200;

    public static final int TOUCH_ICON_MAX_SIZE = 256;

    public static final int EMPTY_WEBVIEW_CACHE_INTERVAL = 7 * 24 * 60 * 60 * 1000;

    public static final String PRIVACY_POLICY_URL = "http://www.linkbubble.com/privacy";
    public static final String TERMS_OF_SERVICE_URL = "http://www.linkbubble.com/terms";

    public static final boolean DEBUG_SHOW_TARGET_REGIONS = false;

    public static final String SHARE_PICKER_NAME = "com.linkbubble.SharePicker";

    public static String getOSFlavor() {
        int apiVersion = android.os.Build.VERSION.SDK_INT;
        String flavor = "";
        switch (apiVersion) {
            case 15: flavor = "4.0"; break;
            case 16: flavor = "4.1"; break;
            case 17: flavor = "4.2"; break;
            case 18: flavor = "4.3"; break;
            case 19: flavor = "4.4"; break;
            case 20: flavor = "4.5"; break;
        }
        return flavor;
    }

    public static String DEVICE_ID = "<unset>";
    public static String getValidDeviceId() {
        if (DEVICE_ID.equals("<unset>") || DEVICE_ID == null || DEVICE_ID.length() < 4) {
            return null;
        }
        return DEVICE_ID;
    }

    private static String sSecureAndroidId = null;
    public static String getSecureAndroidId(Context context) {
        if (sSecureAndroidId == null) {
            sSecureAndroidId = android.provider.Settings.Secure.getString(context.getApplicationContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        }
        return sSecureAndroidId;
    }

    // String to represent the group all the notifications will be a part of
    public static final String NOTIFICATION_GROUP_KEY_ARTICLES = "group_key_articles";

    public static final String DATA_USER_ENTRY = "User";
    public static final String DATA_USER_EMAIL_KEY_PREFIX = "email_";
    public static final String DATA_USER_TWITTER_KEY_PREFIX = "twitter_";
    public static final String DATA_USER_YAHOO_KEY_PREFIX = "yahoo_";
    public static final int    DATA_USER_MAX_EMAILS = 9;

    public static final String TWITTER_ACCOUNT_TYPE = "com.twitter.android.auth.login";
    public static final String YAHOO_ACCOUNT_TYPE = "com.yahoo.mobile.client.share.account";

    public static final String DATA_TRIAL_ENTRY = "Trial";
    public static final String DATA_TRIAL_EMAIL = "email";

    public static final String WEBVIEW_DATABASE_LOCATION = "/data/data/" + BuildConfig.APPLICATION_ID;

    public static final String USER_AGENT_CHROME_PHONE = "Mozilla/5.0 (Linux; Android 4.4.2; GT-I9505 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.114 Mobile Safari/537.36";
    public static final String USER_AGENT_CHROME_TABLET = "Mozilla/5.0 (Linux; Android 4.4.2; Nexus 7 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.48 Safari/537.36";
    public static final String USER_AGENT_CHROME_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36";

    public static final String DIFFBOT_KEY = "0f33cad70f2dcf3f9be136ed713c6e58";

    public static final String POCKET_PACKAGE_NAME = "com.ideashower.readitlater.pro";

    public static final String ABOUT_BLANK_URI = "about:blank";
}
