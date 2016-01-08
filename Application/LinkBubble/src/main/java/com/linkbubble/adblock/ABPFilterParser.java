package com.linkbubble.adblock;

import android.content.Context;
import com.linkbubble.R;

/**
 * Created by bbondy on 2015-10-13.
 *
 * Wrapper for native library
 */
public class ABPFilterParser {

    static {
        System.loadLibrary("LinkBubble");
    }

    private static final String ETAG_PREPEND = "abp";

    public ABPFilterParser(Context context) {
        mVerNumber = ADBlockUtils.getDataVerNumber(context.getString(R.string.adblock_url));
        // One time load of binary data for the filter measured to be ~10-30ms
        // List is ~1MB but it is highly compressed > 80% when it is read from disk.
        mBuffer = ADBlockUtils.readData(context, context.getString(R.string.adblock_localfilename),
                context.getString(R.string.adblock_url), ETAG_PREPEND, mVerNumber);
        if (mBuffer != null) {
            init(mBuffer);
        }
    }


    public native void init(byte[] data);
    public native String stringFromJNI();
    public native boolean shouldBlock(String baseHost, String url, String filterOption);
    private byte[] mBuffer;
    private String mVerNumber;
}
