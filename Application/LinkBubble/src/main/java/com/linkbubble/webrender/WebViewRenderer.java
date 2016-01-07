package com.linkbubble.webrender;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.articlerender.ArticleContent;
import com.linkbubble.ui.TabView;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.NetworkConnectivity;
import com.linkbubble.util.NetworkReceiver;
import com.linkbubble.util.PageInspector;
import com.linkbubble.util.Util;
import com.linkbubble.util.YouTubeEmbedHelper;
import com.squareup.otto.Subscribe;

import java.net.URL;
import java.util.Map;

class WebViewRenderer extends WebRenderer {


    protected String TAG;
    private Handler mHandler;
    protected WebView mWebView;
    private View mTouchInterceptorView;
    private long mLastWebViewTouchUpTime = -1;
    private String mLastWebViewTouchDownUrl;
    private String mHost;
    private AlertDialog mJsAlertDialog;
    private AlertDialog mJsConfirmDialog;
    private AlertDialog mJsPromptDialog;
    private PageInspector mPageInspector;
    private int mCheckForEmbedsCount;
    private int mRunPageScripts = 0;
    private int mCurrentProgress;
    private boolean mPauseOnComplete;
    private Boolean mIsDestroyed = false;
    private boolean mRegisteredForBus;
    private boolean mTrackingProtectionEnabled = false;
    private boolean mAdblockEnabled = false;

    private ArticleContent.BuildContentTask mBuildArticleContentTask;
    private ArticleContent mArticleContent;

    private static NetworkReceiver mLastNetworkReceiver = null;

    public WebViewRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        mHandler = new Handler();
        TAG = tag;

        mWebView = new WebView(mContext);
        mWebView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(webRendererPlaceholder, mWebView);

        mTouchInterceptorView = new View(mContext);
        mTouchInterceptorView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        mTouchInterceptorView.setWillNotDraw(true);
        mTouchInterceptorView.setOnTouchListener(mWebViewOnTouchListener);

        ViewGroup parent = (ViewGroup)mWebView.getParent();
        int index = parent.indexOfChild(mWebView);
        parent.addView(mTouchInterceptorView, index + 1);

        mWebView.setLongClickable(true);
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setDownloadListener(mDownloadListener);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setTextZoom(Settings.get().getWebViewTextZoom());
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setGeolocationDatabasePath(Constant.WEBVIEW_DATABASE_LOCATION);
        webSettings.setSavePassword(false);

        String userAgentString = Settings.get().getUserAgentString();
        if (userAgentString != null) {
            webSettings.setUserAgentString(userAgentString);
        }

        mPageInspector = new PageInspector(mContext, mWebView, mOnPageInspectorItemFoundListener);

