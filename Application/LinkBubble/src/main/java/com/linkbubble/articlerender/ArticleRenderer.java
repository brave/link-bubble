/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.articlerender;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;
import com.linkbubble.webrender.CustomWebView;
import com.squareup.otto.Subscribe;


public class ArticleRenderer {

    public interface Controller {
        public void onUrlLongClick(WebView webView, String url, int type);
        public void onDownloadStart(String urlAsString);
        public boolean onBackPressed();
        public void onShowBrowserPrompt();
        public void onFirstPageLoadStarted();
        public void onWebViewContextMenuAppearedGone(boolean appeared);
        public void resetBubblePanelAdjustment();
        public void adjustBubblesPanel(int newY, int oldY, boolean afterTouchAdjust);
    }

    private Context mContext;
    private CustomWebView mWebView;
    private boolean mIsDestroyed = false;
    private boolean mFirstPageLoadTriggered = false;
    private Controller mController;
    private boolean mRegisteredForBus;

    public ArticleRenderer(Context context, Controller controller, ArticleContent articleContent, View articleRendererPlaceholder) {
        mContext = context;
        mController = controller;
        Log.e(BATTERY_SAVE_TAG, "create: " + this.getClass().getSimpleName());

        mWebView = new CustomWebView(context);
        mWebView.setLayoutParams(articleRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(articleRendererPlaceholder, mWebView);

        mWebView.setDownloadListener(mDownloadListener);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);

        mWebView.setWebViewClient(mWebViewClient);

        mWebView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                if (!mWebView.mCopyPasteContextMenuCreated) {
                    mWebView.mCopyPasteContextMenuCreated = true;
                    mController.onWebViewContextMenuAppearedGone(true);
                }
            }
        });
        mWebView.setOnScrollChangedCallback(mOnScrollChangedCallback);
        mWebView.setOnTouchListener(mRealWebViewOnTouchListener);

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

    public boolean isCopyPasteShown() {
        return mWebView.mCopyPasteContextMenuCreated;
    }

    public void copyPasteDialogWasDestroyed() {
        if (null != mController && mWebView.mCopyPasteContextMenuCreated) {
            mWebView.mCopyPasteContextMenuCreated = false;
            mController.onWebViewContextMenuAppearedGone(false);
        }
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
                    //if (Constant.ACTIVITY_WEBVIEW_RENDERING == false) {
                    //    mController.onShowBrowserPrompt();
                    //}
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

    CustomWebView.OnScrollChangedCallback mOnScrollChangedCallback = new CustomWebView.OnScrollChangedCallback() {
        @Override
        public void onScroll(int newY, int oldY) {
            if (!mWebView.mInterceptScrollChangeCalls && 0 == newY) {
                mController.resetBubblePanelAdjustment();
            }
            else {
                mController.adjustBubblesPanel(newY, oldY, false);
            }
        }
    };

    private View.OnTouchListener mRealWebViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mWebView.mInterceptScrollChangeCalls = true;
                    break;

                case MotionEvent.ACTION_UP:
                    mWebView.mInterceptScrollChangeCalls = false;
                    mController.adjustBubblesPanel(0, 0, true);
                    break;
            }
            mWebView.onTouchEvent(event);

            return true;
        }
    };
}
