package com.linkbubble.webrender;


import android.content.Context;
import android.view.View;
import android.webkit.WebView;

public abstract class WebRenderer {

    public WebRenderer(Context context, View webRendererPlaceholder) {
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