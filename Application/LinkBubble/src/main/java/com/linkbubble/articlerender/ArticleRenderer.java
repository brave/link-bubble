package com.linkbubble.articlerender;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.linkbubble.Constant;
import com.linkbubble.util.Util;


public class ArticleRenderer {

    public interface Controller {
        public void onUrlLongClick(String url);
        public void onDownloadStart(String urlAsString);
        public boolean onBackPressed();
        public void onShowBrowserPrompt();
        public void onFirstPageLoadStarted();
    }
    
    private WebView mWebView;
    private boolean mIsDestroyed = false;
    private boolean mFirstPageLoadTriggered = false;
    private Controller mController;

    public ArticleRenderer(Context context, Controller controller, ArticleContent articleContent, View articleRendererPlaceholder) {
        mController = controller;

        mWebView = new WebView(context);
        mWebView.setLayoutParams(articleRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(articleRendererPlaceholder, mWebView);

        mWebView.setDownloadListener(mDownloadListener);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);

        mWebView.setWebViewClient(mWebViewClient);

        display(articleContent, false);

        Log.d("Article", "ArticleRenderer()");
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
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                    final String url = hitTestResult.getExtra();
                    if (url == null) {
                        return false;
                    }

                    mController.onUrlLongClick(url);
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
                        return mController.onBackPressed();
                    }
                }
            }

            return false;
        }
    };

}
