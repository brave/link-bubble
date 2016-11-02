/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;

import com.linkbubble.MainController;
import com.linkbubble.R;

public class NotificationNewBraveBrowserActivity extends Activity {

    public static final int NOTIFICATION_ID = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainController mainController = MainController.get();
        if (mainController != null) {
            mainController.switchToBubbleView(false);
        }
        try {
            Intent gpsIntent = new Intent(Intent.ACTION_VIEW);
            gpsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            gpsIntent.setData(Uri.parse("market://details?id=" + getResources().getString(R.string.tab_based_browser_id_name)));
            startActivity(gpsIntent);
        } catch (android.content.ActivityNotFoundException anfe) {
        }

        finish();
    }

}