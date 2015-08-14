package com.linkbubble.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;

public class PageInspector {

    private static final String TAG = "PageInspector";

    public static final int INSPECT_DROP_DOWN = 0x01;
    public static final int INSPECT_YOUTUBE = 0x02;
    public static final int INSPECT_TOUCH_ICON = 0x04;
    public static final int INSPECT_FETCH_HTML = 0x08;
    public static final int INSPECT_THEME_COLOR = 0x10;
    public static final int INSPECT_ALL = INSPECT_DROP_DOWN | INSPECT_YOUTUBE | INSPECT_TOUCH_ICON | INSPECT_THEME_COLOR;

    private static final String JS_VARIABLE = "LinkBubble";
    private static final String UNKNOWN_TAG = "unknown";        // the tag Chrome/WebView uses for unknown elements

    private Context mContext;
    private String mWebViewUrl;
    private WebView mWebView;
    private Handler mHandler;
    private JSEmbedHandler mJSEmbedHandler;
    private YouTubeEmbedHelper mYouTubeEmbedHelper;
    private String mLastYouTubeEmbedResultString = null;
    private OnItemFoundListener mOnItemFoundListener;

    private static final int MAX_FAVICON_ENTRIES = 4;
    private TouchIconEntry[] mTouchIconEntries = new TouchIconEntry[MAX_FAVICON_ENTRIES];
    private int mTouchIconEntryCount = 0;
    private String mLastTouchIconResultString = null;
    private TouchIconTransformation sTouchIconTransformation = null;

    public interface OnItemFoundListener {
        void onYouTubeEmbeds();
        void onTouchIconLoaded(Bitmap bitmap, String pageUrl);
        void onDropDownFound();
        void onDropDownWarningClick();
        void onFetchHtml(String html);
        void onThemeColor(int color);
    }

    public PageInspector(Context context, WebView webView, OnItemFoundListener listener) {
        mContext = context;
        mWebView = webView;
        mHandler = new Handler();
        mJSEmbedHandler = new JSEmbedHandler();
        mOnItemFoundListener = listener;
        webView.addJavascriptInterface(mJSEmbedHandler, JS_VARIABLE);
    }

    public void run(WebView webView, int inspectFlags) {
        mWebViewUrl = webView.getUrl();

        String jsEmbed = "javascript:(function() {\n";

        if ((inspectFlags & INSPECT_DROP_DOWN) != 0 && Constant.ACTIVITY_WEBVIEW_RENDERING == false) {
            jsEmbed += getFileContents("SelectElements");
        }

        if ((inspectFlags & INSPECT_TOUCH_ICON) != 0) {
            jsEmbed += getFileContents("TouchIcon");
        }

        if ((inspectFlags & INSPECT_YOUTUBE) != 0) {
            jsEmbed += getFileContents("YouTube");
        }

        if ((inspectFlags & INSPECT_FETCH_HTML) != 0) {
            jsEmbed += getFileContents("FetchContent");
        }

        if ((inspectFlags & INSPECT_THEME_COLOR) != 0) {
            jsEmbed += getFileContents("ThemeColor");
        }

        jsEmbed += "})();";

        webView.loadUrl(jsEmbed);
    }

