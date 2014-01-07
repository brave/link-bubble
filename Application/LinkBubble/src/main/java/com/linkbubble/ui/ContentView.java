package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.graphics.Canvas;
import android.widget.Toast;
import com.linkbubble.util.ActionItem;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.YouTubeEmbedHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by gw on 19/08/13.
 */
public class ContentView extends FrameLayout {

    private static final String TAG = "UrlLoad";

    private WebView mWebView;
    private CondensedTextView mTitleTextView;
    private CondensedTextView mUrlTextView;
    private ContentViewButton mShareButton;
    private OpenInAppButton mOpenInAppButton;
    private OpenEmbedButton mOpenEmbedButton;
    private ContentViewButton mOverflowButton;
    private LinearLayout mToolbarLayout;
    private EventHandler mEventHandler;
    private Context mContext;
    private String mUrl;
    private List<AppForUrl> mAppsForUrl = new ArrayList<AppForUrl>();
    private List<ResolveInfo> mTempAppsForUrl = new ArrayList<ResolveInfo>();
    private URL mTempUrl;
    private YouTubeEmbedHelper mYouTubeEmbedHelper;
    private int mCheckForEmbedsCount;
    private PopupMenu mOverflowPopupMenu;
    private AlertDialog mLongPressAlertDialog;
    private long mStartTime;
    private BubbleView mOwner;
    private int mHeaderHeight;
    private int mMarkerX;
    private Path mTempPath = new Path();

    private Paint mPaint;

    private static final String JS_VARIABLE = "LinkBubble";
    private static final String JS_EMBED = "javascript:(function() {\n" +
            "    var elems = document.getElementsByTagName('*'), i;\n" +
            "    var resultArray = null;\n" +
            "    var resultCount = 0;\n" +
            "    for (i in elems) {\n" +
            "       var elem = elems[i];\n" +
            "       if (elem.src != null && elem.src.indexOf(\"" + Config.YOUTUBE_EMBED_PREFIX + "\") != -1) {\n" +
                //"           //console.log(\"found embed: \" + elem.src);\n" +
            "           if (resultArray == null) {\n" +
            "               resultArray = new Array();\n" +
                //"           console.log(\"allocate array\");\n" +
            "           }\n" +
            "           resultArray[resultCount] = elem.src;\n" +
            "           resultCount++;\n" +
            "       }\n" +
            "    }\n" +
            "    if (resultCount > 0) {\n" +
            "       " + JS_VARIABLE + ".onEmbeds(resultArray.toString());\n" +
            "    }\n" +
            "})();";

    public ContentView(Context context) {
        this(context, null);
    }

    public ContentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    static class AppForUrl {
        ResolveInfo mResolveInfo;
        URL mUrl;
        Drawable mIcon;

        AppForUrl(ResolveInfo resolveInfo, URL url) {
            mResolveInfo = resolveInfo;
            mUrl = url;
        }

        Drawable getIcon(Context context) {
            if (mIcon == null) {
                // TODO: Handle OutOfMemory error
                mIcon = mResolveInfo.loadIcon(context.getPackageManager());
            }

            return mIcon;
        }
    };

    public static class PageLoadInfo {
        Bitmap bmp;
        String url;
        String mHost;
        String title;
    }

    public interface EventHandler {
        public void onSharedLink();
        public void onPageLoading(String url);
        public void onProgressChanged(int progress);
        public void onPageLoaded(PageLoadInfo info);
        public void onReceivedIcon(Bitmap bitmap);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        mTempPath.reset();
        float mCenterX = mMarkerX + Config.mBubbleWidth * 0.5f;
        mTempPath.moveTo(mCenterX - mHeaderHeight, mHeaderHeight + 1.0f);
        mTempPath.lineTo(mCenterX, 0.0f);
        mTempPath.lineTo(mCenterX + mHeaderHeight, mHeaderHeight + 1.0f);

        canvas.drawPath(mTempPath, mPaint);
    }

    public void destroy() {
        removeView(mWebView);
        mWebView.destroy();
    }

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

