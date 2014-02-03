package com.linkbubble.util;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

public class PageInspector {

    private static final String TAG = "PageInspector";

    private static final String JS_VARIABLE = "LinkBubble";
    private static final String UNKNOWN_TAG = "unknown";        // the tag Chrome/WebView uses for unknown elements

    private static final String JS_DROP_DOWN_ITEM_CHECK =
            "{\n" +
            "    var elems = document.getElementsByTagName('select'), i;\n" +
            "    var disabledCount = 0;\n" +
            "    for (i in elems) {\n" +
            "      var elem = elems[i];\n" +
            "      if (elem.disabled == false) {\n" +
            "        elem.disabled = true;\n" +
            "        disabledCount++;\n" +
            "      }\n" +
            "    }\n" +
            "\n" +
            "    if (disabledCount > 0) {\n" +
            "      " + JS_VARIABLE + ".onDropDownFound();\n" +
            "    }\n" +
            "\n" +
            "}";

    private static final String JS_YOUTUBE_EMBED_CHECK =
            "{\n" +
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
            "       " + JS_VARIABLE + ".onYouTubeEmbeds(resultArray.toString());\n" +
            "    }\n" +
            "}";

    // https://developer.apple.com/library/ios/documentation/AppleApplications/Reference/SafariWebContent/ConfiguringWebApplications/ConfiguringWebApplications.html
    // https://developers.google.com/chrome/mobile/docs/installtohomescreen
    private static final String JS_TOUCH_ICON_CHECK =
            "{\n" +
            "  var links = document.head.getElementsByTagName('link');\n" +
            "  var linksArray = null;\n" +
            "  var linksCount = 0;\n" +
            "  for(var link in links){\n" +
            "      if(links.hasOwnProperty(link)){\n" +
            "          var l = links[link];\n" +
            "          if (l.rel != null && l.rel.indexOf(\"apple-touch-icon\") != -1) {\n" +
            "            if (linksArray == null) {\n" +
            "              linksArray = new Array();\n" +
            "            }\n" +
            "            var s = \"@@@\" + l.rel + \",\" + l.href + \",\" + l.sizes + \"###\";\n" +
            "            linksArray[linksCount] = s;\n" +
            "            linksCount++;\n" +
            "          }\n" +
            "      }\n" +
            "  }\n" +
            "  if (linksCount > 0) {" +
            "  " + JS_VARIABLE + ".onTouchIconLinks(linksArray.toString());\n" +
            "  }\n" +
            "}";

    private Context mContext;
    private String mWebViewUrl;
    private JSEmbedHandler mJSEmbedHandler;
    private YouTubeEmbedHelper mYouTubeEmbedHelper;
    private String mLastYouTubeEmbedResultString = null;
    private OnItemFoundListener mOnItemFoundListener;

    private static final int TOUCH_ICON_MAX_SIZE = 256;
    private static final int MAX_FAVICON_ENTRIES = 4;
    private TouchIconEntry[] mTouchIconEntries = new TouchIconEntry[MAX_FAVICON_ENTRIES];
    private int mTouchIconEntryCount = 0;
    private String mLastTouchIconResultString = null;
    private TouchIconTransformation sTouchIconTransformation = null;

    public interface OnItemFoundListener {
        void onYouTubeEmbeds();
        void onTouchIconLoaded(Bitmap bitmap, String pageUrl);
        void onDropDownFound();
    }

    public PageInspector(Context context, WebView webView, OnItemFoundListener listener) {
        mContext = context;
        mJSEmbedHandler = new JSEmbedHandler();
        mOnItemFoundListener = listener;
        webView.addJavascriptInterface(mJSEmbedHandler, JS_VARIABLE);
    }

    public void run(WebView webView, boolean checkForTouchIcon) {
        mWebViewUrl = webView.getUrl();

        String jsEmbed = "javascript:(function() {\n";

        jsEmbed += JS_DROP_DOWN_ITEM_CHECK;

        if (checkForTouchIcon) {
            jsEmbed += JS_TOUCH_ICON_CHECK;
        }

        jsEmbed += JS_YOUTUBE_EMBED_CHECK;
        jsEmbed += "})();";

        webView.loadUrl(jsEmbed);
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
    };

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
        public void onDropDownFound() {
            if (mOnItemFoundListener != null) {
                mOnItemFoundListener.onDropDownFound();
            }
        }
    };

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
            if (w > TOUCH_ICON_MAX_SIZE) {
                try {
                    result = Bitmap.createScaledBitmap(source, TOUCH_ICON_MAX_SIZE, TOUCH_ICON_MAX_SIZE, true);
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
