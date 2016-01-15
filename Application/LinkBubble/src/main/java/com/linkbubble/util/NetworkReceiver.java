/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.linkbubble.webrender.WebRenderer;

public class NetworkReceiver extends BroadcastReceiver {

    protected WebRenderer mWebRenderer;

    public NetworkReceiver(WebRenderer webRenderer) {
        mWebRenderer = webRenderer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        // If there is a connection reload the webivew.
        if (networkInfo != null) {
            mWebRenderer.reload();
            context.unregisterReceiver(this);
        }
    }
}