package com.linkbubble.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.WebView;

import com.linkbubble.webrender.WebRenderer;

/**
 * Created by kevin on 8/5/15.
 */
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