        MainApplication.registerForBus(context, this);
        mRegisteredForBus = true;
    }

    @Override
    public void destroy() {
        if (mRegisteredForBus) {
            MainApplication.unregisterForBus(mContext, this);
            mRegisteredForBus = false;
        }
        cancelBuildArticleContentTask();
        mIsDestroyed = true;
        mWebView.destroy();
        Log.d("Article", "WebViewRenderer.destroy()");
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

    private void cancelBuildArticleContentTask() {
        if (mBuildArticleContentTask != null) {
            mBuildArticleContentTask.cancel(true);
            Log.d("Article", "BuildContentTask().cancel()");
            mBuildArticleContentTask = null;
        }
    }

    @Override
    public String getUserAgentString(Context context) {
        if (mWebView.getSettings() == null) {
            return Util.getDefaultUserAgentString(context);
        }
        return mWebView.getSettings().getUserAgentString();
    }

    @Override
    public void setUserAgentString(String userAgentString) {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setUserAgentString(userAgentString);
    }

    @Override
    public void loadUrl(URL url, Mode mode) {
        mHost = url.getHost();
        if (mHost.startsWith("www.")) {
            mHost = mHost.substring(4);
        }
        mTrackingProtectionEnabled = Settings.get().isTrackingProtectionEnabled();
        mAdblockEnabled = Settings.get().isAdBlockEnabled();

        String urlAsString = url.toString();
        Log.d(TAG, "loadUrl() - " + urlAsString);

        cancelBuildArticleContentTask();
        mArticleContent = null;

        mMode = mode;

        switch (mMode) {

            case Article:
                //mGetArticleContentTask = new GetArticleContentTask();
                //mGetArticleContentTask.execute(urlAsString);

                // This is only called by Snacktory renderer so that the loading animations start at the point the page HTML commences.
                // Not needed for other Renderers given onPageStarted() will be called.
                mController.onLoadUrl(urlAsString);
                break;

            case Web:
                mWebView.loadUrl(url.toString());
                break;
        }
    }

    @Override
    public void reload() {
        switch (mMode) {
            case Article:
                loadUrl(getUrl(), mMode);
                break;

            case Web:
                // In case the user changes adblock / TP settings and reloads the current bubble
                mTrackingProtectionEnabled = Settings.get().isTrackingProtectionEnabled();
                mAdblockEnabled = Settings.get().isAdBlockEnabled();
                mWebView.reload();
                break;
        }
    }

    @Override
    public void stopLoading() {
        cancelBuildArticleContentTask();
        mArticleContent = null;

        if (null != mWebView) {
            mWebView.stopLoading();

            // Ensure the loading indicators cease when stop is pressed.
            mWebChromeClient.onProgressChanged(mWebView, 100);
        }
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
    }

    @Override
    public void resetPageInspector() {
        mPageInspector.reset();
    }

    @Override
    public void runPageInspector() {
        mPageInspector.run(mWebView);
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
        public void onFetchHtml(String html) {
            if (html != null && html.isEmpty() == false) {
                if (mBuildArticleContentTask == null) {
                    mBuildArticleContentTask = ArticleContent.fetchArticleContent(getUrl().toString(), html,
                            new ArticleContent.OnFinishedListener() {
                                @Override
                                public void onFinished(ArticleContent articleContent) {
                                    mArticleContent = articleContent;
                                    mController.onArticleContentReady(mArticleContent);
                                    mBuildArticleContentTask = null;
                                }
                            }
                    );
                }
            }
        }

        @Override
        public void onThemeColor(int color) {
            mController.onPagedInspectorThemeColorFound(color);
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
            Log.d(TAG, "onLongClick type: " + hitTestResult.getType());
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
                        Message msg = new Message();
                        msg.setTarget(new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                Bundle b = msg.getData();
                                if (b != null && b.getString("url") != null) {
                                    mController.onShowBrowserPrompt();
                                }
                            }
                        });
                        mWebView.requestFocusNodeHref(msg);
                    }
                    return true;
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

            return mController.shouldOverrideUrlLoading(urlAsString, viaInput);
        }

        private WebResourceResponse interceptTheCall(WebView view, String urlStr, String filterOption, boolean apiLevelAbove21) {
            // null signifies allowing the request
            WebResourceResponse allowRequest = null;

            // Quickly check to see if no checks are needed because ad blocking and tracking
            // protection are not enabled.
            if (!mTrackingProtectionEnabled && !mAdblockEnabled) {
                return allowRequest;
            }

            String host;
            try {
                host = new URL(urlStr).getHost();
            } catch (Exception e) {
                return allowRequest;
            }

            // Never block for hosts which are not third party to the baseHost
            if (host.length() == mHost.length() && host.equalsIgnoreCase(mHost)) {
                return null;
            } else if(host.length() > mHost.length() &&
                    host.charAt(host.length() - mHost.length() - 1) == '.' &&
                    host.substring(host.length() - mHost.length()).equalsIgnoreCase(mHost)) {
                return allowRequest;
            }

            if (mTrackingProtectionEnabled &&
                    mController.shouldTrackingProtectionBlockUrl(mHost, host) ||
                    mAdblockEnabled && mController.shouldAdBlockUrl(mHost, urlStr, filterOption)) {
                // Unfortunately the deprecated API that we're targetting doesn't have a better
                // way to block this. Once we upgrade our target then we can use a better override
                // which allows us to set a response code.
                if (!apiLevelAbove21) {
                    return new WebResourceResponse("text/html", "UTF-8", null);
                }
                else {
                    return new WebResourceResponse("text/html", "UTF-8", 150, "Blocked", null, null);
                }
            }

            return allowRequest;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest (WebView view, String urlStr) {
            // That call is for the API level is lower then 21

            // Just return as is for now, we will have a solution for older devices later
            return null;

            // Quickly check to see if no checks are needed because ad blocking and tracking
            // protection are not enabled.
            /*if (!mTrackingProtectionEnabled && !mAdblockEnabled) {
                return null;
            }

            String filterOption = "none";*/

            /*HttpURLConnection connection;
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(3000);
                connection.connect();
                // get the size of the file which is in the header of the request
                int size = connection.getContentLength();
                if (-1 != size) {
                    InputStream inputStream = connection.getInputStream();
                    byte[] buffer = new byte[size];
                    inputStream.read(buffer);
                    String str = new String(buffer);
                    if (null != str) {
                        if (str.contains("Accept")) {

                        }
                    }
                }
            }
            catch (Exception e) {
                // Do nothing here
            }*/

            /*try {
                URL url = new URL(urlStr);
                String urlPath = url.getPath();
                if (urlPath.endsWith("css") || urlStr.endsWith(".css") || urlStr.endsWith(".woff")) {
                    filterOption = "/css";
                }
                else if (urlStr.endsWith(".png") || urlStr.endsWith(".ico") || urlStr.endsWith(".gif")
                        || urlStr.endsWith(".svg") || urlStr.endsWith(".icns") || urlStr.endsWith(".bmp")
                        || urlStr.endsWith(".pdf") || urlStr.endsWith(".pcd") || urlStr.endsWith(".fpx")
                        || urlStr.endsWith(".jp2") || urlStr.endsWith(".jpx") || urlStr.endsWith(".j2k")
                        || urlStr.endsWith(".j2c") || urlStr.endsWith(".jpeg") || urlStr.endsWith(".jpg")
                        || urlStr.endsWith(".jif") || urlStr.endsWith(".jfif") || urlStr.endsWith(".bmp")
                        || urlStr.endsWith(".tif") || urlStr.endsWith(".tiff")) {
                    filterOption = "image/";
                }
                else if (urlStr.endsWith(".js") || urlPath.endsWith("js")) {
                    filterOption = "javascript";
                }
            } catch (Exception e) {
                // Do nothing here
            }

            return interceptTheCall(view, urlStr, filterOption, false);*/
        }

        @Override
        public WebResourceResponse shouldInterceptRequest (WebView view, WebResourceRequest resourceRequest) {
            // That call is for the API level is higher or equal to 21

            // Quickly check to see if no checks are needed because ad blocking and tracking
            // protection are not enabled.
            if (!mTrackingProtectionEnabled && !mAdblockEnabled) {
                return null;
            }

            String filterOption = "none";
            Map<String, String> requestHeaders = resourceRequest.getRequestHeaders();
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                if (entry.getKey().equals("Accept")) {
                    if (entry.getValue().contains("/css")) {
                        filterOption = "/css";
                        break;
                    }
                    else if (entry.getValue().contains("image/")) {
                        filterOption = "image/";
                        break;
                    }
                    else if (entry.getValue().contains("javascript")) {
                        filterOption = "javascript";
                        break;
                    }
                }
            }

            return interceptTheCall(view, resourceRequest.getUrl().toString(), filterOption, true);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "WebViewRenderer - onReceivedError() - " + description + " - " + failingUrl);

            // Reload webviews once we have a connection.
            if (NetworkConnectivity.isConnected(mContext) == false) {
                Log.d(TAG, "Not connected, will retry on connection.");

                // We only reload a single webview at a time, so if there is a previous receiver, we unregister it.
                if (mLastNetworkReceiver != null) {
                    try {
                        mContext.unregisterReceiver(mLastNetworkReceiver);
                    } catch(Exception e) {
                        Log.d(TAG, "Could not unregister existing network receiver.");
                    }
                    mLastNetworkReceiver = null;
                }

                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                NetworkReceiver receiver = new NetworkReceiver(WebViewRenderer.this);
                mContext.registerReceiver(receiver, filter);
                mLastNetworkReceiver = receiver;
            }
            mController.onReceivedError();
        }

        @Override
        public void onReceivedSslError(WebView webView, final SslErrorHandler handler, SslError error) {
            handler.cancel();
            /*
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(mContext.getString(R.string.warning));
            String s = error.toString();
            Log.d("blerg", s);
            URL url;
            try {
                 url = new URL(error.getUrl());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                url = mUrl;
            }
            builder.setMessage(String.format(mContext.getString(R.string.untrusted_certificate), url.getHost()))
                    .setCancelable(true)
                    .setPositiveButton(mContext.getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    handler.proceed();
                                }
                            })
                    .setNegativeButton(mContext.getString(R.string.action_no_recommended),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    handler.cancel();
                                }
                            });
            if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
                AlertDialog alert = builder.create();
                alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                Util.showThemedDialog(alert);
            } else {
                handler.proceed();
            }*/
        }

        @Override
        public void onPageStarted(WebView view, String urlAsString, Bitmap favIcon) {
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
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            MainController.get().startFileBrowser(fileChooserParams.getAcceptTypes(), filePathCallback);
            return true;
        }

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
            mCurrentProgress = progress;
            mController.onProgressChanged(progress, webView.getUrl());

            // Inject page scripts after there has been some progress, otherwise they get injected into an empty page.
            if (mCurrentProgress >= 60 && mRunPageScripts == 0) {
                mRunPageScripts = 1;
                mPageInspector.run(webView);
            }

            if (mCurrentProgress == 100 && mPauseOnComplete) {
                mHandler.postDelayed(mCheckForPauseRunnable, 3000);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            mController.onCloseWindow();
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            mJsAlertDialog = new AlertDialog.Builder(mContext).create();
            mJsAlertDialog.setMessage(message);
            mJsAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mJsAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(R.string.action_ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            result.confirm();
                        }

                    });
            mJsAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    result.cancel();
                }
            });
            mJsAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mJsAlertDialog = null;
                }
            });
            Util.showThemedDialog(mJsAlertDialog);
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
            mJsConfirmDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mJsConfirmDialog = null;
                }
            });
            Util.showThemedDialog(mJsConfirmDialog);
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
            mJsPromptDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mJsPromptDialog = null;
                }
            });
            Util.showThemedDialog(mJsPromptDialog);

            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            // Call the old version of this function for backwards compatability.
            //onConsoleMessage(consoleMessage.message(), consoleMessage.lineNumber(),
            //        consoleMessage.sourceId());
            String message = consoleMessage.message();
            if (message == null) {
                message = "(null)";
            }
            Log.e("Console", message);

            return false;
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            TabView tabView = MainController.get().openUrl(Constant.NEW_TAB_URL, System.currentTimeMillis(), false, Analytics.OPENED_URL_FROM_NEW_WINDOW);
            if (tabView != null) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView((WebView) tabView.getContentView().getWebRenderer().getView());
                resultMsg.sendToTarget();
                return true;
            }

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

    @Override
    public ArticleContent getArticleContent() {
        return mArticleContent;
    }

    private static final String BATTERY_SAVE_TAG = "BatterySaveWebView";

    private Runnable mCheckForPauseRunnable = new Runnable() {
        @Override
        public void run() {
            switch (Settings.get().getWebViewBatterySaveMode()) {
                case Default:
                case Aggressive:
                    if (mPauseOnComplete) {
                        mPauseOnComplete = false;
                        webviewPause("runnable");
                    }
                    break;
            }
        }
    };

    private void webviewPause(String via) {
        String msg = "PAUSE (" + via + ") ";
        if (mWebView != null && mIsDestroyed == false) {
            if (mCurrentProgress == 100) {
                mWebView.onPause();
                mPauseOnComplete = false;
            } else {
                msg += " **IGNORE** (" + mCurrentProgress + ")";
                mPauseOnComplete = true;
            }

        }
        Log.d(BATTERY_SAVE_TAG, msg + ", url:" + getUrl().getHost());
    }

    private void webviewResume(String via) {
        mPauseOnComplete = false;
        String msg = "RESUME (" + via + ") ";
        if (mWebView != null && mIsDestroyed == false) {
            mWebView.onResume();
        }
        Log.d(BATTERY_SAVE_TAG, msg + ", url:" + getUrl().getHost());
    }

    @Override
    public void resumeOnSetActive() {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Aggressive:
                webviewResume("setActive");
                break;
        }
    }

    @Override
    public void pauseOnSetInactive() {
        switch (Settings.get().getWebViewBatterySaveMode()) {
            case Aggressive:
                webviewPause("setInactive");
                break;
        }
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
        // Do nothing here for now as we Resume current active Tab on resumeOnSetActive
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
