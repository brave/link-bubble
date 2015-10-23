package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebView;

import com.linkbubble.articlerender.ArticleContent;
import com.linkbubble.util.YouTubeEmbedHelper;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class WebRenderer {

    public interface GetGeolocationCallback {
        public void onAllow();
    }

    public interface Controller {
        public boolean shouldAdBlockUrl(String urlStr);
        public boolean shouldTrackingProtectionBlockUrl(String baseHost, String urlStr);
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
        public void onUrlLongClick(WebView webView, String url, int type);
        public void onShowBrowserPrompt();
        public void onCloseWindow();
        public void onGeolocationPermissionsShowPrompt(String origin, GetGeolocationCallback callback);
        public void onPageInspectorYouTubeEmbedFound();
        public void onPageInspectorTouchIconLoaded(Bitmap bitmap, String pageUrl);
        public void onPageInspectorDropDownWarningClick();
        void onPagedInspectorThemeColorFound(int color);
        public void onArticleContentReady(ArticleContent articleContent);
    }

    public enum Type {
        Stub,
        WebView,
    };

    public static WebRenderer create(Type type, Context context, Controller controller, View webRendererPlaceholder, String TAG) {
        switch (type) {
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

    protected Controller mController;
    protected URL mUrl;

    WebRendererContextWrapper mContext;

    WebRenderer(Context context, Controller controller, View webRendererPlaceholder) {
        super();
        mContext = new WebRendererContextWrapper(context);
        mController = controller;
    }

    public abstract void destroy();
    
    public abstract View getView();

    public abstract void updateIncognitoMode(boolean incognito);

    public abstract void loadUrl(URL url, Mode mode);

    public abstract void reload();

    public abstract void stopLoading();

    public abstract void hidePopups();

    public abstract void resetPageInspector();

    public void runPageInspector() {}

    public abstract YouTubeEmbedHelper getPageInspectorYouTubeEmbedHelper();

    public abstract String getUserAgentString();

    public abstract void setUserAgentString(String userAgentString);

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

    public ArticleContent getArticleContent() {
        return null;
    }
}