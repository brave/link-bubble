package com.linkbubble.util;


import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.linkbubble.Config;

public class PageInspector {

    private static final String TAG = "PageInspector";

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
            "       " + JS_VARIABLE + ".onYouTubeEmbeds(resultArray.toString());\n" +
            "    }\n" +
            "})();";

    private Context mContext;
    private JSEmbedHandler mJSEmbedHandler;
    private YouTubeEmbedHelper mYouTubeEmbedHelper;
    private OnItemFoundListener mOnItemFoundListener;

    public interface OnItemFoundListener {
        void onYouTubeEmbeds();
    }

    public PageInspector(Context context, WebView webView, OnItemFoundListener listener) {
        mContext = context;
        mJSEmbedHandler = new JSEmbedHandler();
        mOnItemFoundListener = listener;
        webView.addJavascriptInterface(mJSEmbedHandler, JS_VARIABLE);
    }

    public void run(WebView webView) {
        webView.loadUrl(JS_EMBED);
    }

    public void reset() {
        if (mYouTubeEmbedHelper != null) {
            mYouTubeEmbedHelper.clear();
        }
    }

    public YouTubeEmbedHelper getYouTubeEmbedHelper() {
        return mYouTubeEmbedHelper;
    }

    // For security reasons, all callbacks should be in a self contained class
    private class JSEmbedHandler {

        @JavascriptInterface
        public void onYouTubeEmbeds(String string) {
            Log.d(TAG, "onYouTubeEmbeds() - " + string);

            if (string == null || string.length() == 0) {
                return;
            }

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
    };


}
