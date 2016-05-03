/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.Constant.BubbleAction;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.adblock.TPFilterParser;
import com.linkbubble.adblock.WhiteListCollector;
import com.linkbubble.adinsert.AdInserter;
import com.linkbubble.articlerender.ArticleContent;
import com.linkbubble.articlerender.ArticleRenderer;
import com.linkbubble.httpseverywhere.HttpsEverywhere;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.DownloadImage;
import com.linkbubble.util.Util;
import com.linkbubble.webrender.CustomWebView;
import com.linkbubble.webrender.WebRenderer;
import com.linkbubble.adblock.ABPFilterParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class ContentView extends FrameLayout {

    private static final String TAG = "UrlLoad";
    private static final Integer BLACK_LIST_MAX_REDIRECT_COUNT = 5;
    private static final int DEFAULT_TOOLBAR_SIZE = 112;

    private static int sNextArticleNotificationId = 1111;

    private enum LifeState {
        Init,
        Alive,
        Removed,
        Destroyed
    }

    private WebRenderer mWebRenderer;
    private ArticleRenderer mArticleRenderer;
    private int mArticleNotificationId = -1;
    private TabView mOwnerTabView;

    private View mCaretView;
    private CondensedTextView mTitleTextView;
    private CondensedTextView mUrlTextView;
    private ContentViewButton mShareButton;
    private ContentViewButton mReloadButton;
    private ArticleModeButton mArticleModeButton;
    private OpenInAppButton mOpenInAppButton;
    private OpenEmbedButton mOpenEmbedButton;
    private ContentViewButton mOverflowButton;
    private View mRequestLocationShadow;
    private View mRequestLocationContainer;
    private CondensedTextView mRequestLocationTextView;
    private Button mRequestLocationYesButton;
    private LinearLayout mToolbarLayout;
    private EventHandler mEventHandler;
    private int mCurrentProgress = 0;

    // Search URL functionality
    private CustomAutoCompleteTextView metUrl;
    private ImageButton mbtUrlClear;
    private FrameLayout mContentEditUrl;

    private boolean mPageFinishedLoading;
    private LifeState mLifeState = LifeState.Init;
    private Set<String> mAppPickersUrls = new HashSet<String>();

    private List<AppForUrl> mAppsForUrl = new ArrayList<AppForUrl>();
    private List<ResolveInfo> mTempAppsForUrl = new ArrayList<ResolveInfo>();

    private PopupMenu mOverflowPopupMenu;
    private AlertDialog mLongPressAlertDialog;
    private long mInitialUrlLoadStartTime;
    private String mInitialUrlAsString;
    private String mLoadingString;
    private Context mContext;
    private MainController mController;

    private Stack<URL> mUrlStack = new Stack<URL>();
    // We only want to handle this once per link. This prevents 3+ dialogs appearing for some links, which is a bad experience. #224
    private boolean mHandledAppPickerForCurrentUrl = false;
    private boolean mUsingLinkBubbleAsDefaultForCurrentUrl = false;
    
    private SearchURLCustomAdapter mAdapter;
    private SearchURLSuggestions mFirstSuggestedItem;

    private float mOneRowAutoSuggestionsSize = 53f;
    private int mRowsToShowOnAutoSuggestions = 5;

    private boolean mApplyAutoSuggestionToUrlString = true;
    private boolean mSetTheRealUrlString = true;
    private boolean mFirstTimeUrlTyped = true;
    private boolean mHostInWhiteList = false;

    ConcurrentHashMap<String, Integer> mHostRedirectCounter;

    // Tracking protection third party hosts
    String[] mThirdPartyHosts = null;

    public ContentView(Context context) {
        this(context, null);
    }

    public ContentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;
        mLoadingString = getResources().getString(R.string.loading);
    }

    public long getTotalTrackedLoadTime() {
        if (mInitialUrlLoadStartTime > -1) {
            return System.currentTimeMillis() - mInitialUrlLoadStartTime;
        }
        return -1;
    }

    public WebRenderer getWebRenderer() {
        return mWebRenderer;
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
                if (mResolveInfo != null) {
                    mIcon = mResolveInfo.loadIcon(context.getPackageManager());
                }
            }

            return mIcon;
        }
    }

    public interface EventHandler {
        public void onPageLoading(URL url);
        public void onProgressChanged(int progress);
        public void onPageLoaded(boolean withError);
        public boolean onReceivedIcon(Bitmap bitmap);
        public void setDefaultFavicon();
        public void onCanGoBackChanged(boolean canGoBack);
        public boolean hasHighQualityFavicon();
        void onThemeColor(Integer color);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (isInEditMode()) {
            return;
        }

        /*
        float centerX = Config.mScreenCenterX;
        float indicatorEndY = 2.f;
        float indicatorStartX = centerX - mHeaderHeight + indicatorEndY;
        float indicatorEndX = centerX + mHeaderHeight - indicatorEndY;

        mTempPath.reset();
        mTempPath.moveTo(indicatorStartX, mHeaderHeight);
        mTempPath.lineTo(centerX, indicatorEndY);
        mTempPath.lineTo(indicatorEndX, mHeaderHeight);
        canvas.drawPath(mTempPath, sIndicatorPaint);

        canvas.drawLine(indicatorEndY, mHeaderHeight, indicatorStartX, mHeaderHeight, sBorderPaint);
        canvas.drawLine(indicatorStartX, mHeaderHeight, centerX, 0, sBorderPaint);
        canvas.drawLine(centerX, indicatorEndY, indicatorEndX, mHeaderHeight, sBorderPaint);
        canvas.drawLine(indicatorEndX, mHeaderHeight, Config.mScreenWidth, mHeaderHeight, sBorderPaint);
        */
    }

    public void destroy() {
        Log.d(TAG, "*** destroy() - url" + (mWebRenderer.getUrl() != null ? mWebRenderer.getUrl().toString() : "<null>"));
        mLifeState = LifeState.Destroyed;
        removeView(mWebRenderer.getView());
        mWebRenderer.destroy();

        if (mArticleRenderer != null) {
            removeView(mArticleRenderer.getView());
            mArticleRenderer.destroy();
        }

        //if (mDelayedAutoContentDisplayLinkLoadedScheduled) {
        //    mDelayedAutoContentDisplayLinkLoadedScheduled = false;
        //    Log.e(TAG, "*** set mDelayedAutoContentDisplayLinkLoadedScheduled=" + mDelayedAutoContentDisplayLinkLoadedScheduled);
        //}
        removeCallbacks(mDelayedAutoContentDisplayLinkLoadedRunnable);
    }

    public void onRemoved() {
        mLifeState = LifeState.Removed;
        cancelWearNotification();
    }

    public void onRestored() {
        mLifeState = LifeState.Alive;
        // If we need to re-add the notification, do so here
        configureArticleModeButton();
    }

    public void updateIncognitoMode(boolean incognito) {
        mWebRenderer.updateIncognitoMode(incognito);
    }

    // We need it to be a member, because we need to dismiss it on bubbles collapse
    private AlertDialog mShareAlertDialog = null;
    private void showSelectShareMethod(final String urlAsString, final boolean closeBubbleOnShare) {

        mShareAlertDialog = ActionItem.getShareAlert(getContext(), false, new ActionItem.OnActionItemSelectedListener() {
            @Override
            public void onSelected(ActionItem actionItem) {
                Intent intent = Util.getSendIntent(actionItem.mPackageName, actionItem.mActivityClassName, urlAsString);
                getContext().startActivity(intent);

                boolean isCopyToClipboardAction = actionItem.mPackageName.equals("com.google.android.apps.docs")
                        && actionItem.mActivityClassName.equals("com.google.android.apps.docs.app.SendTextToClipboardActivity");

                // L_WATCH: L currently lacks getRecentTasks(), so minimize here
                if (isCopyToClipboardAction == false && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    MainController.get().switchToBubbleView();
                }

                //if (closeBubbleOnShare && isCopyToClipboardAction == false && MainController.get() != null) {
                //    MainController.get().closeTab(mOwnerTabView, true);
                //}
            }
        });
        Util.showThemedDialog(mShareAlertDialog);
    }

    private void saveImage(final String urlAsString) {
        new DownloadImage(mContext, urlAsString).download();
    }

    ArrayList<Drawable> mTintableDrawables = new ArrayList<>();

    private Drawable getTintableDrawable(@DrawableRes int resId) {
        return getTintableDrawable(resId, true);
    }

    private Drawable getTintableDrawable(@DrawableRes int resId, boolean addToList) {
        Drawable d = Util.getTintableDrawable(this.getContext(), resId);
        if (addToList) {
            mTintableDrawables.add(d);
        }
        return d;
    }

    private void HostInWhiteListCheck(String url) {
        MainApplication app = (MainApplication) mContext.getApplicationContext();

        WhiteListCollector whiteListCollector = app.getWhiteListCollector();
        if (null == whiteListCollector) {
            mHostInWhiteList = false;

            return;
        }

        String host = "";
        try {
            host = new URL(url).getHost();
        }
        catch (MalformedURLException exc) {
        }

        mHostInWhiteList = whiteListCollector.isInWhiteList(host);
    }

    private void AddRemoveHostFromWhiteList(String url, boolean add) {
        MainApplication app = (MainApplication) mContext.getApplicationContext();

        WhiteListCollector whiteListCollector = app.getWhiteListCollector();
        if (null == whiteListCollector) {
            mHostInWhiteList = false;

            return;
        }

        String host = "";
        try {
            host = new URL(url).getHost();
        }
        catch (MalformedURLException exc) {
        }

        if (add) {
            whiteListCollector.addHostToWhiteList(host);
        }
        else {
            whiteListCollector.removeHostFromWhiteList(host);
        }
    }

    public int toolbarHeight() {
        FrameLayout.LayoutParams currentUrlBarParams = (FrameLayout.LayoutParams)mContentEditUrl.getLayoutParams();
        FrameLayout.LayoutParams currentShadowParams = (FrameLayout.LayoutParams)findViewById(R.id.actionbar_shadow).getLayoutParams();
        if (null != currentUrlBarParams && null != currentShadowParams) {
            return currentUrlBarParams.height + currentShadowParams.height;
        }

        return DEFAULT_TOOLBAR_SIZE;
    }
    // The function configures the urlBar
    private void configureUrlBar(String urlAsString, final MainController controller) {
        // Set the current URL to the search URL
        mContentEditUrl = (FrameLayout)findViewById(R.id.content_edit_url);
        metUrl = (CustomAutoCompleteTextView) findViewById(R.id.autocomplete_top500websites);

        metUrl.setDropDownWidth(getResources().getDisplayMetrics().widthPixels);
        metUrl.setText(urlAsString);
        mFirstTimeUrlTyped = true;
        metUrl.addTextChangedListener(murlTextWatcher);
        metUrl.setOnFocusChangeListener(murlOnFocusChangeListener);
        metUrl.setOnItemClickListener(murlOnItemClickListener);
        metUrl.setOnEditorActionListener(murlActionListener);
        metUrl.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        metUrl.setOnKeyListener(murlKeyListener);
        metUrl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (metUrl.mCopyPasteContextMenuCreated) {
                    controller.onBubbleFlowContextMenuAppearedGone(false);
                    metUrl.mCopyPasteContextMenuCreated = false;
                }
            }
        });
        metUrl.configure(controller);
        metUrl.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                metUrl.mCopyPasteContextMenuCreated = true;
                controller.onBubbleFlowContextMenuAppearedGone(true);
            }
        });

        mAdapter = new SearchURLCustomAdapter(getContext(), android.R.layout.simple_list_item_1, getResources(),
                getResources().getDisplayMetrics().widthPixels);
        mAdapter.mRealUrlBarConstraint = urlAsString;
        metUrl.setAdapter(mAdapter);

        mAdapter.registerDataSetObserver(mDataSetObserver);

        mbtUrlClear = (ImageButton) findViewById(R.id.search_url_clear);
        mbtUrlClear.setOnClickListener(mbtClearUrlClicked);
    }

    void setTabAsActive () {
        try {
            if (mUrlTextView.getText().toString().equals(getContext().getString(R.string.empty_bubble_page))) {
                mTitleTextView.performClick();
            } else {
                View view = mWebRenderer.getView();
                if (null != view) {
                    view.requestFocus();
                }
            }
        }
        catch (NullPointerException exc) {
            // We have that exception sometimes inside Android SDK on requestFocus,
            // we would better to not get focus than crash
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    void configure(String urlAsString, TabView ownerTabView, long urlLoadStartTime, boolean hasShownAppPicker,
                   MainController controller, EventHandler eventHandler) throws MalformedURLException {
        mController = controller;
        mHostRedirectCounter = new ConcurrentHashMap<String, Integer>();
        mLifeState = LifeState.Alive;
        mTintableDrawables.clear();


        HostInWhiteListCheck(urlAsString);
        View webRendererPlaceholder = findViewById(R.id.web_renderer_placeholder);
        mWebRenderer = WebRenderer.create(WebRenderer.Type.WebView, getContext(), mWebRendererController, webRendererPlaceholder, TAG);
        mWebRenderer.setUrl(mWebRendererController.getHTTPSUrl(urlAsString));

        // Generates 1000 history links
        /*for (int i = 0; i < 1000; i++) {
            URL currentUrl = mWebRenderer.getUrl();
            MainApplication.saveUrlInHistory(getContext(), null, currentUrl.toString() + String.valueOf(i), currentUrl.getHost(), "111_test");
        }*/
        //

        if (mArticleRenderer != null) {
            mArticleRenderer.destroy();
            mArticleRenderer = null;
        }

        mOwnerTabView = ownerTabView;
        mHandledAppPickerForCurrentUrl = hasShownAppPicker;
        mUsingLinkBubbleAsDefaultForCurrentUrl = false;

        if (hasShownAppPicker) {
            mAppPickersUrls.add(urlAsString);
        }

        mToolbarLayout = (LinearLayout) findViewById(R.id.content_toolbar);
        mTitleTextView = (CondensedTextView) findViewById(R.id.title_text);
        mUrlTextView = (CondensedTextView) findViewById(R.id.url_text);

        // Set on click listeners to show the search URL control
        mTitleTextView.setOnClickListener(mOnURLEnterClicked);
        mUrlTextView.setOnClickListener(mOnURLEnterClicked);

        findViewById(R.id.content_text_container).setOnTouchListener(mOnTextContainerTouchListener);

        configureUrlBar(urlAsString, controller);

        mCaretView = findViewById(R.id.caret);

        mShareButton = (ContentViewButton)findViewById(R.id.share_button);
        mShareButton.setImageDrawable(getTintableDrawable(R.drawable.ic_share_white_24dp));
        mShareButton.setOnClickListener(mOnShareButtonClickListener);

        mOpenInAppButton = (OpenInAppButton)findViewById(R.id.open_in_app_button);
        mOpenInAppButton.setOnOpenInAppClickListener(mOnOpenInAppButtonClickListener);

        mOpenEmbedButton = (OpenEmbedButton)findViewById(R.id.open_embed_button);
        mOpenEmbedButton.setOnOpenEmbedClickListener(mOnOpenEmbedButtonClickListener);

        mReloadButton = (ContentViewButton)findViewById(R.id.reload_button);
        mReloadButton.setImageDrawable(getTintableDrawable(R.drawable.ic_refresh_white_24dp));
        mReloadButton.setOnClickListener(mOnReloadButtonClickListener);

        mArticleModeButton = (ArticleModeButton)findViewById(R.id.article_mode_button);
        mArticleModeButton.setState(ArticleModeButton.State.Article);
        mArticleModeButton.setOnClickListener(mOnArticleModeButtonClickListener);

        mOverflowButton = (ContentViewButton)mToolbarLayout.findViewById(R.id.overflow_button);
        mOverflowButton.setImageDrawable(getTintableDrawable(R.drawable.ic_more_vert_white_24dp));
        mOverflowButton.setOnClickListener(mOnOverflowButtonClickListener);

        mRequestLocationShadow = findViewById(R.id.request_location_shadow);
        mRequestLocationContainer = findViewById(R.id.request_location_container);
        mRequestLocationTextView = (CondensedTextView) findViewById(R.id.requesting_location_text_view);
        mRequestLocationYesButton = (Button) findViewById(R.id.access_location_yes);
        findViewById(R.id.access_location_no).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAllowLocationDialog();
            }
        });

        mEventHandler = eventHandler;
        mEventHandler.onCanGoBackChanged(false);
        mPageFinishedLoading = false;

        updateIncognitoMode(Settings.get().isIncognitoMode());

        mInitialUrlLoadStartTime = urlLoadStartTime;
        mInitialUrlAsString = urlAsString;

        updateAndLoadUrl(urlAsString);
        updateAppsForUrl(mWebRenderer.getUrl());
        Log.d(TAG, "load url: " + urlAsString);
        updateUrlTitleAndText(urlAsString);

        updateColors(null);
    }

    private void WorkWithURL(String strUrl, SearchURLSuggestions.SearchEngine selectedSearchEngine, boolean fromGoAction) {

        metUrl.dismissDropDown();

        strUrl = strUrl.trim();
        String strUrlWithPrefix = strUrl;
        if (!strUrl.startsWith(getContext().getString(R.string.http_prefix)) &&
                !strUrl.startsWith(getContext().getString(R.string.https_prefix)))
            strUrlWithPrefix = getContext().getString(R.string.http_prefix) + strUrl;

        if (SearchURLSuggestions.SearchEngine.NONE == selectedSearchEngine && Patterns.WEB_URL.matcher(strUrlWithPrefix).matches()) {
            LoadWebPage(strUrlWithPrefix);
        } else if (SearchURLSuggestions.SearchEngine.NONE == selectedSearchEngine && fromGoAction) {
            if (null != mFirstSuggestedItem) {
                WorkWithURL(strUrl, mFirstSuggestedItem.EngineToUse, false);
            }
        } else if (SearchURLSuggestions.SearchEngine.DUCKDUCKGO == selectedSearchEngine) {
            // Make the search using duck duck go
            try {
                String strQuery = String.format(getContext().getString(R.string.duckduckgo_search_engine), URLEncoder.encode(strUrl, "UTF-8"));
                LoadWebPage(strQuery);
            } catch (IOException ioe) {
                Log.e(TAG, ioe.getMessage(), ioe);
            }
        }
        else if (SearchURLSuggestions.SearchEngine.GOOGLE == selectedSearchEngine) {
            // Make the search using google
            try {
                String strQuery = getContext().getString(R.string.google_search_engine) + URLEncoder.encode(strUrl, "UTF-8");
                LoadWebPage(strQuery);
            } catch (IOException ioe) {
                Log.e(TAG, ioe.getMessage(), ioe);
            }
        }
        else if (SearchURLSuggestions.SearchEngine.YAHOO == selectedSearchEngine) {
            // Make the search using yahoo
            try {
                String strQuery = getContext().getString(R.string.yahoo_search_engine) + URLEncoder.encode(strUrl, "UTF-8");
                LoadWebPage(strQuery);
            } catch (IOException ioe) {
                Log.e(TAG, ioe.getMessage(), ioe);
            }
        }
        else if (SearchURLSuggestions.SearchEngine.AMAZON == selectedSearchEngine) {
            // Make the search using amazon
            try {
                String strQuery = getContext().getString(R.string.amazon_search_engine) + URLEncoder.encode(strUrl, "UTF-8");
                LoadWebPage(strQuery);
            } catch (IOException ioe) {
                Log.e(TAG, ioe.getMessage(), ioe);
            }
        }

        mToolbarLayout.bringToFront();
    }

    private void LoadWebPage(String strUrl) {
        updateAndLoadUrl(strUrl);
        mWebRendererController.resetBubblePanelAdjustment();
    }


    Integer themeColor;
    void updateColors(Integer color) {
        themeColor = color;
        int textColor;
        int bgColor;
        if (color == null || !Settings.get().getThemeToolbar()) {
            textColor = Settings.get().getThemedTextColor();
            bgColor = Settings.get().getThemedContentViewColor();
            mCaretView.setBackground(getResources().getDrawable(Settings.get().getDarkThemeEnabled()
                    ? R.drawable.content_view_caret_dark : R.drawable.content_view_caret_white));
        } else {
            // Calculate text color based on contrast with background:
            // https://24ways.org/2010/calculating-color-contrast/
            int yiq = (Color.red(color) * 299 +
                    Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
            textColor = yiq >= 128 ? Settings.COLOR_BLACK : Settings.COLOR_WHITE;

            bgColor = color;
            Drawable d = getTintableDrawable(R.drawable.content_view_caret_white, false);
            DrawableCompat.setTint(d, color);
            mCaretView.setBackground(d);
        }

        mToolbarLayout.setBackgroundColor(bgColor);
        mTitleTextView.setTextColor(textColor);
        mUrlTextView.setTextColor(textColor);
        metUrl.setBackgroundColor(bgColor);
        metUrl.setTextColor(textColor);
        mContentEditUrl.setBackgroundColor(bgColor);

        for (Drawable d : mTintableDrawables) {
            DrawableCompat.setTint(d, textColor);
        }

        mArticleModeButton.updateTheme(color);
    }

    public void collapse() {
        if (null != mShareAlertDialog) {
            mShareAlertDialog.dismiss();
            mShareAlertDialog = null;
        }
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(metUrl.getWindowToken(),
                InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    void setFaviconColor(Integer color) {
        updateColors(color);
    }
    WebRenderer.Controller mWebRendererController = new WebRenderer.Controller() {

        @Override
        public void resetBubblePanelAdjustment() {
            MainController mainController = MainController.get();
            if (null != mainController) {
                mainController.adjustBubblesPanel(0, 0, false, true);
            }
        }

        @Override
        public void adjustBubblesPanel(int newY, int oldY, boolean afterTouchAdjust) {
            MainController mainController = MainController.get();
            if (null != mainController) {
                mainController.adjustBubblesPanel(newY, oldY, afterTouchAdjust, false);
            }
        }

        @Override
        public String getHTTPSUrl(String originalUrl) {
            if (mHostInWhiteList || !Settings.get().isHttpsEverywhereEnabled()) {
                return originalUrl;
            }
            MainApplication app = (MainApplication) mContext.getApplicationContext();
            HttpsEverywhere httpsEverywhere = app.getHttpsEverywhere();
            if (null == httpsEverywhere) {
                return originalUrl;
            }


            Integer redirectedCount = 0;
            String urlToBlackList = "";
            try {
                urlToBlackList = new URL(originalUrl).getHost();
            }
            catch (MalformedURLException exc) {
                urlToBlackList = originalUrl;
            }
            if (null != mHostRedirectCounter && null != originalUrl && !originalUrl.startsWith("https")) {
                if (urlToBlackList.startsWith("http://m.")) {
                    urlToBlackList = "http://" + urlToBlackList.substring("http://m.".length());
                }
                redirectedCount = mHostRedirectCounter.get(urlToBlackList);
                if (null == redirectedCount) {
                    redirectedCount = 0;
                }
                if (redirectedCount >= BLACK_LIST_MAX_REDIRECT_COUNT) {
                    return originalUrl;
                }
            }
            String realUrl = httpsEverywhere.getRealUrl(originalUrl);
            if (!realUrl.equals(originalUrl)) {
                redirectedCount++;
                if (null != mHostRedirectCounter) {
                    mHostRedirectCounter.put(urlToBlackList, redirectedCount);
                }
            }

            return realUrl;
        }

        @Override
        public boolean shouldAdBlockUrl(String baseHost, String urlStr, String filterOption) {
            if (mHostInWhiteList) {
                return false;
            }

            if (null != mWebRenderer) {
                URL currentUrl = mWebRenderer.getUrl();
                if (currentUrl.toString().equals(urlStr)) {
                    return false;
                }
            }

            MainApplication app = (MainApplication) mContext.getApplicationContext();
            ABPFilterParser parser = app.getABPParser();
            if (null == parser) {
                return false;
            }

            return parser.shouldBlockJava(baseHost, urlStr, filterOption);
        }

        @Override
        public boolean shouldTrackingProtectionBlockUrl(String baseHost, String host) {
            if (mHostInWhiteList) {
                return false;
            }

            MainApplication app = (MainApplication) mContext.getApplicationContext();
            TPFilterParser tpList = app.getTrackingProtectionList();
            if (null == tpList) {
                return false;
            }

            if (tpList.matchesTrackerJava(baseHost, host)) {
                if (null == mThirdPartyHosts) {
                    mThirdPartyHosts = tpList.findFirstPartyHostsJava(baseHost).split(",");
                }

                if (null != mThirdPartyHosts) {
                    for (int i = 0; i < mThirdPartyHosts.length; i++) {
                        if (host == mThirdPartyHosts[i] || host.endsWith("." + mThirdPartyHosts[i])) {
                            return false;
                        }
                    }
                }

                // Temporary whitelist until we have an UI to unblock hosts
                List<String> whitelistHosts = Arrays.asList("connect.facebook.net");
                if (whitelistHosts.contains(host)) {
                    return false;
                }

                return true;
            }

            return false;
        }

        @Override
        public String adInsertionList(String baseHost) {
            if (mHostInWhiteList) {
                return "";
            }

            MainApplication app = (MainApplication) mContext.getApplicationContext();
            if (!app.mAdInserterEnabled) {
                return "";
            }
            AdInserter adInsertionList = app.getAdInserter();
            if (null == adInsertionList) {
                return "";
            }

            return adInsertionList.getHostObjects(baseHost);
        }

        private int mConsecutiveRedirectCount = 0;

        @Override
        public void doUpdateVisitedHistory (String url, boolean isReload, boolean unknownClick) {
            String peekUrl = "";
            if (mUrlStack.size() > 0) {
                peekUrl = mUrlStack.peek().toString();
            }
            // We need isReload check when click on links from twitter. It usually has some internal links which are not the same
            // as real link and isReload is true in that case, so we need to skip them if it is a top website
            if ((isReload && 0 == mUrlStack.size()) || url.equals("file:///android_asset/blank.html") ||
                    mUrlStack.size() > 0 && peekUrl.equals(url)) {
                return;
            }

            try {
                URL historyUrl = new URL(url);
                if (unknownClick) {
                    // Here we check on anchors change without clicking on them
                    String ref = historyUrl.getRef();
                    if (null != ref && 0 != ref.length()
                            && (ref.indexOf("/") == -1 || ref.equals("/") || ref.length() >= 2 && ref.indexOf("/", 1) == -1)) {
                        String originalUrl = url.substring(0, url.length() - ref.length() - 1);
                        if (peekUrl.equals(originalUrl)) {
                            return;
                        }
                        else if (0 != peekUrl.length()){
                            URL peekURLForRef = new URL(peekUrl);
                            String peekRef = peekURLForRef.getRef();
                            if (null != peekRef && 0 != peekRef.length()
                                    && (peekRef.indexOf("/") == -1 || peekRef.equals("/")
                                        || peekRef.length() >= 2 && peekRef.indexOf("/", 1) == -1)) {
                                String originalPeekUrl = peekUrl.substring(0, peekUrl.length() - peekRef.length() - 1);
                                if (originalPeekUrl.equals(originalUrl)) {
                                    return;
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "[urlstack] push:" + url + ", urlStack.size():" + mUrlStack.size());
                mUrlStack.push(historyUrl);
                mEventHandler.onCanGoBackChanged(mUrlStack.size() > 1);
            } catch (MalformedURLException e) {
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(String urlAsString, boolean viaUserInput) {
            if (mLifeState != LifeState.Alive) {
                mConsecutiveRedirectCount = 0;
                return true;
            }

            if (urlAsString.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(urlAsString));
                if (MainApplication.loadIntent(getContext(), intent, urlAsString, mInitialUrlLoadStartTime)) {
                    MainController.get().switchToBubbleView();
                }
                mConsecutiveRedirectCount = 0;
                return true;
            }

            URL updatedUrl = getUpdatedUrl(urlAsString, !viaUserInput);
            if (updatedUrl == null) {
                Log.d(TAG, "ignore unsupported URI scheme: " + urlAsString);
                showOpenInBrowserPrompt(R.string.unsupported_scheme_default_browser,
                        R.string.unsupported_scheme_no_default_browser, mWebRenderer.getUrl().toString());
                mConsecutiveRedirectCount = 0;
                return true;        // true because we've handled the link ourselves
            }

            Log.d(TAG, "shouldOverrideUrlLoading() - url:" + urlAsString);
            if (viaUserInput) {
                URL currentUrl = mWebRenderer.getUrl();
                mHandledAppPickerForCurrentUrl = false;
                mUsingLinkBubbleAsDefaultForCurrentUrl = false;
            }

            //if (mDelayedAutoContentDisplayLinkLoadedScheduled) {
            //    mDelayedAutoContentDisplayLinkLoadedScheduled = false;
            //    Log.e(TAG, "*** set mDelayedAutoContentDisplayLinkLoadedScheduled=" + mDelayedAutoContentDisplayLinkLoadedScheduled);
            //}
            removeCallbacks(mDelayedAutoContentDisplayLinkLoadedRunnable);

            String host;
            try {
                URL url = new URL(urlAsString);
                host = url.getHost();
                if (host.equals("www.forbes.com")) {
                    CookieManager.getInstance().setCookie("http://www.forbes.com", "forbes_ab=true");
                    CookieManager.getInstance().setCookie("http://www.forbes.com", "welcomeAd=true");
                    CookieManager.getInstance().setCookie("http://www.forbes.com", "adblock_session=Off");
                    CookieManager.getInstance().setCookie("http://www.forbes.com", "dailyWelcomeCookie=true");
                    if (url.getPath().equals("/forbes/welcome/") && mConsecutiveRedirectCount < 5) {
                        mConsecutiveRedirectCount++;
                        updateAndLoadUrl("http://www.forbes.com/");
                        return true;
                    }
                }
            } catch (Exception e) {
            }

            mConsecutiveRedirectCount = 0;
            updateAndLoadUrl(urlAsString);
            mWebRendererController.resetBubblePanelAdjustment();
            return true;
        }

        @Override
        public void onLoadUrl(String urlAsString) {
            try {
                URL url = new URL(urlAsString);
                mEventHandler.onPageLoading(url);
                updateUrlTitleAndText(urlAsString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReceivedError() {
            Log.d(TAG, "onReceivedError()");
            mEventHandler.onPageLoaded(true);
            mReloadButton.setVisibility(VISIBLE);
            mShareButton.setVisibility(GONE);
            mArticleModeButton.setVisibility(GONE);
        }

        @Override
        public void onPageStarted(final String urlAsString, Bitmap favIcon) {
            Log.d(TAG, "onPageStarted() - " + urlAsString);
            //to do debug
            /*
            try {
                CrashTracking.log("onPageStarted(), " + urlAsString + ", index:" + MainController.get().getTabIndex(mOwnerTabView));
            } catch (NullPointerException npe) {
                CrashTracking.log("onPageStarted(), " + urlAsString + ", index: no current MainController");
                Log.e(TAG, npe.getLocalizedMessage(), npe);
            }
            */

            if (mLifeState != LifeState.Alive) {
                return;
            }

            hideAllowLocationDialog();

            mPageFinishedLoading = false;

            String oldUrl = mWebRenderer.getUrl().toString();

            if (urlAsString.equals(Constant.ABOUT_BLANK_URI)) {
                Log.d(TAG, "ignore " + urlAsString);
            } else if (updateUrl(urlAsString) == false) {
                List<ResolveInfo> tempResolveInfos = new ArrayList<>();
                if (!urlAsString.equals(mContext.getString(R.string.empty_bubble_page))) {
                    tempResolveInfos = Settings.get().getAppsThatHandleUrl(urlAsString, getContext().getPackageManager());
                }
                final List<ResolveInfo> apps = tempResolveInfos;

                boolean openedInApp = apps != null && apps.size() > 0 ? openInApp(apps.get(0), urlAsString) : false;
                if (openedInApp == false) {
                    CrashTracking.log("ContentView.onPageStarted() - openedInApp == false");
                    openInBrowser(urlAsString);
                }
                return;
            }

            if (oldUrl.equals(Constant.NEW_TAB_URL)) {
                MainController.get().saveCurrentTabs();
            }

            mWebRenderer.resetPageInspector();

            final Context context = getContext();
            PackageManager packageManager = context.getPackageManager();

            URL currentUrl = mWebRenderer.getUrl();

            List<ResolveInfo> tempResolveInfos = new ArrayList<>();
            if (!currentUrl.toString().equals(mContext.getString(R.string.empty_bubble_page))) {
                tempResolveInfos = Settings.get().getAppsThatHandleUrl(currentUrl.toString(), getContext().getPackageManager());
            }

            updateAppsForUrl(tempResolveInfos, currentUrl);
            if (Settings.get().redirectUrlToBrowser(currentUrl)) {
                CrashTracking.log("ContentView.onPageStarted() - url redirects to browser");
                if (openInBrowser(urlAsString)) {
                    String title = String.format(context.getString(R.string.link_redirected), Settings.get().getDefaultBrowserLabel());
                    MainApplication.saveUrlInHistory(context, null, urlAsString, title);
                    return;
                }
            }

            if (mHandledAppPickerForCurrentUrl == false
                    && mUsingLinkBubbleAsDefaultForCurrentUrl == false
                    && mAppsForUrl != null
                    && mAppsForUrl.size() > 0
                    && Settings.get().didRecentlyRedirectToApp(urlAsString) == false) {

                AppForUrl defaultAppForUrl = getDefaultAppForUrl();
                if (defaultAppForUrl != null) {
                    if (Util.isLinkBubbleResolveInfo(defaultAppForUrl.mResolveInfo)) {
                        mUsingLinkBubbleAsDefaultForCurrentUrl = true;
                    } else {
                        if (openInApp(defaultAppForUrl.mResolveInfo, urlAsString)) {
                            return;
                        }
                    }
                } else {
                    boolean isLinkBubblePresent = false;
                    //boolean isLinkBubblePresent = mAppsForUrl.size() == 1 ? Util.isLinkBubbleResolveInfo(mAppsForUrl.get(0).mResolveInfo) : false;
                    for (AppForUrl info : mAppsForUrl) {

                        // Handle crash: https://fabric.io/brave6/android/apps/com.linkbubble.playstore/issues/562667c7f5d3a7f76bf16a4c
                        if (info.mResolveInfo == null || info.mResolveInfo.activityInfo == null) {
                            CrashTracking.log("onPageStarted() Null resolveInfo when getting default for app: " + info);
                            continue;
                        }

                        if (info.mResolveInfo.activityInfo.packageName.startsWith("com.linkbubble.playstore")
                                || info.mResolveInfo.activityInfo.packageName.startsWith("com.brave.playstore")) {
                            isLinkBubblePresent = true;
                            break;
                        }
                    }

                    if (isLinkBubblePresent == false && MainApplication.sShowingAppPickerDialog == false &&
                            mHandledAppPickerForCurrentUrl == false && mAppPickersUrls.contains(urlAsString) == false) {
                        final ArrayList<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
                        for (AppForUrl appForUrl : mAppsForUrl) {
                            if (appForUrl.mResolveInfo != null) {
                                resolveInfos.add(appForUrl.mResolveInfo);
                            }
                        }
                        if (0 != resolveInfos.size()) {
                            AlertDialog dialog = ActionItem.getActionItemPickerAlert(context, resolveInfos, R.string.pick_default_app,
                                    new ActionItem.OnActionItemDefaultSelectedListener() {
                                        @Override
                                        public void onSelected(ActionItem actionItem, boolean always) {
                                            CrashTracking.log("onPageStarted(): OnActionItemDefaultSelectedListener.onSelected()");
                                            boolean loaded = false;
                                            String appPackageName = context.getPackageName();
                                            for (ResolveInfo resolveInfo : resolveInfos) {
                                                if (resolveInfo.activityInfo.packageName.equals(actionItem.mPackageName)
                                                        && resolveInfo.activityInfo.name.equals(actionItem.mActivityClassName)) {
                                                    if (always) {
                                                        Settings.get().setDefaultApp(urlAsString, resolveInfo);
                                                    }

                                                    // Jump out of the loop and load directly via a BubbleView below
                                                    if (resolveInfo.activityInfo.packageName.equals(appPackageName)) {
                                                        break;
                                                    }

                                                    mInitialUrlLoadStartTime = -1;
                                                    loaded = MainApplication.loadIntent(context, actionItem.mPackageName,
                                                            actionItem.mActivityClassName, urlAsString, -1, true);
                                                    break;
                                                }
                                            }

                                            if (loaded) {
                                                if (MainController.get() != null) {
                                                    MainController.get().closeTab(mOwnerTabView, MainController.get().contentViewShowing(), false);
                                                }
                                                Settings.get().addRedirectToApp(urlAsString);
                                            }
                                            // NOTE: no need to call loadUrl(urlAsString) or anything in the event the link is to be handled by
                                            // Link Bubble. The flow already assumes that will happen by continuing the load when the Dialog displays. #244
                                        }
                                    });

                            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    MainApplication.sShowingAppPickerDialog = false;
                                }
                            });

                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.show();
                            MainApplication.sShowingAppPickerDialog = true;
                            mHandledAppPickerForCurrentUrl = true;
                            mAppPickersUrls.add(urlAsString);
                        }
                    }
                }
            }

            configureOpenInAppButton();
            configureOpenEmbedButton();
            configureArticleModeButton();
            Log.d(TAG, "redirect to url: " + urlAsString);
            mEventHandler.onPageLoading(mWebRenderer.getUrl());
            updateUrlTitleAndText(urlAsString);

            if (mShareButton.getVisibility() == GONE) {
                mShareButton.setVisibility(VISIBLE);
            }

            if ((urlAsString.equals(Constant.WELCOME_MESSAGE_URL) ||
                    urlAsString.equals(getContext().getString(R.string.empty_bubble_page)) ) && mController != null) {
                mController.displayTab(mOwnerTabView);
            }
        }

        @Override
        public void onPageFinished(String urlAsString) {
            if (mLifeState != LifeState.Alive) {
                return;
            }
            if (mIgnoreNextOnPageFinished) {
                mIgnoreNextOnPageFinished = false;
                Log.d(TAG, "onPageFinished() - ignoring because of mIgnoreNextOnPageFinished...");
                return;
            }

            Integer debugIndex = MainController.get() != null ? MainController.get().getTabIndex(mOwnerTabView) : null;
            CrashTracking.log("onPageFinished(), " + (debugIndex != null ? "index:" + debugIndex : "<MainController.get() == null>"));

            // This should not be necessary, but unfortunately is.
            // Often when pressing Back, onPageFinished() is mistakenly called when progress is 0. #245
            if (mCurrentProgress != 100) {
                mPageFinishedIgnoredUrl = urlAsString;
                return;
            }

            onPageLoadComplete(urlAsString);
            if (null != MainController.get() && MainController.get().getCurrentTab() != mOwnerTabView) {
                mWebRenderer.pauseOnSetInactive();
            }
            if (mUrlTextView.getText().toString().equals(getContext().getString(R.string.empty_bubble_page))) {
                mTitleTextView.performClick();
            }
        }

        @Override
        public void onDownloadStart(String urlAsString) {
            ContentView.this.onDownloadStart(urlAsString);
        }

        @Override
        public void onReceivedTitle(String url, String title) {
            if (title == null || title.isEmpty()) {
                return;
            }

            if (url != null && url.equals(getContext().getString(R.string.empty_bubble_page))) {
                mTitleTextView.setTextColor(0xFFFFFFFF);
            }
            mTitleTextView.setText(title);
            if (MainApplication.sTitleHashMap != null && url != null) {
                MainApplication.sTitleHashMap.put(url, title);
            }
        }

        @Override
        public void onReceivedIcon(Bitmap bitmap) {

            // Only pass this along if the page has finished loading (https://github.com/brave/LinkBubble/issues/155).
            // This is to prevent passing a stale icon along when a redirect has already occurred. This shouldn't cause
            // too many ill-effects, because BitmapView attempts to load host/favicon.ico automatically anyway.
            if (mPageFinishedLoading) {
                if (mEventHandler.onReceivedIcon(bitmap)) {
                    String faviconUrl = Util.getDefaultFaviconUrl(mWebRenderer.getUrl());
                    MainApplication.sFavicons.putFaviconInMemCache(faviconUrl, bitmap);
                }
            }
        }

        // Hacky variables to get around version 40 of Android System WebView returning "about:blank"
        // urls.
        // * mIgnoreNextOnProgressChanged is necessary to ignore the 100 progress that comes in with a
        //      null urlAsString.
        // * mIgnoreNextOnPageFinished is necessary to ignore the ensuing onPageFinished() call.
        //
        // Both of these hacks combine to allow links to load correctly using WebView, and have the
        // progress indicator display as expected.
        boolean mIgnoreNextOnProgressChanged = false;
        boolean mIgnoreNextOnPageFinished = false;
        @Override
        public void onProgressChanged(int progress, String urlAsString) {
            if (urlAsString == null) {
                Log.d(TAG, "onProgressChanged(): ignore, no url");
                mIgnoreNextOnProgressChanged = true;
                return;
            } else if (mIgnoreNextOnProgressChanged) {
                Log.d(TAG, "onProgressChanged(): ignoring next value...");
                mIgnoreNextOnProgressChanged = false;
                mIgnoreNextOnPageFinished = true;
                return;
            }

            Log.d(TAG, "onProgressChanged() - progress:" + progress + ", " + urlAsString);

            mCurrentProgress = progress;

            // Note: annoyingly, onProgressChanged() can be called with values from a previous url.
            // Eg, "http://t.co/fR9bzpvyLW" redirects to "http://on.recode.net/1eOqNVq" which redirects to
            // "http://recode.net/2014/01/20/...", and after the "on.recode.net" redirect, progress is 100 for a moment.
            mEventHandler.onProgressChanged(progress);

            if (progress == 100 && mPageFinishedIgnoredUrl != null && mPageFinishedIgnoredUrl.equals(urlAsString)) {
                onPageLoadComplete(urlAsString);
            }
        }

        @Override
        public boolean onBackPressed() {
            return ContentView.this.onBackPressed();
        }

        @Override
        public void onUrlLongClick(WebView webView, String url, int type) {
            ContentView.this.onUrlLongClick(webView, url, type);
        }

        @Override
        public void onShowBrowserPrompt() {
            ContentView.this.onShowBrowserPrompt();
        }

        @Override
        public void onCloseWindow() {
            CrashTracking.log("WebRenderer.Controller.onCloseWindow()");
            if (MainController.get() != null) {
                MainController.get().closeTab(mOwnerTabView, true, true);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, WebRenderer.GetGeolocationCallback callback) {
            showAllowLocationDialog(origin, callback);
        }

        private Handler mHandler = new Handler();
        private Runnable mUpdateOpenInAppRunnable = null;

        @Override
        public void onPageInspectorYouTubeEmbedFound() {
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

        @Override
        public void onPageInspectorTouchIconLoaded(final Bitmap bitmap, final String pageUrl) {
            if (bitmap == null || pageUrl == null) {
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    URL url = mWebRenderer.getUrl();
                    if (url != null && url.toString().equals(pageUrl)) {
                        mEventHandler.onReceivedIcon(bitmap);

                        String faviconUrl = Util.getDefaultFaviconUrl(url);
                        MainApplication.sFavicons.putFaviconInMemCache(faviconUrl, bitmap);
                    }
                }
            });
        }

        @Override
        public void onPageInspectorDropDownWarningClick() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showOpenInBrowserPrompt(R.string.unsupported_drop_down_default_browser,
                            R.string.unsupported_drop_down_no_default_browser, mWebRenderer.getUrl().toString());
                }
            });
        }

        @Override
        public void onPagedInspectorThemeColorFound(final int color) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateColors(color);
                    mEventHandler.onThemeColor(color);
                }
            });
        }

        @Override
        public void onArticleContentReady(ArticleContent articleContent) {
            Log.d("Article", "onArticleContentReady() - " + (articleContent == null ? "<null>" : "valid"));
            configureArticleModeButton();
        }

    };

    private String mPageFinishedIgnoredUrl;

    void onPageLoadComplete(String urlAsString) {

        mPageFinishedLoading = true;

        // NOTE: *don't* call updateUrl() here. Turns out, this function is called after a redirect has occurred.
        // Eg, urlAsString "t.co/xyz" even after the next redirect is starting to load

        // Check exact equality first for common case to avoid an allocation.
        URL currentUrl = mWebRenderer.getUrl();
        boolean equalUrl = currentUrl.toString().equals(urlAsString);

        if (!equalUrl) {
            try {
                URL url = new URL(urlAsString);

                if (url.getProtocol().equals(currentUrl.getProtocol()) &&
                        url.getHost().equals(currentUrl.getHost()) &&
                        url.getPath().equals(currentUrl.getPath())) {
                    equalUrl = true;
                }
            } catch (MalformedURLException e) {
            }
        }

        mWebRenderer.runPageInspector(mWebRendererController.adInsertionList(currentUrl.getHost().replace("www.", "").replace("m.", "")));

        if (equalUrl) {
            updateAppsForUrl(currentUrl);
            configureOpenInAppButton();
            configureOpenEmbedButton();
            configureArticleModeButton();

            mEventHandler.onPageLoaded(false);
            Log.e(TAG, "onPageLoadComplete() - url: " + urlAsString);

            String title = MainApplication.sTitleHashMap != null ? MainApplication.sTitleHashMap.get(urlAsString) : "";
            if (TextUtils.isEmpty(title)) {
                // Note: it's possible for title == null above, but there be a valid title in the following case:
                // * title set for http://url.com/page?arg=1
                // * urlAsString now changed to http://url.com/page, which isn't in sTitleHashMap
                // In this case, if there's a valid title, keep using it.
                if (mTitleTextView.getText() != null) {
                    String currentTitle = mTitleTextView.getText().toString();
                    if (currentTitle.equals(mLoadingString) == false) {
                        title = currentTitle;
                    }
                }

                // If no title is set, display nothing rather than "Loading..." #265
                if (title == null) {
                    mTitleTextView.setText(null);
                }
            }

            if (!currentUrl.toString().equals(getContext().getString(R.string.empty_bubble_page))) {
                Settings settings = Settings.get();
                if (null != settings && !settings.isIncognitoMode()) {
                    // Adding the URL to the auto suggestions list
                    mAdapter.addUrlToAutoSuggestion(currentUrl.toString());
                    MainApplication.saveUrlInHistory(getContext(), null, currentUrl.toString(), currentUrl.getHost(), title);
                }

            }
            else {
                mTitleTextView.performClick();
            }
            //mDelayedAutoContentDisplayLinkLoadedScheduled = true;
            //Log.d(TAG, "set mDelayedAutoContentDisplayLinkLoadedScheduled=" + mDelayedAutoContentDisplayLinkLoadedScheduled);
            postDelayed(mDelayedAutoContentDisplayLinkLoadedRunnable, Constant.AUTO_CONTENT_DISPLAY_DELAY);

            mWebRenderer.onPageLoadComplete();
            mWebRenderer.getView().requestFocus();
        }

        mPageFinishedIgnoredUrl = null;
    }

    //boolean mDelayedAutoContentDisplayLinkLoadedScheduled = false;

    // Call autoContentDisplayLinkLoaded() via a delay so as to fix #412
    Runnable mDelayedAutoContentDisplayLinkLoadedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLifeState == LifeState.Alive && MainController.get() != null) {
                //Log.e(TAG, "*** set mDelayedAutoContentDisplayLinkLoadedScheduled=" + mDelayedAutoContentDisplayLinkLoadedScheduled);
                MainController.get().autoContentDisplayLinkLoaded(mOwnerTabView);
                saveLoadTime();
            }
        }
    };

    OnClickListener mOnShareButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showSelectShareMethod(mWebRenderer.getUrl().toString(), true);
        }
    };

    OnClickListener mbtClearUrlClicked = new View.OnClickListener() {
        public void onClick(View v) {
            metUrl.setText("");
            mbtUrlClear.setEnabled(false);
            mbtUrlClear.getBackground().setAlpha(50);
        }
    };

    OnFocusChangeListener murlOnFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (!b) {
                // Show the toolbar again if lost focus and hide the soft keyboard
                findViewById(R.id.content_toolbar).bringToFront();

                InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(metUrl.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        }
    };

    AdapterView.OnItemClickListener murlOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // Hide the soft keyboard
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(metUrl.getWindowToken(),
                    InputMethodManager.RESULT_UNCHANGED_SHOWN);

            SearchURLSuggestions urlSuggestion = (SearchURLSuggestions)adapterView.getItemAtPosition(i);

            WorkWithURL(urlSuggestion.Name, urlSuggestion.EngineToUse, false);
        }
    };

    TextView.OnEditorActionListener murlActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(metUrl.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);

                String urlText = metUrl.getText().toString();
                String urlToCheck = urlText;
                if (!urlToCheck.startsWith(getContext().getString(R.string.http_prefix)) &&
                        !urlToCheck.startsWith(getContext().getString(R.string.https_prefix)))
                    urlToCheck = getContext().getString(R.string.http_prefix) + urlToCheck;
                if (Util.isValidURL(getContext(), urlToCheck)) {
                    WorkWithURL(urlText, SearchURLSuggestions.SearchEngine.NONE, true);
                }
                else if (null != mFirstSuggestedItem) {
                    String strUrl = mFirstSuggestedItem.Name;

                    WorkWithURL(strUrl, SearchURLSuggestions.SearchEngine.NONE, true);
                }
            }

            return false;
        }
    };

    EditText.OnKeyListener murlKeyListener = new EditText.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return ContentView.this.onBackPressed();
                }
            }
            return false;
        }
    };

    DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (mAdapter.getCount() > 0) {
                mFirstSuggestedItem = (SearchURLSuggestions)mAdapter.getItem(0);
            }
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            int pixels = 0;
            if (mAdapter.getCount() > mRowsToShowOnAutoSuggestions) {
                float dp = mOneRowAutoSuggestionsSize * mRowsToShowOnAutoSuggestions;
                float fpixels = metrics.density * dp;
                pixels = (int) (fpixels + 0.5f);
            }
            else {
                float dp = mOneRowAutoSuggestionsSize;
                float fpixels = metrics.density * dp;
                pixels = (int) (fpixels + 0.5f) * mAdapter.getCount();
            }

            metUrl.setDropDownHeight(pixels);

            // Set an autosuggestion
            String urlText = metUrl.getText().toString();
            if (mApplyAutoSuggestionToUrlString && 0 != urlText.length() && null != mFirstSuggestedItem &&
                    SearchURLSuggestions.SearchEngine.NONE == mFirstSuggestedItem.EngineToUse) {
                String suggestedString = mFirstSuggestedItem.Name;
                String stringToAppend = "";
                if (suggestedString.length() > urlText.length()) {
                    stringToAppend = suggestedString.substring(urlText.length());
                    String toCompare = suggestedString.substring(0, urlText.length());
                    if (toCompare.equals(urlText)) {
                        mSetTheRealUrlString = false;
                        metUrl.setText(urlText + stringToAppend);
                        mSetTheRealUrlString = true;
                        metUrl.setSelection(urlText.length(), urlText.length() + stringToAppend.length());
                    }
                }
            }
        }
    };

    TextWatcher murlTextWatcher = new TextWatcher() {
        private String mBeforeTextString;
        private boolean mApplyAutoSuggestion = true;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            mBeforeTextString = metUrl.getText().toString();
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String urlText = metUrl.getText().toString();
            if (!mFirstTimeUrlTyped &&
                    (urlText.equals(mAdapter.mRealUrlBarConstraint) || mAdapter.mRealUrlBarConstraint.length() > urlText.length())) {
                mApplyAutoSuggestion = false;
            }
            else {
                mApplyAutoSuggestion = true;
            }
            if (mSetTheRealUrlString) {
                mAdapter.mRealUrlBarConstraint = urlText;
            }
            mFirstTimeUrlTyped = false;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String urlText = metUrl.getText().toString();
            if (!mApplyAutoSuggestion) {
                mApplyAutoSuggestionToUrlString = false;
            }
            else {
                mApplyAutoSuggestionToUrlString = true;
            }
            if (urlText.length() != 0) {
                mbtUrlClear.setEnabled(true);
                mbtUrlClear.getBackground().setAlpha(255);
            }
            else {
                mbtUrlClear.setEnabled(false);
                mbtUrlClear.getBackground().setAlpha(50);
            }
        }
    };

    OnClickListener mOnURLEnterClicked = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mWebRenderer.getUrl().toString().equals(getContext().getString(R.string.empty_bubble_page))) {
                metUrl.setText(mWebRenderer.getUrl().toString());
            }
            else {
                metUrl.setText("");
            }
            mFirstTimeUrlTyped = true;
            // Bring the search URL layout on top
            findViewById(R.id.content_edit_url).bringToFront();

            // Request the focus for the search URL control
            metUrl.requestFocus();
            metUrl.selectAll();
            // Show the soft keyboard
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            imm.showSoftInput(metUrl,InputMethodManager.SHOW_FORCED);
        }
    };

    OpenInAppButton.OnOpenInAppClickListener mOnOpenInAppButtonClickListener = new OpenInAppButton.OnOpenInAppClickListener() {

        @Override
        public void onAppOpened() {
            CrashTracking.log("mOnOpenInAppButtonClickListener.onAppOpened()");
            if (MainController.get() != null) {
                MainController.get().closeTab(mOwnerTabView, true, false);
                // L_WATCH: L currently lacks getRecentTasks(), so minimize here
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    MainController.get().switchToBubbleView();
                }
            }
        }

    };

    OpenEmbedButton.OnOpenEmbedClickListener mOnOpenEmbedButtonClickListener = new OpenEmbedButton.OnOpenEmbedClickListener() {

        @Override
        public void onYouTubeEmbedOpened() {

        }
    };

    OnClickListener mOnReloadButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mReloadButton.setVisibility(GONE);
            mWebRenderer.reload();
        }
    };

    ArticleRenderer.Controller mArticleModeController = new ArticleRenderer.Controller() {

        @Override
        public void onUrlLongClick(WebView webView, String url, int type) {
            ContentView.this.onUrlLongClick(webView, url, type);
        }

        @Override
        public void onDownloadStart(String urlAsString) {
            ContentView.this.onDownloadStart(urlAsString);
        }

        @Override
        public boolean onBackPressed() {
            return ContentView.this.onBackPressed();
        }

        @Override
        public void onShowBrowserPrompt() {
            ContentView.this.onShowBrowserPrompt();
        }

        @Override
        public void onFirstPageLoadStarted() {
            // Ugly hack to get ensure the Back button works in Article mode
            if (mArticleModeButton.getState() == ArticleModeButton.State.Web) {
                mWebRenderer.getView().setVisibility(View.INVISIBLE);
            }
        }
    };;

    OnClickListener mOnArticleModeButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mArticleModeButton.toggleState();
            mArticleModeButton.updateTheme(themeColor);

            ArticleContent articleContent = mWebRenderer.getArticleContent();

            switch (mArticleModeButton.getState()) {
                case Article:
                    if (mArticleRenderer != null && mArticleRenderer.getView() != null) {
                        mArticleRenderer.getView().setVisibility(View.INVISIBLE);
                    }
                    mWebRenderer.getView().setVisibility(View.VISIBLE);
                    break;

                case Web:
                    if (mArticleRenderer == null) {
                        View articleRendererPlaceholder = findViewById(R.id.article_renderer_placeholder);
                        mArticleRenderer = new ArticleRenderer(getContext(), mArticleModeController, articleContent, articleRendererPlaceholder);
                    } else {
                        mArticleRenderer.display(articleContent);
                        mWebRenderer.getView().setVisibility(View.INVISIBLE);
                    }
                    mArticleRenderer.getView().setVisibility(VISIBLE);
                    break;
            }
        }
    };

    OnClickListener mOnOverflowButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Context context = getContext();
            mOverflowPopupMenu = new PopupMenu(context, mOverflowButton);
            Resources resources = context.getResources();
            final MenuItem siteProtection = mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_site_protection, Menu.NONE, resources.getString(R.string.action_site_protection))
                    .setCheckable(true)
                    .setChecked(!mHostInWhiteList);
            if (mCurrentProgress != 100) {
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_stop, Menu.NONE, resources.getString(R.string.action_stop));
            }
            mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_reload_page, Menu.NONE, resources.getString(R.string.action_reload_page));

            String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
            if (defaultBrowserLabel != null) {
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_open_in_browser, Menu.NONE,
                        String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel));
            }

            final MenuItem requestDesktopSite = mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_request_desktop_site, Menu.NONE, resources.getString(R.string.action_request_desktop_site))
                    .setCheckable(true)
                    .setChecked(mWebRenderer.getUserAgentString(mContext).equals(Constant.USER_AGENT_CHROME_DESKTOP));

            mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_copy_link, Menu.NONE, resources.getString(R.string.action_copy_to_clipboard));
            mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_new_bubble, Menu.NONE, resources.getString(R.string.action_new_bubble));
            mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_close_tab, Menu.NONE, resources.getString(R.string.action_close_tab));
            mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_settings, Menu.NONE, resources.getString(R.string.action_settings));
            mOverflowPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.item_site_protection: {
                            CrashTracking.log("R.id.item_site_protection");
                            // This looks backwards, but is correct, as isChecked() isn't true until
                            // after onMenuItemClick() is called.
                            AddRemoveHostFromWhiteList(mWebRenderer.getUrl().toString(), siteProtection.isChecked());
                            HostInWhiteListCheck(mWebRenderer.getUrl().toString());
                            // We need to go to reload page case.
                        }

                        case R.id.item_reload_page: {
                            CrashTracking.log("R.id.item_reload_page");
                            mWebRenderer.resetPageInspector();
                            URL currentUrl = mWebRenderer.getUrl();
                            mEventHandler.onPageLoading(currentUrl);
                            mWebRenderer.stopLoading();
                            mWebRenderer.reload();
                            String urlAsString = currentUrl.toString();
                            updateAppsForUrl(currentUrl);
                            configureOpenInAppButton();
                            configureOpenEmbedButton();
                            configureArticleModeButton();
                            Log.d(TAG, "reload url: " + urlAsString);
                            mInitialUrlLoadStartTime = System.currentTimeMillis();
                            updateUrlTitleAndText(urlAsString);
                            break;
                        }

                        case R.id.item_open_in_browser: {
                            CrashTracking.log("ContentView.setOnMenuItemClickListener() - open in browser clicked");
                            openInBrowser(mWebRenderer.getUrl().toString(), true);
                            break;
                        }

                        case R.id.item_request_desktop_site: {
                            String newUserAgentString;
                            // This looks backwards, but is correct, as isChecked() isn't true until
                            // after onMenuItemClick() is called.
                            if (!requestDesktopSite.isChecked()) {
                                newUserAgentString = Constant.USER_AGENT_CHROME_DESKTOP;
                            } else {
                                String defaultUserAgentString = Settings.get().getUserAgentString();
                                if (defaultUserAgentString != null
                                        && !defaultUserAgentString.equals(Constant.USER_AGENT_CHROME_DESKTOP)) {
                                    newUserAgentString = defaultUserAgentString;
                                } else {
                                    newUserAgentString = Util.getDefaultUserAgentString(getContext());
                                }
                            }

                            mWebRenderer.setUserAgentString(newUserAgentString);
                            mWebRenderer.reload();
                            break;
                        }

                        case R.id.item_copy_link: {
                            MainApplication.copyLinkToClipboard(getContext(), mWebRenderer.getUrl().toString(), R.string.bubble_link_copied_to_clipboard);
                            break;
                        }

                        case R.id.item_stop: {
                            mWebRenderer.stopLoading();
                            break;
                        }

                        case R.id.item_new_bubble: {
                            //to do debug
                            //Intent intent = new Intent(getContext(), BubbleFlowActivity.class);
                            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            //getContext().startActivity(intent);
                            //
                            MainApplication.openLink(getContext(), getContext().getString(R.string.empty_bubble_page),
                                    Analytics.OPENED_URL_FROM_NEW_TAB);
                            break;
                        }

                        case R.id.item_close_tab: {
                            CrashTracking.log("R.id.item_close_tab");
                            if (MainController.get() != null) {
                                MainController.get().closeTab(mOwnerTabView, MainController.get().contentViewShowing(), true);
                            }
                            break;
                        }

                        case R.id.item_settings: {
                            Intent intent = new Intent(context, SettingsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            context.startActivity(intent);
                            MainController.get().switchToBubbleView();
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
    };

    OnSwipeTouchListener mOnTextContainerTouchListener = new OnSwipeTouchListener() {
        public void onSwipeRight() {
            MainController.get().showPreviousBubble();
        }
        public void onSwipeLeft() {
            MainController.get().showNextBubble();
        }
    };

    private void onDownloadStart(String urlAsString) {
        CrashTracking.log("onDownloadStart()");
        openInBrowser(urlAsString);
        if (MainController.get() != null) {
            MainController.get().closeTab(mOwnerTabView, true, false);
            // L_WATCH: L currently lacks getRecentTasks(), so minimize here
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                MainController.get().switchToBubbleView();
            }
        }
    }

    private boolean onBackPressed() {
        if (mUrlStack.size() <= 1) {
            CrashTracking.log("onBackPressed() - closeTab()");
            if (MainController.get() != null) {
                MainController.get().closeTab(mOwnerTabView, BubbleAction.BackButton, true, true);
            }
            return true;
        } else {
            CrashTracking.log("onBackPressed() - go back");
            mWebRenderer.stopLoading();
            String urlBefore = mWebRenderer.getUrl().toString();

            mUrlStack.pop();
            URL previousUrl = mUrlStack.peek();
            String previousUrlAsString = previousUrl.toString();
            mEventHandler.onCanGoBackChanged(mUrlStack.size() > 1);
            mHandledAppPickerForCurrentUrl = false;
            mUsingLinkBubbleAsDefaultForCurrentUrl = false;
            Log.d(TAG, "[urlstack] Go back: " + urlBefore + " -> " + mWebRenderer.getUrl() + ", urlStack.size():" + mUrlStack.size());
            updateAndLoadUrl(previousUrlAsString);
            updateUrlTitleAndText(previousUrlAsString);

            mEventHandler.onPageLoading(mWebRenderer.getUrl());

            updateAppsForUrl(null, previousUrl);
            configureOpenInAppButton();
            configureArticleModeButton();

            mWebRenderer.resetPageInspector();
            configureOpenEmbedButton();
            // The WebView doesn't reload on all pages correctly if call only loadUrl, seems like there is some kind of cache as
            // it loads fast on back but doesn't load pictures for thestar.com website. clearCache method doesn't work also. Only
            // reload works nice here. Perhaps it is some bu in API as lots of people say that problem with loadUrl method
            // That is the temp fix, thestar.com has a new beta website and it works great with it without reloading
            if (previousUrlAsString.endsWith("m.thestar.com/#/?referrer=")) {
                mWebRenderer.reload();
            }

            return true;
        }
    }

    public void onShowBrowserPrompt() {
        showOpenInBrowserPrompt(R.string.long_press_unsupported_default_browser,
                R.string.long_press_unsupported_no_default_browser, mWebRenderer.getUrl().toString());

    }

    private void onUrlLongClick(final WebView webView, final String urlAsString, final int type) {
        Resources resources = getResources();

        final ArrayList<String> longClickSelections = new ArrayList<String>();

        final String shareLabel = resources.getString(R.string.action_share);
        longClickSelections.add(shareLabel);

        String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();

        final String leftConsumeBubbleLabel = Settings.get().getConsumeBubbleLabel(BubbleAction.ConsumeLeft);
        if (leftConsumeBubbleLabel != null) {
            if (defaultBrowserLabel == null || defaultBrowserLabel.equals(leftConsumeBubbleLabel) == false) {
                longClickSelections.add(leftConsumeBubbleLabel);
            }
        }

        final String rightConsumeBubbleLabel = Settings.get().getConsumeBubbleLabel(BubbleAction.ConsumeRight);
        if (rightConsumeBubbleLabel != null) {
            if (defaultBrowserLabel == null || defaultBrowserLabel.equals(rightConsumeBubbleLabel) == false) {
                longClickSelections.add(rightConsumeBubbleLabel);
            }
        }

        // Long pressing for a link doesn't work reliably, re #279
        //final String copyLinkLabel = resources.getString(R.string.action_copy_to_clipboard);
        //longClickSelections.add(copyLinkLabel);

        Collections.sort(longClickSelections);

        final String openLinkInNewBubbleLabel= resources.getString(R.string.action_open_link_in_new_bubble);
        final String openImageInNewBubbleLabel  = resources.getString(R.string.action_open_image_in_new_bubble);
        if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            longClickSelections.add(0, openImageInNewBubbleLabel);
        }
        if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            longClickSelections.add(0, openLinkInNewBubbleLabel);
        }

        final String openInBrowserLabel = defaultBrowserLabel != null ?
                String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel) : null;
        if (openInBrowserLabel != null) {
            longClickSelections.add(1, openInBrowserLabel);
        }

        final String saveImageLabel = resources.getString(R.string.action_save_image);
        if (type == WebView.HitTestResult.IMAGE_TYPE ||
                type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            longClickSelections.add(saveImageLabel);
        }

        ListView listView = new ListView(getContext());
        listView.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
                longClickSelections.toArray(new String[0])));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CrashTracking.log("ContentView listView.setOnItemClickListener");
                String string = longClickSelections.get(position);
                if (string.equals(openLinkInNewBubbleLabel) && type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    Message msg = new Message();
                    msg.setTarget(new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            Bundle b = msg.getData();
                            if (b != null && b.getString("url") != null) {
                                MainController.get().openUrl(b.getString("url"), System.currentTimeMillis(), false, Analytics.OPENED_URL_FROM_NEW_TAB);
                            }
                        }
                    });
                    webView.requestFocusNodeHref(msg);
                } if (string.equals(openLinkInNewBubbleLabel) || string.equals(openImageInNewBubbleLabel)) {
                    //MainController.get().openUrl(urlAsString, System.currentTimeMillis(), false, Analytics.OPENED_URL_FROM_NEW_TAB);
                    MainApplication.openLink(getContext(), urlAsString,
                            Analytics.OPENED_URL_FROM_NEW_TAB);
                } else if (openInBrowserLabel != null && string.equals(openInBrowserLabel)) {
                    openInBrowser(urlAsString);
                } else if (string.equals(shareLabel)) {
                    showSelectShareMethod(urlAsString, false);
                } else if (string.equals(saveImageLabel)) {
                    saveImage(urlAsString);
                } else if (leftConsumeBubbleLabel != null && string.equals(leftConsumeBubbleLabel)) {
                    MainApplication.handleBubbleAction(getContext(), BubbleAction.ConsumeLeft, urlAsString, -1);
                } else if (rightConsumeBubbleLabel != null && string.equals(rightConsumeBubbleLabel)) {
                    MainApplication.handleBubbleAction(getContext(), BubbleAction.ConsumeRight, urlAsString, -1);
                //} else if (string.equals(copyLinkLabel)) {
                //    MainApplication.copyLinkToClipboard(getContext(), urlAsString, R.string.link_copied_to_clipboard);
                }

                if (mLongPressAlertDialog != null) {
                    mLongPressAlertDialog.dismiss();
                }
            }
        });
        listView.setBackgroundColor(Settings.get().getThemedContentViewColor());

        mLongPressAlertDialog = new AlertDialog.Builder(getContext()).create();
        mLongPressAlertDialog.setView(listView);
        mLongPressAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mLongPressAlertDialog.show();
    }

    private void configureOpenEmbedButton() {
        if (mOpenEmbedButton.configure(mWebRenderer.getPageInspectorYouTubeEmbedHelper())) {
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

    private void configureArticleModeButton() {
        ArticleContent articleContent = mWebRenderer.getArticleContent();
        if (articleContent != null) {
            if (mArticleNotificationId == -1 && TextUtils.isEmpty(articleContent.mText) == false && Settings.get().getArticleModeOnWearEnabled()) {
                mArticleNotificationId = sNextArticleNotificationId;
                sNextArticleNotificationId++;

                String title = MainApplication.sTitleHashMap != null ? MainApplication.sTitleHashMap.get(articleContent.mUrl.toString()) : "Open Bubble";

                Context context = getContext();

                Intent closeTabIntent = new Intent(context, NotificationCloseTabActivity.class);
                closeTabIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                closeTabIntent.putExtra(NotificationCloseTabActivity.EXTRA_DISMISS_NOTIFICATION, mArticleNotificationId);
                PendingIntent closeTabPendingIntent =
                        PendingIntent.getActivity(context, mArticleNotificationId, closeTabIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Intent openTabIntent = new Intent(context, NotificationOpenTabActivity.class);
                openTabIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
                openTabIntent.putExtra(NotificationOpenTabActivity.EXTRA_DISMISS_NOTIFICATION, mArticleNotificationId);
                PendingIntent openTabPendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), openTabIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(context)
                        .addAction(R.drawable.ic_action_cancel_white, context.getString(R.string.action_close_tab), closeTabPendingIntent)
                        .setContentTitle(title)
                        .setContentText(articleContent.mText)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setGroup(Constant.NOTIFICATION_GROUP_KEY_ARTICLES)
                        .setContentIntent(openTabPendingIntent)
                        .build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(mArticleNotificationId, notification);
            }

            if (Settings.get().getArticleModeEnabled()) {
                mArticleModeButton.setVisibility(VISIBLE);
            } else {
                mArticleModeButton.setVisibility(GONE);
            }
        } else {
            mArticleModeButton.setVisibility(GONE);
            if (mArticleRenderer != null) {
                mArticleRenderer.stopLoading();
            }
            cancelWearNotification();
        }
    }

    private void cancelWearNotification() {
        if (mArticleNotificationId > -1) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
            notificationManager.cancel(mArticleNotificationId);
            mArticleNotificationId = -1;
        }
    }

    private void updateAppsForUrl(URL url) {
        String urlString = url.toString();
        List<ResolveInfo> tempResolveInfos = new ArrayList<>();
        if (!urlString.equals(mContext.getString(R.string.empty_bubble_page))) {
            tempResolveInfos = Settings.get().getAppsThatHandleUrl(urlString, getContext().getPackageManager());
        }
        final List<ResolveInfo> resolveInfos = tempResolveInfos;
        updateAppsForUrl(resolveInfos, url);
    }

    private void updateAppsForUrl(List<ResolveInfo> resolveInfos, URL url) {
        if (resolveInfos != null && resolveInfos.size() > 0) {
            mTempAppsForUrl.clear();
            for (ResolveInfo resolveInfoToAdd : resolveInfos) {
                if (resolveInfoToAdd.activityInfo != null) {
                    boolean alreadyAdded = false;
                    for (int i = 0; i < mAppsForUrl.size(); i++) {
                        AppForUrl existing = mAppsForUrl.get(i);

                        // In certain situations mResolveInfo is null, likely because we can't find the app.
                        // One possibility is that this happens when the app is currently being updated through the play store.
                        // Prevents crash: https://fabric.io/brave6/android/apps/com.linkbubble.playstore/issues/55dcee53e0d514e5d6413e8d
                        if (existing.mResolveInfo == null) {
                            continue;
                        }

                        if (existing.mResolveInfo.activityInfo.packageName.equals(resolveInfoToAdd.activityInfo.packageName)
                                && existing.mResolveInfo.activityInfo.name.equals(resolveInfoToAdd.activityInfo.name)) {
                            alreadyAdded = true;
                            if (existing.mUrl.equals(url) == false) {
                                if (url.getHost().contains(existing.mUrl.getHost())
                                        && url.getHost().length() > existing.mUrl.getHost().length()) {
                                    // don't update the url in this case. This means prevents, as an example, saving a host like
                                    // "mobile.twitter.com" instead of using "twitter.com". This occurs when loading
                                    // "https://twitter.com/lokibartleby/status/412160702707539968" with Tweet Lanes
                                    // and the official Twitter client installed.
                                } else {
                                    try {
                                        existing.mUrl = new URL(url.toString());   // Update the Url
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException("Malformed URL: " + url);
                                    }
                                }
                            }
                            break;
                        }
                    }

                    if (alreadyAdded == false) {
                        //if (resolveInfoToAdd.activityInfo.packageName.equals(Settings.get().mLinkBubbleEntryActivityResolveInfo.activityInfo.packageName)) {
                        //    continue;
                        //}
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

        boolean containsLinkBubble = false;
        for (AppForUrl appForUrl : mAppsForUrl) {
            if (appForUrl.mResolveInfo != null
                    && appForUrl.mResolveInfo.activityInfo != null
                    && appForUrl.mResolveInfo.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID)) {
                containsLinkBubble = true;
                break;
            }
        }

        if (containsLinkBubble == false) {
            mAppsForUrl.add(new AppForUrl(Settings.get().mLinkBubbleEntryActivityResolveInfo, url));
        }
    }

    AppForUrl getDefaultAppForUrl() {
        if (mAppsForUrl != null && mAppsForUrl.size() > 0) {
            mTempAppsForUrl.clear();
            for (AppForUrl appForUrl : mAppsForUrl) {
                mTempAppsForUrl.add(appForUrl.mResolveInfo);
            }
            if (mTempAppsForUrl.size() > 0) {
                ResolveInfo defaultApp = Settings.get().getDefaultAppForUrl(mWebRenderer.getUrl(), mTempAppsForUrl);
                if (defaultApp != null) {
                    for (AppForUrl appForUrl : mAppsForUrl) {
                        if (appForUrl.mResolveInfo == defaultApp) {
                            return appForUrl;
                        }
                    }
                }
            }
        }

        return null;
    }

    public void onAnimateOnScreen() {
        hidePopups();
        resetButtonPressedStates();
    }

    public void onAnimateOffscreen() {
        hidePopups();
        resetButtonPressedStates();
    }

    public void onBeginBubbleDrag() {
        hidePopups();
        resetButtonPressedStates();
    }

    void onCurrentContentViewChanged(boolean isCurrent) {
        hidePopups();
        resetButtonPressedStates();

        //to do debug
        //if (isCurrent && MainController.get().contentViewShowing()) {
        //    saveLoadTime();
        //}
        //
    }

    public void saveLoadTime() {
        if (mInitialUrlLoadStartTime > -1) {
            Settings.get().trackLinkLoadTime(System.currentTimeMillis() - mInitialUrlLoadStartTime, Settings.LinkLoadType.PageLoad, mWebRenderer.getUrl().toString());
            mInitialUrlLoadStartTime = -1;
        }
    }

    void onOrientationChanged() {
        metUrl.setDropDownWidth(getResources().getDisplayMetrics().widthPixels);
        mAdapter.setDropDownWidth(getResources().getDisplayMetrics().widthPixels);
    }

    private boolean updateUrl(String urlAsString) {
        if (urlAsString.equals("about:blank")) {
            Log.d(TAG, "updateUrl(): ignore url:" + urlAsString);
            return true;
        }
        if (urlAsString.equals(mWebRenderer.getUrl().toString()) == false) {
            try {
                Log.d(TAG, "change url from " + mWebRenderer.getUrl() + " to " + urlAsString);
                HostInWhiteListCheck(urlAsString);
                mWebRenderer.setUrl(mWebRendererController.getHTTPSUrl(urlAsString));
            } catch (MalformedURLException e) {
                return false;
            }
        }

        return true;
    }

    private void updateAndLoadUrl(String urlAsString) {
        mThirdPartyHosts = null;
        updateUrl(urlAsString);
        URL updatedUrl = getUrl();

        WebRenderer.Mode mode = WebRenderer.Mode.Web;
        if (Settings.get().getAutoArticleMode()) {
            String path = updatedUrl.getPath();
            if (path != null && !path.equals("") && !path.equals("/")) {
                mode = WebRenderer.Mode.Article;
            }
        }

        //if (mWebRenderer.getUrl().toString().equals(getContext().getString(R.string.empty_bubble_page))) {
            //mWebRenderer.getView().bringToFront();
        //}


        mWebRenderer.loadUrl(updatedUrl, mode);
    }

    private void cleanVisitedHistory(String urlToLook) {
        String peekUrl = "";
        if (mUrlStack.size() > 0) {
            peekUrl = mUrlStack.peek().toString();
        }
        if (peekUrl.equals(urlToLook)) {
            mUrlStack.pop();
            mEventHandler.onCanGoBackChanged(mUrlStack.size() > 1);
        }
    }

    private URL getUpdatedUrl(String urlAsString, boolean cleanVisitedHistory) {
        URL currentUrl = mWebRenderer.getUrl();
        String currentUrlString = currentUrl.toString();
        if (urlAsString.equals(currentUrl.toString()) == false) {
            if (cleanVisitedHistory) {
                cleanVisitedHistory(currentUrlString);
            }
            try {
                Log.d(TAG, "getUpdatedUrl(): change url from " + currentUrlString + " to " + urlAsString);
                return new URL(urlAsString);
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return currentUrl;
    }

    URL getUrl() {
        return mWebRenderer.getUrl();
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
        mWebRenderer.hidePopups();
    }

    private void resetButtonPressedStates() {
        if (mShareButton != null) {
            mShareButton.setIsTouched(false);
        }
        if (mOpenEmbedButton != null) {
            mOpenEmbedButton.setIsTouched(false);
        }
        if (mOpenInAppButton != null) {
            mOpenInAppButton.setIsTouched(false);
        }
        if (mArticleModeButton != null) {
            mArticleModeButton.setIsTouched(false);
        }
        if (mOverflowButton != null) {
            mOverflowButton.setIsTouched(false);
        }
    }

    private boolean openInBrowser(String urlAsString) {
        return openInBrowser(urlAsString, false);
    }

    private boolean openInBrowser(String urlAsString, boolean canShowUndoPrompt) {
        Log.d(TAG, "ContentView.openInBrowser() - url:" + urlAsString);
        CrashTracking.log("ContentView.openInBrowser()");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urlAsString));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (MainApplication.openInBrowser(getContext(), intent, true) && MainController.get() != null && mOwnerTabView != null) {
            MainController.get().closeTab(mOwnerTabView, MainController.get().contentViewShowing(), canShowUndoPrompt);
            // L_WATCH: L currently lacks getRecentTasks(), so minimize here
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                MainController.get().switchToBubbleView();
            }
            return true;
        }

        return false;
    }

    private boolean openInApp(ResolveInfo resolveInfo, String urlAsString) {
        Context context = getContext();
        if (MainApplication.loadResolveInfoIntent(getContext(), resolveInfo, urlAsString, mInitialUrlLoadStartTime)) {
            CrashTracking.log("openInApp(): resolveInfo:" + resolveInfo.activityInfo.packageName);
            String title = String.format(context.getString(R.string.link_loaded_with_app),
                    resolveInfo.loadLabel(context.getPackageManager()));
            MainApplication.saveUrlInHistory(context, resolveInfo, urlAsString, title);

            MainController mainController = MainController.get();
            if (mainController != null) {
                mainController.closeTab(mOwnerTabView, mainController.contentViewShowing(), false);
            }
            Settings.get().addRedirectToApp(urlAsString);

            // L_WATCH: L currently lacks getRecentTasks(), so minimize here
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && null != mainController) {
                mainController.switchToBubbleView();
            }
            return true;
        }

        return false;
    }

    private void showOpenInBrowserPrompt(int hasBrowserStringId, int noBrowserStringId, final String urlAsString) {
        String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
        String message;
        if (defaultBrowserLabel != null) {
            message = String.format(getResources().getString(hasBrowserStringId), defaultBrowserLabel);
        } else {
            message = getResources().getString(noBrowserStringId);
        }
        Prompt.show(message, getResources().getString(android.R.string.ok),
                Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
            @Override
            public void onActionClick() {
                if (urlAsString != null) {
                    CrashTracking.log("ContentView.showOpenInBrowserPrompt() - onActionClick()");
                    openInBrowser(urlAsString);
                }
            }
            @Override
            public void onClose() {
            }
        });
    }

    void updateUrlTitleAndText(String urlAsString) {
        String title = MainApplication.sTitleHashMap != null ? MainApplication.sTitleHashMap.get(urlAsString) : null;
        boolean showTitleUrl = !urlAsString.equals(getContext().getString(R.string.empty_bubble_page));
        if (title == null) {
            title = mLoadingString;
        }
        else if (!showTitleUrl) {
            mTitleTextView.setTextColor(0xFFFFFFFF);
        }
        mTitleTextView.setText(title);

        if (urlAsString.equals(Constant.NEW_TAB_URL)) {
            mUrlTextView.setText(null);
        } else if (urlAsString.equals(Constant.WELCOME_MESSAGE_URL)) {
            mUrlTextView.setText(Constant.WELCOME_MESSAGE_DISPLAY_URL);
        } else {
            mUrlTextView.setText(urlAsString.replace("http://", ""));
            if (!showTitleUrl) {
                mUrlTextView.setTextColor(0xFFFFFFFF);
            }
        }
    }

    void showAllowLocationDialog(final String origin, final WebRenderer.GetGeolocationCallback callback) {

        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null
                || locationManager.getAllProviders() == null
                || locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) == false) {
            return;
        }

        String originCopy = origin.replace("http://", "").replace("https://", "");
        String messageText = String.format(getResources().getString(R.string.requesting_location_message), originCopy);
        mRequestLocationTextView.setText(messageText);
        mRequestLocationContainer.setVisibility(View.VISIBLE);
        mRequestLocationShadow.setVisibility(View.VISIBLE);
        mRequestLocationYesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onAllow();
                hideAllowLocationDialog();
            }
        });
    }

    void hideAllowLocationDialog() {
        mRequestLocationContainer.setVisibility(View.GONE);
        mRequestLocationShadow.setVisibility(View.GONE);
    }

    int getArticleNotificationId() {
        return mArticleNotificationId;
    }
}
