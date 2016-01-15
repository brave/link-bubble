/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;

import com.linkbubble.util.CrashTracking;

public class DownloadHandlerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * This activity doesn't actually do anything. It exists solely so that it's componentEnabled flag can be turned on
         * briefly and we can query if a given URL would result in a download (based on the Intent filter in AndroidManifest.xml),
         * and if so, kick to the default browser.
         */

        /*
        Intent intent = getIntent();
        String url = intent.getDataString();
        if (url != null) {
            MainApplication.openInBrowser(this, intent, true);
        }*/

        finish();
    }

}
