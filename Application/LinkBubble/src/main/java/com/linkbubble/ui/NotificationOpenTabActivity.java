/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.linkbubble.BuildConfig;
import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;

public class NotificationOpenTabActivity extends Activity {

    public static final String EXTRA_DISMISS_NOTIFICATION = BuildConfig.APPLICATION_ID + ".notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainController mainController = MainController.get();

        Intent intent = getIntent();

        if (mainController != null) {
            Log.d("blerg", "*** handle openTabFromNotification:" + intent.getIntExtra(EXTRA_DISMISS_NOTIFICATION, -1));
            mainController.openTabFromNotification(intent.getIntExtra(EXTRA_DISMISS_NOTIFICATION, -1));
        }

        finish();
    }

}

