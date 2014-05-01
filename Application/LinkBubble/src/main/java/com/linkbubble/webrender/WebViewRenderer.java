package com.linkbubble.webrender;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.PageInspector;
import com.linkbubble.util.Util;
import com.linkbubble.util.YouTubeEmbedHelper;

class WebViewRenderer extends WebRenderer {

    protected String TAG;
    private Handler mHandler;
    protected WebView mWebView;
    private View mTouchInterceptorView;
    private long mLastWebViewTouchUpTime = -1;
    private String mLastWebViewTouchDownUrl;
    private AlertDialog mJsAlertDialog;
    private AlertDialog mJsConfirmDialog;
    private AlertDialog mJsPromptDialog;
    private PageInspector mPageInspector;
    private int mCheckForEmbedsCount;
    private Boolean mIsDestroyed = false;

    public WebViewRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        mHandler = new Handler();
        TAG = tag;
        mContext = context;
        mDoDropDownCheck = true;

        mWebView = newWebView(context);
        mWebView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(webRendererPlaceholder, mWebView);

        mTouchInterceptorView = new View(context);
        mTouchInterceptorView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        mTouchInterceptorView.setWillNotDraw(true);
        mTouchInterceptorView.setOnTouchListener(mWebViewOnTouchListener);

        ViewGroup parent = (ViewGroup)mWebView.getParent();
        int index = parent.indexOfChild(mWebView);
        parent.addView(mTouchInterceptorView, index+1);

        mWebView.setLongClickable(true);
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setDownloadListener(mDownloadListener);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setTextZoom(Settings.get().getWebViewTextZoom());
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportMultipleWindows(DRM.allowProFeatures() ? true : false);
        webSettings.setGeolocationDatabasePath(Constant.WEBVIEW_DATABASE_LOCATION);
        webSettings.setSavePassword(false);

        String userAgentString = Settings.get().getUserAgentString();
        if (userAgentString != null) {
            webSettings.setUserAgentString(userAgentString);
        }