    public String getFileContents(String pageScript) {
        pageScript = "pagescripts/" + pageScript + ".js";
        AssetManager assetManager = mContext.getResources().getAssets();
        InputStream inputStream = null;
        final StringBuilder stringBuilder = new StringBuilder();

        try {
            inputStream = assetManager.open(pageScript);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            boolean done = false;

            while (!done) {
                final String line = reader.readLine();
                done = (line == null);

                if (line != null) {
                    stringBuilder.append(line);
                }
            }

            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public void reset() {
        if (mYouTubeEmbedHelper != null) {
            mYouTubeEmbedHelper.clear();
        }
    }

    public YouTubeEmbedHelper getYouTubeEmbedHelper() {
        return mYouTubeEmbedHelper;
    }

    private static class TouchIconEntry {
        String mRel;
        URL mUrl;
        int mSize;

        public String toString() {
            return "rel:" + mRel + ", url:" + mUrl.toString() + ", size:" + mSize;
        }
    }

    // For security reasons, all callbacks should be in a self contained class
    private class JSEmbedHandler {

        @JavascriptInterface
        public void onTouchIconLinks(String string) {
            if (mLastTouchIconResultString != null && mLastTouchIconResultString.equals(string)) {
                return;
            }
            mLastTouchIconResultString = string;

            if (string == null || string.length() == 0) {
                return;
            }

            Log.d(TAG, "onFaviconLinks() - " + string);

            mTouchIconEntryCount = 0;

            String[] items = string.split("@@@");
            for (String s : items) {
                if (mTouchIconEntryCount == MAX_FAVICON_ENTRIES) {
                    break;
                }
                s = s.replace("###", "");
                if (s.length() > 0) {
                    String[] vars = s.split(",");
                    if (vars == null || vars.length < 2) {
                        continue;
                    }

                    String rel = vars[0];
                    if (rel.equals(UNKNOWN_TAG)) {
                        continue;
                    }
                    String href = vars[1];
                    if (href.equals(UNKNOWN_TAG)) {
                        continue;
                    }
                    int size = -1;
                    String sizes = vars.length > 2 ? vars[2] : null;
                    if (sizes != null && sizes.equals(UNKNOWN_TAG) == false) {
                        String[] splitSizes = sizes.split("x");     // specified as 'sizes="128x128"' (http://goo.gl/tGV50j)
                        if (splitSizes.length > 0) {
                            try {
                                // just pick the first one...
                                size = Integer.valueOf(splitSizes[0]);
                            } catch (NumberFormatException e) {
                                // do nothing...
                            }
                        }
                    }

                    try {
                        TouchIconEntry touchIconEntry = mTouchIconEntries[mTouchIconEntryCount];
                        if (touchIconEntry == null) {
                            touchIconEntry = new TouchIconEntry();
                            mTouchIconEntries[mTouchIconEntryCount] = touchIconEntry;
                        }
                        touchIconEntry.mRel = rel;
                        touchIconEntry.mUrl = new URL(href);
                        touchIconEntry.mSize = size;
                        mTouchIconEntryCount++;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mTouchIconEntryCount > 0) {
                // pick the first one for now
                TouchIconEntry touchIconEntry = mTouchIconEntries[0];
                if (sTouchIconTransformation == null) {
                    sTouchIconTransformation = new TouchIconTransformation();
                }
                sTouchIconTransformation.setListener(mOnItemFoundListener);
                sTouchIconTransformation.mTouchIconPageUrl = mWebViewUrl;
                Picasso.with(mContext).load(touchIconEntry.mUrl.toString()).transform(sTouchIconTransformation).fetch();
            }

        }

        @JavascriptInterface
        public void onSelectElementInteract(String optionString) {
            if (optionString == null || optionString.length() == 0) {
                return;
            }

            Log.d(TAG, "onSelectElementInteract() - " + optionString);

            final ArrayList<String> optionList = new ArrayList<String>();
            int selectedIndex = 0;
            try {
                JSONArray optionArray = new JSONArray(optionString);
                if (optionArray != null) {
                    int len = optionArray.length();
                    for (int i = 1; i < len; i += 2) {
                        optionList.add(optionArray.get(i).toString());
                    }
                }
                selectedIndex = Integer.parseInt(optionArray.get(0).toString());
            } catch(JSONException e) {
                Log.d(TAG, "error parsing json");
            }

            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setSingleChoiceItems(optionList.toArray(new String[optionList.size()]), selectedIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, final int position) {
                    Log.d(TAG, "click position is - " + position);
                    dialog.dismiss();

                    mHandler.postDelayed(new Runnable() {

                        public void run() {
                            mWebView.loadUrl("javascript:LinkBubble.selectOption(" + position + ")");
                        }

                    }, 1);
                }
            });
            dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
        }

        @JavascriptInterface
        public void onYouTubeEmbeds(String string) {
            if (mLastYouTubeEmbedResultString != null && mLastYouTubeEmbedResultString.equals(string)) {
                return;
            }
            mLastYouTubeEmbedResultString = string;

            if (string == null || string.length() == 0) {
                return;
            }

            Log.d(TAG, "onYouTubeEmbeds() - " + string);

            if (mYouTubeEmbedHelper == null) {
                mYouTubeEmbedHelper = new YouTubeEmbedHelper(mContext);
            }

            String[] strings = string.split(",");
            if (mYouTubeEmbedHelper.onEmbeds(strings)) {
                if (mOnItemFoundListener != null) {
                    mOnItemFoundListener.onYouTubeEmbeds();
                }
            }
        }

        @JavascriptInterface
        public void onThemeColor(String string) {
            Log.e("themecolor", "onThemeColor():" + string);
            if (string != null) {
                string = string.replace("#", "");
                if (mOnItemFoundListener != null) {
                    try {
                        int color = Integer.parseInt(string, 16);
                        color |= 0xff000000;
                        mOnItemFoundListener.onThemeColor(color);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        @JavascriptInterface
        public void onDropDownFound() {
            if (mOnItemFoundListener != null) {
                mOnItemFoundListener.onDropDownFound();
            }
        }

        @JavascriptInterface
        public void onDropDownWarningClick() {
            if (mOnItemFoundListener != null) {
                mOnItemFoundListener.onDropDownWarningClick();
            }
        }

        @JavascriptInterface
        public void fetchHtml(String html) {
            //Log.d(TAG, "fetchHtml() - " + html);
            if (mOnItemFoundListener != null) {
                mOnItemFoundListener.onFetchHtml(html);
            }
        }
    }

    private static class TouchIconTransformation implements Transformation {

        private WeakReference<OnItemFoundListener> mListener;
        String mTouchIconPageUrl = null;

        void setListener(OnItemFoundListener listener) {
            if (mListener == null || mListener.get() != listener) {
                mListener = new WeakReference<OnItemFoundListener>(listener);
            }
        }

        @Override
        public Bitmap transform(Bitmap source) {
            int w = source.getWidth();

            Bitmap result = source;
            if (w > Constant.TOUCH_ICON_MAX_SIZE) {
                try {
                    result = Bitmap.createScaledBitmap(source, Constant.TOUCH_ICON_MAX_SIZE, Constant.TOUCH_ICON_MAX_SIZE, true);
                } catch (OutOfMemoryError e) {
                    
                }
            }

            if (result != null && mListener != null) {
                OnItemFoundListener listener = mListener.get();
                if (listener != null) {
                    listener.onTouchIconLoaded(result, mTouchIconPageUrl);
                }
            }

            // return null. No need for Picasso to cache this, as we're already doing so elsewhere
            return null;
        }

        @Override
        public String key() { return "faviconTransformation()"; }
    }
}
