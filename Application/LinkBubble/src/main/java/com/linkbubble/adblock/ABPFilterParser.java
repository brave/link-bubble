package com.linkbubble.adblock;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by bbondy on 2015-10-13.
 *
 * Wrapper for native library
 */
public class ABPFilterParser {
    static {
        System.loadLibrary("LinkBubble");
    }

    public ABPFilterParser(Context context) {
        mBuffer = readAdblockData(context);
        init(mBuffer);
    }

    // One time load of binary data for the filter measured to be ~10-30ms
    // List is ~1MB but it is highly compressed > 80% when it is read from disk.
    private byte[] readAdblockData(Context context) {
        byte[] buffer = null;
        AssetManager assetManager = context.getResources().getAssets();
        InputStream inputStream = null;
        String path = "data/ABPFilterParserData.dat";
        try {
            inputStream = assetManager.open(path);
            int size = inputStream.available();
            buffer = new byte[size];
            inputStream.read(buffer, 0, size);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return buffer;
    }

    public native void init(byte[] data);
    public native String stringFromJNI();
    public native boolean shouldBlock(String baseHost, String url);
    private byte[] mBuffer;
}
