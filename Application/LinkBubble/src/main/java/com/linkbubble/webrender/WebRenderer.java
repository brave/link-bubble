package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import com.linkbubble.util.YouTubeEmbedHelper;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class WebRenderer {

    public interface GetGeolocationCallback {
        public void onAllow();
    }

    public interface Controller {
        public boolean shouldOverrideUrlLoading(String urlAsString, boolean viaUserInput);
        public void onLoadUrl(String urlAsString);      // may or may not be called
        public void onReceivedError();
        public void onPageStarted(String urlAsString, Bitmap favIcon);
        public void onPageFinished(String urlAsString);
        public void onDownloadStart(String urlAsString);
        public void onReceivedTitle(String url, String title);
        public void onReceivedIcon(Bitmap bitmap);
        public void onProgressChanged(int progress, String urlAsString);
        public boolean onBackPressed();
        public void onUrlLongClick(String url);
        public void onShowBrowserPrompt();
        public void onCloseWindow();
        public void onGeolocationPermissionsShowPrompt(String origin, GetGeolocationCallback callback);
        public int getPageInspectFlags();
        public void onPageInspectorYouTubeEmbedFound();
        public void onPageInspectorTouchIconLoaded(Bitmap bitmap, String pageUrl);
        public void onPageInspectorDropDownWarningClick();
    }

    public enum Type {
        DiffBot,
        Snacktory,
        Stub,
        WebView,
    };

    public static WebRenderer create(Type type, Context context, Controller controller, View webRendererPlaceholder, String TAG) {
        switch (type) {
            case DiffBot:
                return new DiffBotRenderer(context, controller, webRendererPlaceholder, TAG);

            case Snacktory:
                return new SnacktoryRenderer(context, controller, webRendererPlaceholder, TAG);

            case Stub:
                return new StubRenderer(context, controller, webRendererPlaceholder, TAG);

            case WebView:
                return new WebViewRenderer(context, controller, webRendererPlaceholder, TAG);
        }

        throw new IllegalArgumentException("Invalid type");
    }

    public enum Mode {
        Web,
        Article,
    }

    protected Mode mMode;

    protected Context mContext;
    protected Controller mController;
    protected URL mUrl;

    WebRenderer(Context context, Controller controller, View webRendererPlaceholder) {
        super();
        mContext = context;
        mController = controller;
    }

    public abstract void destroy();
    
    public abstract View getView();

    public abstract void updateIncognitoMode(boolean incognito);

    public abstract void loadUrl(String urlAsString);

    public abstract void reload();

    public abstract void stopLoading();

    public abstract void hidePopups();

    public abstract void resetPageInspector();

    public abstract void runPageInspector();

    public abstract YouTubeEmbedHelper getPageInspectorYouTubeEmbedHelper();

    public void onPageLoadComplete() {}

    public URL getUrl() {
        return mUrl;
    }

    public void setUrl(String urlAsString) throws MalformedURLException {
        mUrl = new URL(urlAsString);
    }

    public void setUrl(URL url) {
        mUrl = url;
    }

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        mMode = mode;
    }
}