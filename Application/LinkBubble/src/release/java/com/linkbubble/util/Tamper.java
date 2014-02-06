package com.linkbubble.util;

import android.content.Context;

public class Tamper {

    public static boolean isTweaked(Context context) {
        int check = dexguard.util.TamperDetection.checkApk(context);
        return check == 0 ? false : true;
    }

}