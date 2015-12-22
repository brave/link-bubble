package com.linkbubble.adblock;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Environment;

import com.linkbubble.R;
import com.linkbubble.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bbondy on 2015-10-13.
 *
 * Wrapper for native library
 */
public class ABPFilterParser {

    static {
        System.loadLibrary("LinkBubble");
    }

    private static final int BUFFER_TO_READ = 16384;    // 16Kb

    public ABPFilterParser(Context context) {
        mVerNumber = getDataVerNumber(context);
        mBuffer = readAdblockData(context);
        if (mBuffer != null) {
            init(mBuffer);
        }
    }

    private String getDataVerNumber(Context context) {
        String url = context.getString(R.string.adblock_url);
        String[] split = url.split("/");
        if (split.length > 2) {
            return split[split.length - 2];
        }

        return "";
    }

    private void removeOldVersionFiles(Context context) {
        File dataDirPath = new File(context.getApplicationInfo().dataDir);
        File[] fileList = dataDirPath.listFiles();

        for (File file : fileList) {
            if (file.getAbsoluteFile().toString().endsWith(context.getString(R.string.adblock_localfilename))) {
                file.delete();
            }
        }
    }

    // One time load of binary data for the filter measured to be ~10-30ms
    // List is ~1MB but it is highly compressed > 80% when it is read from disk.
    private byte[] readAdblockData(Context context) {
        File dataPath = new File(context.getApplicationInfo().dataDir,
                mVerNumber + context.getString(R.string.adblock_localfilename));
        if (!dataPath.exists()) {
            removeOldVersionFiles(context);
            downloadAdblockData(context);
        }

        byte[] buffer = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dataPath.getAbsolutePath());
            int size = inputStream.available();
            buffer = new byte[size];
            int n = - 1;
            int bytesOffset = 0;
            byte[] tempBuffer = new byte[BUFFER_TO_READ];
            while ( (n = inputStream.read(tempBuffer)) != -1) {
                System.arraycopy(tempBuffer, 0, buffer, bytesOffset, n);
                bytesOffset += n;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    private byte[] downloadAdblockData(Context context) {
        byte[] buffer = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(context.getString(R.string.adblock_url));
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return buffer;
            }

            File path = new File(context.getApplicationInfo().dataDir,
                    mVerNumber + context.getString(R.string.adblock_localfilename));
            FileOutputStream outputStream = new FileOutputStream(path);
            inputStream = connection.getInputStream();
            buffer = new byte[BUFFER_TO_READ];
            int n = - 1;
            while ( (n = inputStream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, n);
            }
            outputStream.close();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (connection != null)
                connection.disconnect();
        }

        return buffer;
    }

    public native void init(byte[] data);
    public native String stringFromJNI();
    public native boolean shouldBlock(String baseHost, String url, String filterOption);
    private byte[] mBuffer;
    private String mVerNumber;
}