    private boolean isValidUrl(String urlString) {
        try {
            return isValidUrl(new URL(urlString));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean isValidUrl(URL url) {
        if (url == null) {
            return false;
        }

        boolean isValid = true;

        String [] urlBlacklist = { "t.co", "goo.gl", "bit.ly" };

        String hostName = url.getHost();

        for (int i=0 ; i < urlBlacklist.length ; ++i) {
            if (hostName.equalsIgnoreCase(urlBlacklist[i])) {
                isValid = false;
                break;
            }
        }

        return isValid;
    }

    private void showSelectShareMethod(final String url, final boolean closeBubbleOnShare) {

        AlertDialog alertDialog = ActionItem.getShareAlert(mContext, new ActionItem.OnActionItemSelectedListener() {
            @Override
            public void onSelected(ActionItem actionItem) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setClassName(actionItem.mPackageName, actionItem.mActivityClassName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_TEXT, url);
                mContext.startActivity(intent);

                if (closeBubbleOnShare) {
                    mEventHandler.onSharedLink();
                }
            }
        });
        alertDialog.show();
    }

    public void setMarkerX(int x) {
        mMarkerX = x;
        invalidate();
    }

    void configure(BubbleView owner, String url, long startTime, EventHandler eh) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(getResources().getColor(R.color.content_toolbar_background));

        try {
            mTempUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        mHeaderHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_header);

        mWebView = (WebView) findViewById(R.id.webView);
        mToolbarLayout = (LinearLayout) findViewById(R.id.content_toolbar);
        mTitleTextView = (CondensedTextView) findViewById(R.id.title_text);
        mUrlTextView = (CondensedTextView) findViewById(R.id.url_text);

        View textContainer = findViewById(R.id.content_text_container);
        textContainer.setOnTouchListener(new OnSwipeTouchListener() {
            public void onSwipeRight() {
                MainController.get().showPreviousBubble();
            }
            public void onSwipeLeft() {
                MainController.get().showNextBubble();
            }
        });

        mShareButton = (ContentViewButton)findViewById(R.id.share_button);
        mShareButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_share));
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectShareMethod(mUrl, true);
            }
        });

        mOpenInAppButton = (OpenInAppButton)findViewById(R.id.open_in_app_button);
        mOpenInAppButton.setOnOpenInAppClickListener(new OpenInAppButton.OnOpenInAppClickListener() {

            @Override
            public void onAppOpened() {
                mEventHandler.onSharedLink();
            }

        });

        mOpenEmbedButton = (OpenEmbedButton)findViewById(R.id.open_embed_button);
        mOpenEmbedButton.setOnOpenEmbedClickListener(new OpenEmbedButton.OnOpenEmbedClickListener() {

            @Override
            public void onYouTubeEmbedOpened() {

            }
        });

        mOverflowButton = (ContentViewButton)mToolbarLayout.findViewById(R.id.overflow_button);
        mOverflowButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_overflow));
        mOverflowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOverflowPopupMenu = new PopupMenu(mContext, mOverflowButton);
                Resources resources = mContext.getResources();
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_upgrade_to_pro, Menu.NONE,
                        resources.getString(R.string.action_upgrade_to_pro));
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_reload_page, Menu.NONE,
                        resources.getString(R.string.action_reload_page));
                String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
                if (defaultBrowserLabel != null) {
                    mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_open_in_browser, Menu.NONE,
                            String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel));
                }
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_settings, Menu.NONE,
                        resources.getString(R.string.action_settings));
                mOverflowPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.item_upgrade_to_pro: {
                                Intent intent = Config.getStoreIntent(mContext, Config.STORE_PRO_URL);
                                if (intent != null) {
                                    mContext.startActivity(intent);
                                    mEventHandler.onSharedLink();
                                }
                                break;
                            }

                            case R.id.item_reload_page: {
                                if (mYouTubeEmbedHelper != null) {
                                    mYouTubeEmbedHelper.clear();
                                }
                                mEventHandler.onPageLoading(mUrl);
                                mWebView.stopLoading();
                                mWebView.reload();
                                updateAppsForUrl(mUrl);
                                configureOpenInAppButton();
                                configureOpenEmbedButton();
                                Log.d(TAG, "reload url: " + mUrl);
                                mStartTime = System.currentTimeMillis();
                                mTitleTextView.setText(R.string.loading);
                                mUrlTextView.setText(mUrl.replace("http://", ""));
                                break;
                            }

                            case R.id.item_open_in_browser: {
                                openInBrowser(mUrl);
                                break;
                            }

                            case R.id.item_settings: {
                                Intent intent = new Intent(mContext, SettingsActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                mContext.startActivity(intent);
                                mEventHandler.onSharedLink();
                                break;
                            }
                        }
                        mOverflowPopupMenu = null;
                        return false;
                    }
                });
                mOverflowPopupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu menu) {
                        if (mOverflowPopupMenu == menu) {
                            mOverflowPopupMenu = null;
                        }
                    }
                });
                mOverflowPopupMenu.show();
            }
        });

        mContext = getContext();
        mEventHandler = eh;
        mOwner = owner;
        mUrl = url;

        WebSettings ws = mWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);

        mWebView.setLongClickable(true);
        mWebView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult hitTestResult = mWebView.getHitTestResult();
                Log.d(TAG, "onLongClick type: " + hitTestResult.getType());
                switch (hitTestResult.getType()) {
                    case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                    case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                        final String url = hitTestResult.getExtra();
                        if (url == null) {
                            return false;
                        }

                        onUrlLongClick(url);
                        return true;
                    }

                    case WebView.HitTestResult.UNKNOWN_TYPE:
                    default:
                        String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
                        String message;
                        if (defaultBrowserLabel != null) {
                            message = String.format(getResources().getString(R.string.long_press_unsupported_default_browser), defaultBrowserLabel);
                        } else {
                            message = getResources().getString(R.string.long_press_unsupported_no_default_browser);
                        }
                        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                        return false;
                }
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView webView, String title) {
                super.onReceivedTitle(webView, title);
                mTitleTextView.setText(title);
            }

            @Override
            public void onReceivedIcon(WebView webView, Bitmap bitmap) {
                super.onReceivedIcon(webView, bitmap);
                mEventHandler.onReceivedIcon(bitmap);
            }

            @Override
            public void onProgressChanged(WebView webView, int progress) {
                //Log.d(TAG, "onProgressChanged() - progress:" + progress);

                mEventHandler.onProgressChanged(progress);

                // At 60%, the page is more often largely viewable, but waiting for background shite to finish which can
                // take many, many seconds, even on a strong connection. Thus, do a check for embeds now to prevent the button
                // not being updated until 100% is reached, which feels too slow as a user.
                if (progress >= 60) {
                    if (mCheckForEmbedsCount == 0) {
                        mCheckForEmbedsCount = 1;
                        if (mYouTubeEmbedHelper != null) {
                            mYouTubeEmbedHelper.clear();
                        }

                        if (Settings.get().checkForYouTubeEmbeds()) {
                            Log.d(TAG, "onProgressChanged() - checkForYouTubeEmbeds() - progress:" + progress + ", mCheckForEmbedsCount:" + mCheckForEmbedsCount);
                            webView.loadUrl(JS_EMBED);
                        }
                    } else if (mCheckForEmbedsCount == 1 && progress >= 80) {
                        mCheckForEmbedsCount = 2;
                        if (Settings.get().checkForYouTubeEmbeds()) {
                            Log.d(TAG, "onProgressChanged() - checkForYouTubeEmbeds() - progress:" + progress + ", mCheckForEmbedsCount:" + mCheckForEmbedsCount);
                            webView.loadUrl(JS_EMBED);
                        }
                    }
                }
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            private int mCount = 0;

            @Override
            public boolean shouldOverrideUrlLoading(WebView wView, String url) {
                if (isValidUrl(url)) {
                    ++mCount;
                }

                if (mYouTubeEmbedHelper != null) {
                    mYouTubeEmbedHelper.clear();
                }

                List<ResolveInfo> resolveInfos = Settings.get().getAppsThatHandleUrl(url);
                updateAppsForUrl(resolveInfos, url);
                if (Settings.get().redirectUrlToBrowser(url)) {
                    if (openInBrowser(url)) {
                        String title = String.format(mContext.getString(R.string.link_redirected), Settings.get().getDefaultBrowserLabel());
                        MainApplication.saveUrlInHistory(mContext, null, url, title);
                        return false;
                    }
                }

                if (Settings.get().getAutoContentDisplayAppRedirect() && resolveInfos != null && resolveInfos.size() > 0) {
                    ResolveInfo resolveInfo = resolveInfos.get(0);
                    if (resolveInfo != Settings.get().mLinkBubbleEntryActivityResolveInfo) {
                        // TODO: Fix to handle multiple apps
                        if (MainApplication.loadResolveInfoIntent(mContext, resolveInfo, url, mStartTime)) {
                            String title = String.format(mContext.getString(R.string.link_loaded_with_app),
                                                        resolveInfo.loadLabel(mContext.getPackageManager()));
                            MainApplication.saveUrlInHistory(mContext, resolveInfo, url, title);

                            mEventHandler.onSharedLink();
                            return false;
                        }
                    }
                }

                configureOpenInAppButton();
                configureOpenEmbedButton();
                Log.d(TAG, "redirect to url: " + url);
                mWebView.loadUrl(url);
                mEventHandler.onPageLoading(url);
                mTitleTextView.setText(R.string.loading);
                mUrlTextView.setText(url.replace("http://", ""));
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                mEventHandler.onPageLoaded(null);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favIcon) {
                if (isValidUrl(url)) {
                    mCount = Math.max(mCount, 1);
                }
            }

            @Override
            public void onPageFinished(WebView webView, String urlAsString) {
                super.onPageFinished(webView, urlAsString);

                URL url = null;
                try {
                    url = new URL(urlAsString);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (isValidUrl(url)) {
                    updateAppsForUrl(urlAsString);
                    configureOpenInAppButton();
                    configureOpenEmbedButton();

                    mTitleTextView.setText(webView.getTitle());
                    mUrlTextView.setText(urlAsString.replace("http://", ""));

                    if (--mCount == 0) {
                        // Store final resolved url
                        mUrl = urlAsString;

                        PageLoadInfo pageLoadInfo = new PageLoadInfo();
                        pageLoadInfo.bmp = webView.getFavicon();
                        pageLoadInfo.url = urlAsString;
                        pageLoadInfo.mHost = url.getHost();
                        pageLoadInfo.title = webView.getTitle();

                        mEventHandler.onPageLoaded(pageLoadInfo);
                        Log.d(TAG, "onPageFinished() - url: " + urlAsString);

                        if (mStartTime > -1) {
                            Log.d("LoadTime", "Saved " + ((System.currentTimeMillis() - mStartTime) / 1000) + " seconds.");
                            mStartTime = -1;
                        }

                        // Always check again at 100%
                        if (Settings.get().checkForYouTubeEmbeds()) {
                            webView.loadUrl(JS_EMBED);
                            Log.d(TAG, "onPageFinished() - checkForYouTubeEmbeds()");
                        }
                    }
                }
            }
        });

        mWebView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    WebView webView = (WebView) v;
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack()) {
                                webView.stopLoading();
                                String urlBefore = webView.getUrl();
                                webView.goBack();
                                updateAppsForUrl(webView.getUrl());
                                if (mYouTubeEmbedHelper != null) {
                                    mYouTubeEmbedHelper.clear();
                                }
                                Log.d(TAG, "Go back: " + urlBefore + " -> " + webView.getUrl());
                                configureOpenInAppButton();
                                configureOpenEmbedButton();
                                return true;
                            } else {
                                mEventHandler.onSharedLink();
                            }
                            break;
                    }
                }

                return false;
            }
        });

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                openInBrowser(url);
                mEventHandler.onSharedLink();
            }
        });

        if (Settings.get().checkForYouTubeEmbeds()) {
            mJSEmbedHandler = new JSEmbedHandler();
            mWebView.addJavascriptInterface(mJSEmbedHandler, JS_VARIABLE);
        }

        updateIncognitoMode(Settings.get().isIncognitoMode());

        updateAppsForUrl(url);
        configureOpenInAppButton();
        configureOpenEmbedButton();
        Log.d(TAG, "load url: " + url);
        mStartTime = startTime;
        mWebView.loadUrl(url);
        mEventHandler.onPageLoading(url);
        mTitleTextView.setText(R.string.loading);
        mUrlTextView.setText(url.replace("http://", ""));
    }

    private void onUrlLongClick(final String url) {
        Resources resources = mContext.getResources();

        final ArrayList<String> longClickSelections = new ArrayList<String>();

        final String shareLabel = resources.getString(R.string.action_share);
        longClickSelections.add(shareLabel);

        final String openInNewBubbleLabel = resources.getString(R.string.action_open_in_new_bubble);
        longClickSelections.add(openInNewBubbleLabel);

        String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
        final String openInBrowserLabel = defaultBrowserLabel != null ?
                String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel) : null;
        if (openInBrowserLabel != null) {
            longClickSelections.add(openInBrowserLabel);
        }

        final String leftConsumeBubbleLabel = Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeLeft);
        if (leftConsumeBubbleLabel != null) {
            if (defaultBrowserLabel == null || defaultBrowserLabel.equals(leftConsumeBubbleLabel) == false) {
                longClickSelections.add(leftConsumeBubbleLabel);
            }
        }

        final String rightConsumeBubbleLabel = Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeRight);
        if (rightConsumeBubbleLabel != null) {
            if (defaultBrowserLabel == null || defaultBrowserLabel.equals(rightConsumeBubbleLabel) == false) {
                longClickSelections.add(rightConsumeBubbleLabel);
            }
        }

        Collections.sort(longClickSelections);

        ListView listView = new ListView(getContext());
        listView.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
                longClickSelections.toArray(new String[0])));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String string = longClickSelections.get(position);
                if (string.equals(openInNewBubbleLabel)) {
                    MainController.get().onOpenUrl(url, System.currentTimeMillis());
                } else if (openInBrowserLabel != null && string.equals(openInBrowserLabel)) {
                    openInBrowser(url);
                } else if (string.equals(shareLabel)) {
                    showSelectShareMethod(url, false);
                } else if (leftConsumeBubbleLabel != null && string.equals(leftConsumeBubbleLabel)) {
                    MainApplication.handleBubbleAction(mContext, Config.BubbleAction.ConsumeLeft, url);
                } else if (rightConsumeBubbleLabel != null && string.equals(rightConsumeBubbleLabel)) {
                    MainApplication.handleBubbleAction(mContext, Config.BubbleAction.ConsumeRight, url);
                }

                if (mLongPressAlertDialog != null) {
                    mLongPressAlertDialog.dismiss();
                }
            }
        });

        mLongPressAlertDialog = new AlertDialog.Builder(getContext()).create();
        mLongPressAlertDialog.setView(listView);
        mLongPressAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mLongPressAlertDialog.show();
    }

    private void configureOpenEmbedButton() {
        if (mOpenEmbedButton.configure(mYouTubeEmbedHelper)) {
            mOpenEmbedButton.invalidate();
        } else {
            mOpenEmbedButton.setVisibility(GONE);
        }
    }

    private void configureOpenInAppButton() {
        if (mOpenInAppButton.configure(mAppsForUrl)) {
            mOpenInAppButton.invalidate();
        } else {
            mOpenInAppButton.setVisibility(GONE);
        }
    }

    private void updateAppsForUrl(String url) {
        List<ResolveInfo> resolveInfos = Settings.get().getAppsThatHandleUrl(url);
        updateAppsForUrl(resolveInfos, url);
    }

    private void updateAppsForUrl(List<ResolveInfo> resolveInfos, String url) {
        if (resolveInfos != null && resolveInfos.size() > 0) {
            mTempAppsForUrl.clear();
            if (mTempUrl.toString().equals(url) == false) {
                try {
                    mTempUrl = new URL(url);
                } catch (MalformedURLException e) {
                    return;
                }
            }
            for (ResolveInfo resolveInfoToAdd : resolveInfos) {
                if (resolveInfoToAdd.activityInfo != null) {
                    boolean alreadyAdded = false;
                    for (int i = 0; i < mAppsForUrl.size(); i++) {
                        AppForUrl existing = mAppsForUrl.get(i);
                        if (existing.mResolveInfo.activityInfo.packageName.equals(resolveInfoToAdd.activityInfo.packageName)
                                && existing.mResolveInfo.activityInfo.name.equals(resolveInfoToAdd.activityInfo.name)) {
                            alreadyAdded = true;
                            if (existing.mUrl.equals(url) == false) {
                                if (mTempUrl.getHost().contains(existing.mUrl.getHost())
                                        && mTempUrl.getHost().length() > existing.mUrl.getHost().length()) {
                                    // don't update the url in this case. This means prevents, as an example, saving a host like
                                    // "mobile.twitter.com" instead of using "twitter.com". This occurs when loading
                                    // "https://twitter.com/lokibartleby/status/412160702707539968" with Tweet Lanes
                                    // and the official Twitter client installed.
                                } else {
                                    try {
                                        existing.mUrl = new URL(url.toString());   // Update the Url
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            break;
                        }
                    }

                    if (alreadyAdded == false) {
                        mTempAppsForUrl.add(resolveInfoToAdd);
                    }
                }
            }

            if (mTempAppsForUrl.size() > 0) {
                URL currentUrl;
                try {
                    currentUrl = new URL(url.toString());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return;
                }

                // We need to handle the following case:
                //   * Load reddit.com/r/Android. The app to handle that URL might be "Reddit is Fun" or something similar.
                //   * Click on a link to play.google.com/store/, which is handled by the "Google Play" app.
                //   * The code below adds "Google Play" to the app list that contains "Reddit is Fun",
                //       even though "Reddit is Fun" is not applicable for this link.
                // Unfortunately there is no way reliable way to find out when a user has clicked on a link using the WebView.
                // http://stackoverflow.com/a/17937536/328679 is close, but doesn't work because it relies on onPageFinished()
                // being called, which will not be called if the current page is still loading when the link was clicked.
                //
                // So, in the event contains results, and these results reference a different URL that which matched the
                // resolveInfos passed in, clear mAppsForUrl.
                if (mAppsForUrl.size() > 0) {
                    URL firstUrl = mAppsForUrl.get(0).mUrl;
                    if ((currentUrl.getHost().contains(firstUrl.getHost())
                            && currentUrl.getHost().length() > firstUrl.getHost().length()) == false) {
                        mAppsForUrl.clear();    // start again
                    }
                }

                for (ResolveInfo resolveInfoToAdd : mTempAppsForUrl) {
                    mAppsForUrl.add(new AppForUrl(resolveInfoToAdd, currentUrl));
                }
            }

        } else {
            mAppsForUrl.clear();
        }
    }

    public void onAnimateOnScreen() {
        hidePopups();
        resetButtonPressedStates();
    }

    public void onAnimateOffscreen() {
        hidePopups();
        resetButtonPressedStates();
    }

    void onCurrentContentViewChanged(boolean isCurrent) {
        hidePopups();
        resetButtonPressedStates();
    }

    void onOrientationChanged() {
        invalidate();
    }

    private void hidePopups() {
        if (mOverflowPopupMenu != null) {
            mOverflowPopupMenu.dismiss();
            mOverflowPopupMenu = null;
        }
        if (mLongPressAlertDialog != null) {
            mLongPressAlertDialog.dismiss();
            mLongPressAlertDialog = null;
        }
    }

    private void resetButtonPressedStates() {
        mShareButton.setIsTouched(false);
        mOpenEmbedButton.setIsTouched(false);
        mOpenInAppButton.setIsTouched(false);
        mOverflowButton.setIsTouched(false);
    }

    private boolean openInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (MainApplication.loadInBrowser(mContext, intent, true)) {
            mEventHandler.onSharedLink();
            return true;
        }

        return false;
    }

    // For security reasons, all callbacks should be in a self contained class
    public class JSEmbedHandler {

        private Runnable mUpdateOpenInAppRunnable = null;

        @JavascriptInterface
        public void onEmbeds(String string) {
            Log.d(TAG, "onEmbeds() - " + string);

            if (string == null || string.length() == 0) {
                return;
            }

            if (mYouTubeEmbedHelper == null) {
                mYouTubeEmbedHelper = new YouTubeEmbedHelper(mContext);
            }

            String[] strings = string.split(",");
            if (mYouTubeEmbedHelper.onEmbeds(strings)) {
                if (mUpdateOpenInAppRunnable == null) {
                    mUpdateOpenInAppRunnable = new Runnable() {
                        @Override
                        public void run() {
                            configureOpenEmbedButton();
                        }
                    };
                }

                mOpenEmbedButton.post(mUpdateOpenInAppRunnable);
            }
        }
    };

    private JSEmbedHandler mJSEmbedHandler;


}
