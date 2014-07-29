package com.linkbubble.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.linkbubble.BuildConfig;
import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;

public class NotificationCloseTabActivity extends Activity {

    public static final String EXTRA_DISMISS_NOTIFICATION = BuildConfig.PACKAGE_NAME + ".notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        MainController mainController = MainController.get();

        Intent intent = getIntent();

        if (mainController != null) {
            //Log.d("blerg", "*** handle clasTab:" + intent.getIntExtra(EXTRA_DISMISS_NOTIFICATION, -1));
            mainController.closeTab(intent.getIntExtra(EXTRA_DISMISS_NOTIFICATION, -1));
        }

        finish();
    }

}

