package com.linkbubble.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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

        MainController mainController = MainController.get();

        Intent intent = getIntent();
        String action = intent.getStringExtra(EXTRA_ACTION);

        Log.e("blerg", "NotificationControlActivity.onCreate() - action:" + action);
        if (action.equals(ACTION_CLOSE_ALL)) {
            Toast.makeText(this, "*** NotificationAction: " + action, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "NotificationAction: " + action, Toast.LENGTH_SHORT).show();
        }

        if (action != null) {
            if (action.equals(ACTION_CLOSE_ALL)) {
                if (mainController != null) {
                    Log.e("blerg", "*** handle " + action);
                    mainController.saveCurrentTabs();
                    mainController.closeAllBubbles(false);
                    mainController.finish();
                }
            } else if (action.equals(ACTION_HIDE)) {
                if (mainController != null) {
                    Log.e("blerg", "*** handle " + action);
                    mainController.saveCurrentTabs();
                    mainController.setHiddenByUser(true);
                }
            } else if (action.equals(ACTION_UNHIDE)) {
                if (mainController != null) {
                    Log.e("blerg", "*** handle " + action);
                    mainController.setHiddenByUser(false);
                }
            }
        }

        finish();
    }

}
