package com.linkbubble.webrender;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.ui.TabView;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.PageInspector;
import com.linkbubble.util.Util;

import java.net.URL;

public class WebViewRenderer extends WebRenderer {

    private WebView mWebView;

    public WebViewRenderer(Context context, View webRendererPlaceholder) {
        super(context, webRendererPlaceholder);

        mWebView = new WebView(context);
        mWebView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(webRendererPlaceholder, mWebView);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setTextZoom(Settings.get().getWebViewTextZoom());
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportMultipleWindows(DRM.isLicensed() ? true : false);
        webSettings.setGeolocationDatabasePath(Constant.WEBVIEW_DATABASE_LOCATION);
    }

    @Override
    public void destroy() {
        mWebView.destroy();
    }

    @Override
    public View getView() {
        return mWebView;
    }

    @Override
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    public void updateIncognitoMode(boolean incognito) {
        if (incognito) {
            mWebView.getSettings().setCacheMode(mWebView.getSettings().LOAD_NO_CACHE);
            mWebView.getSettings().setAppCacheEnabled(false);
            mWebView.clearHistory();
            mWebView.clearCache(true);

            mWebView.clearFormData();
            mWebView.getSettings().setSavePassword(false);
            mWebView.getSettings().setSaveFormData(false);
        } else {
            mWebView.getSettings().setCacheMode(mWebView.getSettings().LOAD_DEFAULT);
            mWebView.getSettings().setAppCacheEnabled(true);

            mWebView.getSettings().setSavePassword(true);
            mWebView.getSettings().setSaveFormData(true);
        }
    }

    @Override
    public void loadUrl(String urlAsString) {
        mWebView.loadUrl(urlAsString);
    }

    @Override
    public void reload() {
        mWebView.reload();
    }

    @Override
    public void stopLoading() {
        mWebView.stopLoading();
    }
}