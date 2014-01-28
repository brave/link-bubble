package com.linkbubble;


public class Constant {

    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    public static final int BUBBLE_ANIM_TIME = 300;

    public static final String PRIVACY_POLICY_URL = "http://www.actionlauncher.com/privacy";
    public static final String TERMS_OF_SERVICE_URL = "http://www.actionlauncher.com/terms";

    public static final boolean DEBUG_SHOW_TARGET_REGIONS = false;

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
}
