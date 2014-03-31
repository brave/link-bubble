package com.linkbubble.webrender;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
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
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    private Context mContext;
    private WebView mWebView;
    private View mTouchInterceptorView;
    private long mLastWebViewTouchUpTime = -1;
    private String mLastWebViewTouchDownUrl;
    private Controller mController;
    private AlertDialog mJsAlertDialog;
    private AlertDialog mJsConfirmDialog;
    private AlertDialog mJsPromptDialog;


    public WebViewRenderer(Context context, Controller controller, View webRendererPlaceholder) {
        super(context, controller, webRendererPlaceholder);

        mContext = context;
        mController = controller;

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
            mController.onProgressChanged(webView, progress);
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
}