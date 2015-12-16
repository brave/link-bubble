package com.linkbubble.adblock;

import android.content.Context;

import com.linkbubble.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class TrackingProtectionList {

    public TrackingProtectionList(Context context) {
        mVerNumber = getDataVerNumber(context);
        mDisconnectDomains = readTrackingProtectionData(context);
    }

    private String getDataVerNumber(Context context) {
        String url = context.getString(R.string.tracking_protection_url);
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
            if (file.getAbsoluteFile().toString().endsWith(context.getString(R.string.tracking_protection_localfilename))) {
                file.delete();
            }
        }
    }

    private HashSet<String> readTrackingProtectionData(Context context) {
        File dataPath = new File(context.getApplicationInfo().dataDir,
                mVerNumber + context.getString(R.string.tracking_protection_localfilename));
        if (!dataPath.exists()) {
            removeOldVersionFiles(context);

            return downloadTrackingProtectionData(context);
        }

        byte[] buffer = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dataPath.getAbsolutePath());
            int size = inputStream.available();
            buffer = new byte[size];
            inputStream.read(buffer, 0, size);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteToHashSet(buffer);
    }

    private HashSet<String> byteToHashSet(byte[] buffer) {
        if (null == buffer) {
            return new HashSet<String>();
        }
        String str = new String(buffer);
        String[] elems = str.split(",");

        return new HashSet<String>(Arrays.asList(elems));
    }

    private HashSet<String> downloadTrackingProtectionData(Context context) {
        byte[] buffer = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(context.getString(R.string.tracking_protection_url));
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new HashSet<String>();
            }

            int size = connection.getContentLength();
            buffer = new byte[size];

            inputStream = connection.getInputStream();
            inputStream.read(buffer);
            FileOutputStream outputStream;

            try {
                File path = new File(context.getApplicationInfo().dataDir,
                        mVerNumber + context.getString(R.string.tracking_protection_localfilename));

                outputStream = new FileOutputStream(path);
                outputStream.write(buffer);
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
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

            if (connection != null) {
                connection.disconnect();
            }
        }

        return byteToHashSet(buffer);
    }

    public Boolean shouldBlockHost(String baseHost, String host) {
        if (null == mDisconnectDomains) {
            return false;
        }
        String[] domainParts = host.split(Pattern.quote("."));
        if (domainParts.length <= 1) {
            return false;
        }
        StringBuilder domainToCheck = new StringBuilder(domainParts[domainParts.length - 1]);
        for (int i = domainParts.length - 2; i >= 0; i--) {
            domainToCheck.insert(0, ".");
            domainToCheck.insert(0, domainParts[i]);
            if (mDisconnectDomains.contains(domainToCheck.toString())) {
                return true;
            }
        }

        return false;
    }

    HashSet<String> mDisconnectDomains;
    private String mVerNumber;
}
