package com.linkbubble;

import android.content.Context;

public class Constant {

    public enum BubbleAction {
        None,
        Close,
        ConsumeRight,
        ConsumeLeft
    }

    public enum ActionType {
        Unknown,
        View,
        Share,
    }

    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    public static boolean SAVE_CURRENT_TABS = true;

    public static final int BUBBLE_ANIM_TIME = 300;

    public static final float BUBBLE_MODE_ALPHA = 1.f;

    public static final int DESIRED_FAVICON_SIZE = 96;

    public static final String WELCOME_MESSAGE_URL = "http://s3.amazonaws.com/linkbubble/welcome.html";
    public static final String WELCOME_MESSAGE_DISPLAY_URL = "linkbubble.com/welcome";

    // When opening a link in a new tab, there is no reliable way to get the link to be loaded. Use this guy
    // so we can determine when this is occurring, and not pollute the history. #280
    public static final String NEW_TAB_URL = "http://ishouldbeusedbutneverseen55675.com";

    // Check the page every 1 seconds for the presence of drop down items. #270
    public static final int DROP_DOWN_CHECK_TIME = 1000;

    public static final int AUTO_CONTENT_DISPLAY_DELAY = 200;

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

    public static final String DRM_SHARED_PREFERENCES_KEY = "com.linkbubble.license";

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

    public static final String  PRO_DRM_SERVICE_ACTION = "linkbubble.license.PRO_SERVICE";

    public static final String DATA_USER_ENTRY = "User";
    public static final String DATA_USER_EMAIL_KEY_PREFIX = "email_";
    public static final String DATA_USER_TWITTER_KEY_PREFIX = "twitter_";
    public static final String DATA_USER_YAHOO_KEY_PREFIX = "yahoo_";
    public static final int    DATA_USER_MAX_EMAILS = 9;

    public static final String TWITTER_ACCOUNT_TYPE = "com.twitter.android.auth.login";
    public static final String YAHOO_ACCOUNT_TYPE = "com.yahoo.mobile.client.share.account";

    public static final String WEBVIEW_DATABASE_LOCATION = "/data/data/" + BuildConfig.PACKAGE_NAME;
}
