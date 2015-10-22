package com.linkbubble.adblock;

/**
 * Created by bbondy on 2015-10-13.
 *
 * Wrapper for native library
 */
public class ABPFilterParser {
    static {
        System.loadLibrary("LinkBubble");
    }

    public native void init();
    public native String stringFromJNI();
    public native boolean shouldBlock(String s);
}
