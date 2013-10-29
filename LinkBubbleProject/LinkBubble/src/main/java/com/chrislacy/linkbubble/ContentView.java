package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.view.*;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Canvas;

import java.net.URL;
import java.util.List;

/**
 * Created by gw on 19/08/13.
 */
public class ContentView extends LinearLayout {

    private WebView mWebView;
    private ImageButton mCloseButton;
    private ImageButton mShareButton;
    private FrameLayout mToolbarSpacer;
    private View mToolbarHeader;
    private View mWebViewPlaceholder;
    private LinearLayout mToolbarLayout;
    private EventHandler mEventHandler;
    private Context mContext;
    private String mUrl;
    private Bubble mOwner;
    private int mHeaderHeight;
    private LinearLayout.LayoutParams mWebViewLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);

    private Paint mPaint;

    public interface EventHandler {
        public void onCloseClicked();
        public void onSharedLink();
        public void onPageLoaded(Bitmap bmp);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        int xp = mOwner.getXPos();

        Path path = new Path();
        path.moveTo(xp, mHeaderHeight + 1.0f);
        path.lineTo(xp + Config.mBubbleWidth * 0.5f, 0.0f);
        path.lineTo(xp + Config.mBubbleWidth, mHeaderHeight + 1.0f);

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

    public void startDefaultAppOrPromptUserForSelection() {
        String action = Intent.ACTION_SEND;
        String mimeType = "text/plain";

        // Get list of handler apps that can send
        final Intent intent = new Intent(action);
        intent.setType(mimeType);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

        // Class for a singular activity item on the list of apps to send to
        class ListItem {
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
        mPaint.setColor(0xffaaaaaa);

        mHeaderHeight = Config.dpToPx(20.0f);

        setOrientation(VERTICAL);

        mToolbarSpacer = new FrameLayout(ctx);

        mCloseButton = new ImageButton(ctx);
        mCloseButton.setImageResource(android.R.drawable.ic_delete);
        mCloseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            mEventHandler.onCloseClicked();
            }
        });
        mShareButton = new ImageButton(ctx);
        mShareButton.setImageResource(android.R.drawable.ic_menu_share);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDefaultAppOrPromptUserForSelection();
            }
        });

        mToolbarLayout = new LinearLayout(ctx);
        mToolbarLayout.setBackgroundResource(R.drawable.toolbar);
        mToolbarLayout.setOrientation(HORIZONTAL);

        mToolbarLayout.addView(mCloseButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mToolbarLayout.addView(mToolbarSpacer, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        mToolbarLayout.addView(mShareButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

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
                        Bitmap bmp = view.getFavicon();
                        mEventHandler.onPageLoaded(bmp);
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
