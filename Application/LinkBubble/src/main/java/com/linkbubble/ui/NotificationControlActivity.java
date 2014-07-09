package com.linkbubble.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.linkbubble.BuildConfig;
import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;

public class NotificationControlActivity extends Activity {

    public static final String EXTRA_ACTION     = BuildConfig.PACKAGE_NAME + ".action";
    public static final String ACTION_CLOSE_ALL = "close_all";
    public static final String ACTION_HIDE      = "hide";
    public static final String ACTION_UNHIDE    = "unhide";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        Intent intent = getIntent();
        String action = intent.getStringExtra(EXTRA_ACTION);

        if (action != null) {
            if (action.equals(ACTION_CLOSE_ALL)) {
                if (MainController.get() != null) {
                    MainController.get().saveCurrentTabs();
                    MainController.get().closeAllBubbles(false);
                    MainController.get().finish();
                }
            } else if (action.equals(ACTION_HIDE)) {
                if (MainController.get() != null) {
                    MainController.get().saveCurrentTabs();
                    MainController.get().setHiddenByUser(true);
                }
            } else if (action.equals(ACTION_UNHIDE)) {
                if (MainController.get() != null) {
                    MainController.get().setHiddenByUser(false);
                }
            }
        }

        finish();
    }

}
