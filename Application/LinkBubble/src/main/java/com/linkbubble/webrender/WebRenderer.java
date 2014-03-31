package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebView;

public abstract class WebRenderer {

    public interface Controller {
        public boolean shouldOverrideUrlLoading(String urlAsString);
        public void onReceivedError();
        public void onPageStarted(String urlAsString, Bitmap favIcon);
        public void onPageFinished(String urlAsString);
        public void onDownloadStart(String urlAsString);
    }

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
}