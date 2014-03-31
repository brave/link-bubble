package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebView;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class WebRenderer {

    public interface GetGeolocationCallback {
        public void onAllow();
    }

    public interface Controller {
        public boolean shouldOverrideUrlLoading(String urlAsString, boolean viaUserInput);
        public void onReceivedError();
        public void onPageStarted(String urlAsString, Bitmap favIcon);
        public void onPageFinished(String urlAsString);
        public void onDownloadStart(String urlAsString);
        public void onReceivedTitle(String url, String title);
        public void onReceivedIcon(Bitmap bitmap);
        public void onProgressChanged(WebView webView, int progress);
        public void onCloseWindow();
        public void onGeolocationPermissionsShowPrompt(String origin, GetGeolocationCallback callback);
    }

    protected URL mUrl;

    public WebRenderer(Context context, Controller controller, View webRendererPlaceholder) {
        super();
    }

    public abstract void destroy();
    
    public abstract View getView();

    public abstract WebView getWebView();

    public abstract void updateIncognitoMode(boolean incognito);

    public abstract void loadUrl(String urlAsString);

    public abstract void reload();

    public abstract void stopLoading();

    public abstract void hidePopups();

    public URL getUrl() {
        return mUrl;
    }

    public void setUrl(String urlAsString) throws MalformedURLException {
        mUrl = new URL(urlAsString);
    }

    public void setUrl(URL url) {
        mUrl = url;
    }
}