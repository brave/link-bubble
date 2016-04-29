/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;

import com.linkbubble.MainController;


public class NotificationShowContentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainController mainController = MainController.get();

        if (mainController != null) {
            mainController.setHiddenByUser(false);
            mainController.doAnimateToContentView();
        }

        finish();
    }}
