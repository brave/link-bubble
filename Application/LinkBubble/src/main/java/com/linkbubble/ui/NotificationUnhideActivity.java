package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;

public class NotificationUnhideActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        MainController mainController = MainController.get();
        if (mainController != null) {
            //Log.d("blerg", "*** handle Unhide");
            mainController.setHiddenByUser(false);
        }

        finish();
    }

}

