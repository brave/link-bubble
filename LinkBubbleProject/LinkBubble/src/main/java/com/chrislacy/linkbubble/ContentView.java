package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.view.*;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.Canvas;

import java.net.URL;
import java.util.List;

import com.chrislacy.linkbubble.R;

/**
 * Created by gw on 19/08/13.
 */
public class ContentView extends LinearLayout {

    private WebView mWebView;
    private CondensedTextView mTitleTextView;
    private ImageButton mShareButton;
    private ImageButton mAppButton;
    private ImageButton mOverflowButton;
    private int mMaxToolbarHeight;
    private FrameLayout mToolbarSpacer;
    private View mToolbarHeader;
    private View mWebViewPlaceholder;
    private RelativeLayout mToolbarLayout;
    private EventHandler mEventHandler;
    private Context mContext;
    private String mUrl;
    private Bubble mOwner;
    private int mHeaderHeight;
    private LinearLayout.LayoutParams mWebViewLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);

    private String mShareContext;
    private String mSharePackage;

    private Paint mPaint;

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
    }

    public interface EventHandler {
        public void onSharedLink();
        public void onPageLoaded(PageLoadInfo info);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        int xp = mOwner.getXPos();

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

    private void startDefaultAppOrPromptUserForSelection() {
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

    ContentView(final Context ctx, Bubble owner, String url, EventHandler eh) {
        super(ctx);
        mContext = ctx;
        mEventHandler = eh;
        mOwner = owner;
        mUrl = url;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xffdddddd);

        mHeaderHeight = Config.dpToPx(10.0f);

        setOrientation(VERTICAL);

        mToolbarSpacer = new FrameLayout(ctx);

        //mCloseButton = new ImageButton(ctx);
        //mCloseButton.setImageResource(android.R.drawable.ic_delete);
        //mCloseButton.setOnClickListener(new OnClickListener() {
        //    @Override
        //   public void onClick(View v) {
        //        mEventHandler.onCloseClicked();
        //    }
        //});
        //Drawable dClose = mCloseButton.getDrawable();

        /*
        mShareButton = new ImageButton(ctx);
        mShareButton.setImageResource(android.R.drawable.ic_menu_share);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDefaultAppOrPromptUserForSelection();
            }
        });
        Drawable dShare = mShareButton.getDrawable();

        //mMaxToolbarHeight = Math.max(dClose.getIntrinsicHeight(), dShare.getIntrinsicHeight());
        mMaxToolbarHeight = dShare.getIntrinsicHeight();

        mAppButton = new ImageButton(ctx);
        mAppButton.setVisibility(GONE);
        mAppButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String action = Intent.ACTION_VIEW;
                Intent intent = new Intent(action);

                intent.setClassName(mShareContext, mSharePackage);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setData(Uri.parse(mUrl));

                mContext.startActivity(intent);

                mEventHandler.onSharedLink();
            }
        });

        mToolbarLayout = new LinearLayout(ctx);
        mToolbarLayout.setBackgroundResource(R.drawable.toolbar);
        mToolbarLayout.setOrientation(HORIZONTAL);

        //mToolbarLayout.addView(mCloseButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mToolbarLayout.addView(mToolbarSpacer, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        mToolbarLayout.addView(mAppButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mToolbarLayout.addView(mShareButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        */

        mMaxToolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);

        mToolbarLayout = (RelativeLayout) inflate(mContext, R.layout.content_toolbar, null);
        mTitleTextView = (CondensedTextView) mToolbarLayout.findViewById(R.id.title);
        mShareButton = (ImageButton)mToolbarLayout.findViewById(R.id.share_button);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDefaultAppOrPromptUserForSelection();
            }
        });

        mAppButton = (ImageButton)mToolbarLayout.findViewById(R.id.open_in_app_button);
        mToolbarLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String action = Intent.ACTION_VIEW;
                Intent intent = new Intent(action);

                intent.setClassName(mShareContext, mSharePackage);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setData(Uri.parse(mUrl));

                mContext.startActivity(intent);

                mEventHandler.onSharedLink();
            }
        });

        mOverflowButton = (ImageButton)mToolbarLayout.findViewById(R.id.overflow_button);
        mOverflowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mContext, mOverflowButton);
                Resources resources = mContext.getResources();
                popupMenu.getMenu().add(Menu.NONE, R.id.item_upgrade_to_pro, Menu.NONE,
                        resources.getString(R.string.action_upgrade_to_pro));
                popupMenu.getMenu().add(Menu.NONE, R.id.item_reload_page, Menu.NONE,
                        resources.getString(R.string.action_reload_page));
                popupMenu.getMenu().add(Menu.NONE, R.id.item_open_in_browser, Menu.NONE,
                                            resources.getString(R.string.action_open_in_browser));
                popupMenu.getMenu().add(Menu.NONE, R.id.item_settings, Menu.NONE,
                        resources.getString(R.string.action_settings));
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return false;
                    }
                });
                popupMenu.show();
            }
        });

        mToolbarHeader = new View(mContext);
        addView(mToolbarHeader, ViewGroup.LayoutParams.MATCH_PARENT, mHeaderHeight);

        mWebViewPlaceholder = new View(mContext);

        addView(mToolbarLayout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mWebView = new WebView(ctx);

        WebSettings ws = mWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);

        mWebView.setWebChromeClient(new WebChromeClient());

        mWebView.setWebViewClient(new WebViewClient() {
            private int mCount = 0;

            @Override
            public boolean shouldOverrideUrlLoading(WebView wView, String url) {
                if (isValidUrl(url)) {
                    ++mCount;
                }
                mWebView.loadUrl(url);
                mTitleTextView.setText(null);
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

                        String [] blacklist = {
                            "com.chrislacy.linkbubble",
                            "com.android.browser",
                            "com.android.chrome",
                            "org.mozilla.fennec",
                            "org.mozilla.firefox_beta"
                        };

                        PageLoadInfo pli = new PageLoadInfo();
                        pli.bmp = view.getFavicon();
                        pli.url = url;

                        String title = view.getTitle();
                        Spanned text;
                        if (title != null) {
                            text = Html.fromHtml("<b>" + title + "</b><br />" + "<small>" + url + "</small>");
                        } else {
                            text = Html.fromHtml(url);
                        }
                        mTitleTextView.setText(text);

                        PackageManager manager = mContext.getPackageManager();
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        //intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        intent.setData(Uri.parse(url));
                        List<ResolveInfo> infos = manager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
                        for (ResolveInfo info : infos) {
                            IntentFilter filter = info.filter;
                            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {

                                // Check if blacklisted
                                boolean packageOk = true;
                                for (String invalidName : blacklist) {
                                    if (invalidName.equals(info.activityInfo.packageName)) {
                                        packageOk = false;
                                        break;
                                    }
                                }

                                if (packageOk) {
                                    mShareContext = info.activityInfo.packageName;
                                    mSharePackage = info.activityInfo.name;

                                    //pli.appHandlerContext = info.activityInfo.packageName;
                                    //pli.appHandlerPackage = info.activityInfo.name;
                                    //pli.appHandlerDrawable = info.loadIcon(manager);
                                    Drawable d = info.loadIcon(manager);

                                    if (d != null) {
                                        Bitmap bitmap = ((BitmapDrawable)d).getBitmap();
                                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, mMaxToolbarHeight, mMaxToolbarHeight, true);
                                        mAppButton.setBackground(new BitmapDrawable(scaled));
                                        mAppButton.setVisibility(VISIBLE);
                                    }

                                    break;
                                }
                            }
                        }

                        mEventHandler.onPageLoaded(pli);
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

        mWebView.loadUrl(url);
    }
}
