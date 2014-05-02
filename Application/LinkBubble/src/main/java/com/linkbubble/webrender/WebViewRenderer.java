package com.linkbubble.webrender;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.AsyncTask;
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
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

class WebViewRenderer extends WebRenderer {

    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MMM dd, yyyy");
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

    private GetArticleContentTask mGetArticleContentTask;

    public WebViewRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        mHandler = new Handler();
        TAG = tag;
        mContext = context;
        mDoDropDownCheck = true;

        mWebView = new WebView(context);
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
    public void loadUrl(URL url, Mode mode) {
        String urlAsString = url.toString();
        Log.d(TAG, "loadUrl() - " + urlAsString);

        if (mGetArticleContentTask != null) {
            mGetArticleContentTask.cancel(true);
        }

        mMode = mode;

        switch (mMode) {

            case Article:
                mGetArticleContentTask = new GetArticleContentTask();
                mGetArticleContentTask.execute(urlAsString);

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
                mWebView.reload();
                break;
        }
    }

    @Override
    public void stopLoading() {
        if (mGetArticleContentTask != null) {
            mGetArticleContentTask.cancel(true);
        }

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


    // **Broken links
    //
    // [nothing displays]:
    //  * http://www.bostonglobe.com/sports/2014/04/28/the-donald-sterling-profile-not-pretty-picture/jZx4v3EWUFdLYh9c289ODL/story.html

    private class GetArticleContentTask extends AsyncTask<String, JResult, JResult> {
        protected JResult doInBackground(String... urls) {

            JResult result = null;
            String url = urls[0];
            try {
                Log.d(TAG, "GetArticleContentTask().doInBackground(): url:" + url);
                HtmlFetcher fetcher = new HtmlFetcher();
                result = fetcher.fetchAndExtract(url, 30 * 1000, true);
                //String text = result.getText();
                //String title = result.getTitle();
                //String imageUrl = result.getImageUrl();
                //Log.d(TAG, "title: " + title + ", text: " + text + ", imageUrl:" + imageUrl);
            } catch (Exception ex) {
                Log.d(TAG, ex.getLocalizedMessage(), ex);
            }
            return result;
        }

        protected void onPostExecute(JResult result) {
            String urlAsString = result.getCanonicalUrl();
            if (urlAsString == null) {
                urlAsString = result.getUrl();
            }
            URL url = null;
            try {
                setUrl(urlAsString);
                url = getUrl();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String text = result.getText();
            if (text.isEmpty()) {
                Log.d(TAG, "No text found for - forcing to Web mode");
                loadUrl(url, Mode.Web);
                return;
            }

            String headHtml =
                    "  <head>\n" +
                            "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0, height=device-height\"/>\n" +
                            "    <link href='http://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>\n" +
                            "    <style type=\"text/css\">\n" +
                            "      p, div { font-family: 'Roboto', sans-serif; font-size: 16px; color:#333; line-height: 160%; }\n" +
                            "      a { text-decoration: none; }\n" +
                            "      #lbInfo { width:100%; min-height:28px; margin:0 auto; padding-bottom: 20px;}\n" +
                            "      #lbInfoL { float:left; width:70%; }\n" +
                            "      #lbInfoR { float:right; width:30%; }\n" +
                            "    </style>" +
                            "    </style>";

            String bodyHtml = "<body >\n" +
                    "    <div style=\"margin:0px 24px 0px 24px\">\n";

            String title = result.getTitle();
            if (title != null) {
                headHtml += "<title>" + title + "</title>";
                bodyHtml += "<p style=\"font-size:150%;line-height:120%;font-weight:bold;margin:32px 0px 12px 0px\">" + title + "</p>";
            }

            String authorName = result.getAuthorName();
            Date publishedDate = result.getDate();

            String leftString = "";
            String rightString = "";

            if (authorName != null) {
                leftString = "<span class=\"nowrap\">by <b>" + authorName + "</b>,</span> ";
            }
            if (url != null) {
                leftString += "<span class=\"nowrap\"><a href=\"" + url.getProtocol() + "://" + url.getHost() + "\">" + (url.getHost().replace("www.", "")) + "</a></span>";
            }

            Log.d("info", "urlHost:" + url.getHost() + ", authorName: " + authorName);

            if (publishedDate != null) {
                rightString = "<span style=\"float:right\">" + sDateFormat.format(publishedDate) + "</span>";
            }

            bodyHtml += "<hr style=\"border: 0;height: 0; border-top: 1px solid rgba(0, 0, 0, 0.1); border-bottom: 1px solid rgba(255, 255, 255, 0.3);\">"
                    + "<div id=\"lbInfo\"><div id=\"lbInfoL\">" + leftString + "</div><div id=\"lbInfoR\">" + rightString + "</div></div>";

            String html = result.getHtml();
            if (html != null) {
                bodyHtml += html;
            }

            headHtml += "</head>";
            bodyHtml += " </div>\n" +
                    "    </div>\n" +
                    "    <br><br><br>" +
                    "  </body>\n";

            //mWebView.loadUrl(urlAsString);
            //mWebView.stopLoading();

            String pageHtml = "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + headHtml + bodyHtml + "</html>";

            //Log.d(TAG, "pageHtml:" + pageHtml);
            mWebView.loadDataWithBaseURL(urlAsString, pageHtml, "text/html", "utf-8", urlAsString);
            //mWebView.loadData(pageHtml, "text/html", "utf-8");

            if (title != null) {
                mController.onReceivedTitle(urlAsString, title);
            }
            mController.onProgressChanged(100, urlAsString);
            //mController.onPageFinished(urlAsString);
        }
    }
}