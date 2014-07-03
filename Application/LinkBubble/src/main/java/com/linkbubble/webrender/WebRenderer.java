package com.linkbubble.webrender;


import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.linkbubble.articlerender.ArticleContent;
import com.linkbubble.ui.ExpandedActivity;
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
        public void onArticleContentReady(ArticleContent articleContent);
    }

    public enum Type {
        DiffBot,
        Stub,
        WebView,
    };

    public static WebRenderer create(Type type, Context context, Controller controller, View webRendererPlaceholder, String TAG) {
        switch (type) {
            case DiffBot:
                return new DiffBotRenderer(context, controller, webRendererPlaceholder, TAG);

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

    class RendererContextWrapper extends ContextWrapper {

        public RendererContextWrapper(Context base) {
            super(base);
        }

        @Override
        public Resources.Theme getTheme() {
            if (ExpandedActivity.get() != null) {
                //return ExpandedActivity.get().getTheme();
            }
            return super.getTheme();
        }

        @Override
        public Object getSystemService(String name) {
            if (ExpandedActivity.get() != null) {
                //return ExpandedActivity.get().getSystemService(name);
            }
            return super.getSystemService(name);
        }

        /*
        @Override
        public void startIntentSender(IntentSender intent,
                                      Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
                throws IntentSender.SendIntentException {
            super.startIntentSender(intent, fillInIntent, flagsMask,
                    flagsValues, extraFlags);
        }

        @Override
        public void startIntentSender(IntentSender intent,
                                      Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
                                      Bundle options) throws IntentSender.SendIntentException {
            super.startIntentSender(intent, fillInIntent, flagsMask,
                    flagsValues, extraFlags, options);
        }

        @Override
        public void startActivity(Intent intent) {
            Log.e("blerg", "startActivity() " + intent.toString());
            super.startActivity(intent);
        }

        @Override
        public void startActivity(Intent intent, Bundle options) {
            Log.e("blerg", "startActivity(i,o) " + intent.toString());
            super.startActivity(intent, options);
        }*/

        public Resources getResources() {
            return getBaseContext().getResources();
        }
    }

    RendererContextWrapper mContext;

    WebRenderer(Context context, Controller controller, View webRendererPlaceholder) {
        super();
        mContext = new RendererContextWrapper(context);
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

    public void runPageInspector(boolean fetchHtml) {}

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

    public ArticleContent getArticleContent() {
        return null;
    }
}