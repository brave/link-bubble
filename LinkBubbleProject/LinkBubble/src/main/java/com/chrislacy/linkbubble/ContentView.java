package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.graphics.Canvas;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gw on 19/08/13.
 */
public class ContentView extends LinearLayout {

    private static final String TAG = "UrlLoad";

    private WebView mWebView;
    private CondensedTextView mTitleTextView;
    private CondensedTextView mUrlTextView;
    private ImageButton mShareButton;
    private OpenInAppButton mOpenInAppButton;
    private ImageButton mOverflowButton;
    private View mToolbarHeader;
    private View mWebViewPlaceholder;
    private LinearLayout mToolbarLayout;
    private EventHandler mEventHandler;
    private Context mContext;
    private String mUrl;
    private List<AppForUrl> mAppsForUrl;
    private PopupMenu mOverflowPopupMenu;
    private long mStartTime;
    private Bubble mOwner;
    private int mHeaderHeight;
    private LinearLayout.LayoutParams mWebViewLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);

    private Paint mPaint;

    static class AppForUrl {
        ResolveInfo mResolveInfo;
        String mUrl;
        Drawable mIcon;

        AppForUrl(ResolveInfo resolveInfo, String url) {
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

    // Class for a singular activity item on the list of apps to send to
    private static class ListItem {
        public final String name;
        public final Drawable icon;
        public final String context;
        public final String packageClassName;
        public ListItem(String text, Drawable icon, String context, String packageClassName) {
            this.name = text;
            this.icon = icon;
            this.context = context;
            this.packageClassName = packageClassName;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    public static class PageLoadInfo {
        Bitmap bmp;
        String url;
        String title;
    }

    public interface EventHandler {
        public void onSharedLink();
        public void onPageLoading(String url);
        public void onPageLoaded(PageLoadInfo info);
        public void onReceivedIcon(Bitmap bitmap);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        int xp = (int) Config.getContentViewX(mOwner.getBubbleIndex());

        Path path = new Path();
        path.moveTo(xp + Config.mBubbleWidth * 0.33f, mHeaderHeight + 1.0f);
        path.lineTo(xp + Config.mBubbleWidth * 0.5f, 0.0f);
        path.lineTo(xp + Config.mBubbleWidth * 0.67f, mHeaderHeight + 1.0f);

        canvas.drawPath(path, mPaint);
    }

    public void enableWebView(boolean enable) {
        if (enable) {
            removeView(mWebViewPlaceholder);
            addView(mWebView, mWebViewLayoutParams);
        } else {
            removeView(mWebView);
            addView(mWebViewPlaceholder, mWebViewLayoutParams);
        }
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
        boolean isValid = true;

        String [] urlBlacklist = { "t.co", "goo.gl", "bit.ly" };

        try {
            URL url = new URL(urlString);
            String hostName = url.getHost();

            for (int i=0 ; i < urlBlacklist.length ; ++i) {
                if (hostName.equalsIgnoreCase(urlBlacklist[i])) {
                    isValid = false;
                    break;
                }
            }
        } catch (Exception e) {
            // This should never really happen...!
        }

        return isValid;
    }

    private void showSelectShareMethod() {
        String action = Intent.ACTION_SEND;
        String mimeType = "text/plain";

        // Get list of handler apps that can send
        final Intent intent = new Intent(action);
        intent.setType(mimeType);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

        // Form those activities into an array for the list adapter
        final ListItem[] items = new ListItem[resInfos.size()];
        int i = 0;
        for (ResolveInfo resInfo : resInfos) {
            String context = resInfo.activityInfo.packageName;
            String packageClassName = resInfo.activityInfo.name;
            CharSequence label = resInfo.loadLabel(pm);
            Drawable icon = resInfo.loadIcon(pm);
            items[i] = new ListItem(label.toString(), icon, context, packageClassName);
            i++;
        }
        ListAdapter adapter = new ArrayAdapter<ListItem>(mContext, android.R.layout.select_dialog_item,
                                                         android.R.id.text1, items) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, null, null, null);
                int dp5 = (int) (15 * getResources().getDisplayMetrics().density * 0.5f);
                tv.setCompoundDrawablePadding(dp5);
                return v;
            }
        };

        // Build the list of send applications
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Share with");
        builder.setIcon(android.R.drawable.sym_def_app_icon);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            intent.setClassName(items[which].context, items[which].packageClassName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, mUrl);
            mContext.startActivity(intent);

            mEventHandler.onSharedLink();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    ContentView(final Context ctx, Bubble owner, String url, long startTime, EventHandler eh) {
        super(ctx);
        mContext = ctx;
        mEventHandler = eh;
        mOwner = owner;
        mUrl = url;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xffdddddd);

        mHeaderHeight = Config.dpToPx(10.0f);

        setOrientation(VERTICAL);

        mToolbarLayout = (LinearLayout) inflate(mContext, R.layout.content_toolbar, null);
        mTitleTextView = (CondensedTextView) mToolbarLayout.findViewById(R.id.title_text);
        mUrlTextView = (CondensedTextView) mToolbarLayout.findViewById(R.id.url_text);
        mShareButton = (ImageButton)mToolbarLayout.findViewById(R.id.share_button);
        mShareButton.setOnTouchListener(sButtonOnTouchListener);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectShareMethod();
            }
        });

        mOpenInAppButton = (OpenInAppButton)mToolbarLayout.findViewById(R.id.open_in_app_button);
        mOpenInAppButton.setOnTouchListener(sButtonOnTouchListener);
        mOpenInAppButton.setOnOpenInAppClickListener(new OpenInAppButton.OnOpenInAppClickListener() {

            @Override
            public void appOpened() {
                mEventHandler.onSharedLink();
            }

            @Override
            public void appPickerOpened() {
                mEventHandler.onSharedLink();
            }
        });

        mOverflowButton = (ImageButton)mToolbarLayout.findViewById(R.id.overflow_button);
        mOverflowButton.setOnTouchListener(sButtonOnTouchListener);
        mOverflowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOverflowPopupMenu = new PopupMenu(mContext, mOverflowButton);
                Resources resources = mContext.getResources();
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_upgrade_to_pro, Menu.NONE,
                                                    resources.getString(R.string.action_upgrade_to_pro));
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_reload_page, Menu.NONE,
                                                    resources.getString(R.string.action_reload_page));
                mOverflowPopupMenu.getMenu().add(Menu.NONE, R.id.item_open_in_browser, Menu.NONE,
                                                    resources.getString(R.string.action_open_in_browser));
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

                            case R.id.item_open_in_browser: {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(mUrl));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                MainApplication.loadInBrowser(mContext, intent);
                                mEventHandler.onSharedLink();
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

        mToolbarHeader = new View(mContext);
        addView(mToolbarHeader, ViewGroup.LayoutParams.MATCH_PARENT, mHeaderHeight);

        mWebViewPlaceholder = new View(mContext);

        addView(mToolbarLayout, ViewGroup.LayoutParams.MATCH_PARENT, mContext.getResources().getDimensionPixelSize(R.dimen.toolbar_height));

        mWebView = new WebView(ctx);

        WebSettings ws = mWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);

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
        });

        mWebView.setWebViewClient(new WebViewClient() {
            private int mCount = 0;

            @Override
            public boolean shouldOverrideUrlLoading(WebView wView, String url) {
                if (isValidUrl(url)) {
                    ++mCount;
                }

                List<ResolveInfo> resolveInfos = Settings.get().getAppsThatHandleUrl(url);
                updateAppsForUrl(resolveInfos, url);
                if (Settings.get().autoLoadContent() && resolveInfos != null && resolveInfos.size() > 0) {
                    if (MainApplication.loadResolveInfoIntent(mContext, resolveInfos.get(0), url, mStartTime)) {
                        return false;
                    }
                }

                setAppButton();
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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (isValidUrl(url)) {
                    if (--mCount == 0) {
                        // Store final resolved url
                        mUrl = url;

                        PageLoadInfo pli = new PageLoadInfo();
                        pli.bmp = view.getFavicon();
                        pli.url = url;
                        pli.title = view.getTitle();

                        mEventHandler.onPageLoaded(pli);
                        Log.d(TAG, "onPageFinished() - url: " + url);

                        if (mStartTime > -1) {
                            Log.d("LoadTime", "Saved " + ((System.currentTimeMillis()-mStartTime)/1000) + " seconds.");
                            mStartTime = -1;
                        }
                    }
                }
            }
        });

        mWebView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    WebView webView = (WebView) v;

                    switch(keyCode)
                    {
                        case KeyEvent.KEYCODE_BACK:
                            if(webView.canGoBack())
                            {
                                webView.goBack();
                                return true;
                            }
                            break;
                    }
                }

                return false;
            }
        });

        mWebViewPlaceholder.setBackgroundColor(0xffffffff);
        addView(mWebView, mWebViewLayoutParams);

        updateIncognitoMode(Settings.get().isIncognitoMode());

        updateAppsForUrl(url);
        setAppButton();
        Log.d(TAG, "load url: " + url);
        mStartTime = startTime;
        mWebView.loadUrl(url);
        mTitleTextView.setText(R.string.loading);
        mUrlTextView.setText(url.replace("http://", ""));
    }

    private void setAppButton() {
        if (mOpenInAppButton.configure(mAppsForUrl) == false) {
            mOpenInAppButton.setVisibility(GONE);
        }
    }

    private void updateAppsForUrl(String url) {
        List<ResolveInfo> resolveInfos = Settings.get().getAppsThatHandleUrl(url);
        updateAppsForUrl(resolveInfos, url);
    }

    private void updateAppsForUrl(List<ResolveInfo> resolveInfos, String url) {
        if (resolveInfos != null && resolveInfos.size() > 0) {
            if (mAppsForUrl == null) {
                mAppsForUrl = new ArrayList<AppForUrl>();
            }

            for (ResolveInfo resolveInfoToAdd : resolveInfos) {
                if (resolveInfoToAdd.activityInfo != null) {
                    boolean alreadyAdded = false;
                    for (int i = 0; i < mAppsForUrl.size(); i++) {
                        AppForUrl exisiting = mAppsForUrl.get(0);
                        if (exisiting.mResolveInfo.activityInfo.packageName.equals(resolveInfoToAdd.activityInfo.packageName)
                                && exisiting.mResolveInfo.activityInfo.name.equals(resolveInfoToAdd.activityInfo.name)) {
                            alreadyAdded = true;
                            if (exisiting.mUrl.equals(url) == false) {
                                try {
                                    URL existingUrl = new URL(exisiting.mUrl);
                                    URL updatedUrl = new URL(url);
                                    if (updatedUrl.getHost().contains(existingUrl.getHost())
                                            && updatedUrl.getHost().length() > existingUrl.getHost().length()) {
                                        // don't update the url in this case. This means prevents, as an example, saving a host like
                                        // "mobile.twitter.com" instead of using "twitter.com". This occurs when loading
                                        // "https://twitter.com/lokibartleby/status/412160702707539968" with Tweet Lanes
                                        // and the official Twitter client installed.
                                    } else {
                                        exisiting.mUrl = url;   // Update the Url
                                    }
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }

                            }
                            break;
                        }
                    }

                    if (alreadyAdded == false) {
                        mAppsForUrl.add(new AppForUrl(resolveInfoToAdd, url));
                    }
                }
            }
        }
    }

    void hidePopups() {
        if (mOverflowPopupMenu != null) {
            mOverflowPopupMenu.dismiss();
            mOverflowPopupMenu = null;
        }
    }

    static final OnTouchListener sButtonOnTouchListener = new OnTouchListener() {

        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (v.getBackground() == null) {
                        v.setBackgroundColor(0x555d5d5e);
                    } else {
                        v.getBackground().setColorFilter(0x555d5d5e, PorterDuff.Mode.DARKEN);
                    }
                    v.invalidate();
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (v.getBackground() == null) {
                        v.setBackgroundColor(0x00000000);
                    } else {
                        v.getBackground().clearColorFilter();
                    }
                    v.invalidate();
                    break;
                }
            }
            return false;
        }
    };
}