        mPageInspector = new PageInspector(mContext, mWebView, mOnPageInspectorItemFoundListener);
    }

    protected WebView newWebView(Context context) {
        return new WebView(context);
    }

    @Override
    public void destroy() {
        mIsDestroyed = true;
        mWebView.destroy();
    }

    @Override
    public View getView() {
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
            mWebView.getSettings().setSaveFormData(false);
        } else {
            mWebView.getSettings().setCacheMode(mWebView.getSettings().LOAD_DEFAULT);
            mWebView.getSettings().setAppCacheEnabled(true);

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

    @Override
    public void hidePopups() {
        if (mJsAlertDialog != null) {
            mJsAlertDialog.dismiss();
            mJsAlertDialog = null;
        }
        if (mJsConfirmDialog != null) {
            mJsConfirmDialog.dismiss();
            mJsConfirmDialog = null;
        }
        if (mJsPromptDialog != null) {
            mJsPromptDialog.dismiss();
            mJsPromptDialog = null;
        }
    }

    @Override
    public void onPageLoadComplete() {
        super.onPageLoadComplete();

        mHandler.postDelayed(mDropDownCheckRunnable, Constant.DROP_DOWN_CHECK_TIME);
    }

    @Override
    public void resetPageInspector() {
        mPageInspector.reset();
    }

    @Override
    public void runPageInspector() {
        mPageInspector.run(mWebView, mController.getPageInspectFlags());
    }

    @Override
    public YouTubeEmbedHelper getPageInspectorYouTubeEmbedHelper() {
        return mPageInspector.getYouTubeEmbedHelper();
    }

    PageInspector.OnItemFoundListener mOnPageInspectorItemFoundListener = new PageInspector.OnItemFoundListener() {

        @Override
        public void onYouTubeEmbeds() {
            mController.onPageInspectorYouTubeEmbedFound();
        }

        @Override
        public void onTouchIconLoaded(Bitmap bitmap, String pageUrl) {
            mController.onPageInspectorTouchIconLoaded(bitmap, pageUrl);
        }

        @Override
        public void onDropDownFound() {
            mDoDropDownCheck = false;
        }

        @Override
        public void onDropDownWarningClick() {
            mController.onPageInspectorDropDownWarningClick();
        }
    };

    private boolean mDoDropDownCheck;
    private Runnable mDropDownCheckRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mIsDestroyed) {
                if (mIsDestroyed == false && mDoDropDownCheck) {
                    // Check for YouTube as well to fix issues where sometimes embeds are not found.
                    mPageInspector.run(mWebView, PageInspector.INSPECT_DROP_DOWN | PageInspector.INSPECT_YOUTUBE);
                }
            }
        }
    };

    View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && mIsDestroyed == false) {
                WebView webView = (WebView) v;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK: {
                        return mController.onBackPressed();
                    }
                }
            }

            return false;
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
                    mController.onShowBrowserPrompt();
                    return false;
            }
        }
    };

    private View.OnTouchListener mWebViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastWebViewTouchDownUrl = mUrl.toString();
                    //Log.d(TAG, "[urlstack] WebView - MotionEvent.ACTION_DOWN");
                    break;

                case MotionEvent.ACTION_UP:
                    mLastWebViewTouchUpTime = System.currentTimeMillis();
                    //Log.d(TAG, "[urlstack] WebView - MotionEvent.ACTION_UP");
                    break;
            }
            // Forcibly pass along to the WebView. This ensures we receive the ACTION_UP event above.
            mWebView.onTouchEvent(event);
            return true;
        }
    };

    WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView wView, final String urlAsString) {
            boolean viaInput = false;
            if (mLastWebViewTouchUpTime > -1) {
                long touchUpTimeDelta = System.currentTimeMillis() - mLastWebViewTouchUpTime;
                // this value needs to be largish
                if (touchUpTimeDelta < 1500) {
                    // If the url has changed since the use pressed their finger down, a redirect has likely occurred,
                    // in which case we don't update the Url Stack
                    if (mLastWebViewTouchDownUrl.equals(mUrl.toString())) {
                        viaInput = true;
                    }
                    mLastWebViewTouchUpTime = -1;

                }
            }

            if (viaInput) {
                mDoDropDownCheck = true;
            }

            return mController.shouldOverrideUrlLoading(urlAsString, viaInput);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            mController.onReceivedError();
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public void onPageStarted(WebView view, String urlAsString, Bitmap favIcon) {
            Log.d(TAG, "webViewClientOnPageStarted(): urlAsString:" + urlAsString.substring(0, 28) + ", getUrl():" + getUrl().toString());
            mDoDropDownCheck = true;
            mController.onPageStarted(urlAsString, favIcon);
        }

        @Override
        public void onPageFinished(WebView webView, String urlAsString) {
            mController.onPageFinished(urlAsString);
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


    WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onReceivedTitle(WebView webView, String title) {
            mController.onReceivedTitle(webView.getUrl(), title);
        }

        @Override
        public void onReceivedIcon(WebView webView, Bitmap bitmap) {
            mController.onReceivedIcon(bitmap);
        }

        @Override
        public void onProgressChanged(WebView webView, int progress) {
            webChromeClientOnProgressChanged(webView, progress);
        }

        @Override
        public void onCloseWindow(WebView window) {
            mController.onCloseWindow();
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            mJsAlertDialog = new AlertDialog.Builder(mContext).create();
            mJsAlertDialog.setMessage(message);
            mJsAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mJsAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(R.string.action_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }

                    });
            mJsAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mJsAlertDialog = null;
                }
            });
            mJsAlertDialog.show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            mJsConfirmDialog = new AlertDialog.Builder(mContext).create();
            mJsConfirmDialog.setTitle(R.string.confirm_title);
            mJsConfirmDialog.setMessage(message);
            mJsConfirmDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mJsConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
            mJsConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE, mContext.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            });
            mJsConfirmDialog.show();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
            final View v = LayoutInflater.from(mContext).inflate(R.layout.view_javascript_prompt, null);

            ((TextView)v.findViewById(R.id.prompt_message_text)).setText(message);
            ((EditText)v.findViewById(R.id.prompt_input_field)).setText(defaultValue);

            mJsPromptDialog = new AlertDialog.Builder(mContext).create();
            mJsPromptDialog.setView(v);
            mJsPromptDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mJsPromptDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = ((EditText)v.findViewById(R.id.prompt_input_field)).getText().toString();
                    result.confirm(value);
                }
            });
            mJsPromptDialog.setButton(AlertDialog.BUTTON_NEGATIVE, mContext.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            });
            mJsPromptDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    result.cancel();
                }
            });
            mJsPromptDialog.show();

            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            // Call the old version of this function for backwards compatability.
            //onConsoleMessage(consoleMessage.message(), consoleMessage.lineNumber(),
            //        consoleMessage.sourceId());
            Log.d("Console", consoleMessage.message());
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
            mController.onGeolocationPermissionsShowPrompt(origin, new GetGeolocationCallback() {
                @Override
                public void onAllow() {
                    callback.invoke(origin, true, false);
                }
            });
        }
    };

    protected void webChromeClientOnProgressChanged(WebView webView, int progress) {
        mController.onProgressChanged(progress, webView.getUrl());

        // At 60%, the page is more often largely viewable, but waiting for background shite to finish which can
        // take many, many seconds, even on a strong connection. Thus, do a check for embeds now to prevent the button
        // not being updated until 100% is reached, which feels too slow as a user.
        if (progress >= 60) {
            if (mCheckForEmbedsCount == 0) {
                mCheckForEmbedsCount = 1;
                mPageInspector.reset();

                Log.d(TAG, "onProgressChanged() - checkForYouTubeEmbeds() - progress:" + progress + ", mCheckForEmbedsCount:" + mCheckForEmbedsCount);
                mPageInspector.run(webView, mController.getPageInspectFlags());
            } else if (mCheckForEmbedsCount == 1 && progress >= 80) {
                mCheckForEmbedsCount = 2;
                Log.d(TAG, "onProgressChanged() - checkForYouTubeEmbeds() - progress:" + progress + ", mCheckForEmbedsCount:" + mCheckForEmbedsCount);
                mPageInspector.run(webView, mController.getPageInspectFlags());
            }
        }
    }
}