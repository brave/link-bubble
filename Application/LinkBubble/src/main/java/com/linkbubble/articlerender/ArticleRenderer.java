package com.linkbubble.articlerender;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;
import com.squareup.otto.Subscribe;


public class ArticleRenderer {

    public interface Controller {
        public void onUrlLongClick(WebView webView, String url, int type);
        public void onDownloadStart(String urlAsString);
        public boolean onBackPressed(WebView webView);
        public void onShowBrowserPrompt();
        public void onFirstPageLoadStarted();
    }

    private Context mContext;
    private WebView mWebView;
    private boolean mIsDestroyed = false;
    private boolean mFirstPageLoadTriggered = false;
    private Controller mController;
    private boolean mRegisteredForBus;

    public ArticleRenderer(Context context, Controller controller, ArticleContent articleContent, View articleRendererPlaceholder) {
        mContext = context;
        mController = controller;
        Log.e(BATTERY_SAVE_TAG, "create: " + this.getClass().getSimpleName());

        mWebView = new WebView(context);
        mWebView.setLayoutParams(articleRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(articleRendererPlaceholder, mWebView);

        mWebView.setDownloadListener(mDownloadListener);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);

        mWebView.setWebViewClient(mWebViewClient);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setTextZoom(Settings.get().getWebViewTextZoom());
        webSettings.setTextZoom(Settings.get().getWebViewTextZoom());
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        display(articleContent, false);

        Log.d("Article", "ArticleRenderer()");
        MainApplication.registerForBus(context, this);
        mRegisteredForBus = true;
    }

    public void display(ArticleContent articleContent) {
        display(articleContent, true);
    }

    private void display(ArticleContent articleContent, boolean reuse) {
        mWebView.stopLoading();
        String urlAsString = articleContent.mUrl.toString();
        mWebView.loadDataWithBaseURL(urlAsString, articleContent.mPageHtml, "text/html", "utf-8", urlAsString);
        Log.d("Article", ".display() - " + (reuse ? "REUSE" : "NEW") + ", url:" + articleContent.mUrl.toString());
    }

    public void destroy() {
        if (mRegisteredForBus) {
            MainApplication.unregisterForBus(mContext, this);
            mRegisteredForBus = false;
        }
        mIsDestroyed = true;
        if (mWebView != null) {
            mWebView.destroy();
            Log.d("Article", "ArticleRenderer.destroy()");
        }
    }

    public View getView() {
        return mWebView;
    }

    public void stopLoading() {
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String urlAsString, Bitmap favIcon) {
            if (mFirstPageLoadTriggered == false) {
                mController.onFirstPageLoadStarted();
            }
        }
    };

    DownloadListener mDownloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String urlAsString, String userAgent,
                                    String contentDisposition, String mimetype,
                                    long contentLength) {
            mController.onDownloadStart(urlAsString);
        }
    };

    View.OnLongClickListener mOnWebViewLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            WebView.HitTestResult hitTestResult = mWebView.getHitTestResult();
            //Log.d(TAG, "onLongClick type: " + hitTestResult.getType());
            switch (hitTestResult.getType()) {
                case WebView.HitTestResult.IMAGE_TYPE:
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                    final String url = hitTestResult.getExtra();
                    if (url == null) {
                        return false;
                    }

                    mController.onUrlLongClick(mWebView, url, hitTestResult.getType());
                    return true;
                }

                case WebView.HitTestResult.UNKNOWN_TYPE:
                default:
                    if (Constant.ACTIVITY_WEBVIEW_RENDERING == false) {
                        mController.onShowBrowserPrompt();
                    }
                    return false;
            }
        }
    };

    View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && mIsDestroyed == false) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK: {
                        return mController.onBackPressed(mWebView);
                    }
                }
            }

            return false;
        }
    };

    private static final String BATTERY_SAVE_TAG = "BatterySaveArticleRenderer";

    private void webviewPause(String via) {
        String msg = "PAUSE (" + via + ") ";
        if (mWebView != null && mIsDestroyed == false) {
            mWebView.onPause();
        }
        Log.d(BATTERY_SAVE_TAG, msg);
    }

    private void webviewResume(String via) {
        String msg = "RESUME (" + via + ") ";
        if (mWebView != null && mIsDestroyed == false) {
            mWebView.onResume();
        }
        Log.d(BATTERY_SAVE_TAG, msg);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUserPresentEvent(MainController.UserPresentEvent event) {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Default:
                webviewResume("userPresent");
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onScreenOffEvent(MainController.ScreenOffEvent event) {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Aggressive:
            case Default:
                webviewPause("screenOff");
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginCollapseTransitionEvent(MainController.BeginCollapseTransitionEvent event) {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Aggressive:
                webviewPause("beginCollapse");
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginExpandTransitionEvent(MainController.BeginExpandTransitionEvent event) {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Aggressive:
                webviewResume("beginExpand");
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHideContentEvent(MainController.HideContentEvent event) {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Aggressive:
            case Default:
                webviewPause("hide event");
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUnhideContentEvent(MainController.UnhideContentEvent event) {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Default:
                webviewResume("unhide event");
                break;
        }
    }
}
