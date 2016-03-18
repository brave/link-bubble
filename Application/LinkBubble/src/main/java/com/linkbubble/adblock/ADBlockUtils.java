/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.adblock;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

public class ADBlockUtils {

    public static final long MILLISECONDS_IN_A_DAY = 86400 * 1000;
    public static final int BUFFER_TO_READ = 16384;    // 16Kb

    private static final String ETAGS_PREFS_NAME = "EtagsPrefsFile";
    private static final String ETAG_NAME = "Etag";
    private static final String TIME_NAME = "Time";

    public static void saveETagInfo(Context context, String prepend, EtagObject etagObject) {
        SharedPreferences sharedPref = context.getSharedPreferences(ETAGS_PREFS_NAME, 0);

        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(prepend + ETAG_NAME, etagObject.mEtag);
        editor.putLong(prepend + TIME_NAME, etagObject.mMilliSeconds);

        editor.apply();
    }

    public static EtagObject getETagInfo(Context context, String prepend) {
        SharedPreferences sharedPref = context.getSharedPreferences(ETAGS_PREFS_NAME, 0);

        EtagObject etagObject = new EtagObject();

        etagObject.mEtag = sharedPref.getString(prepend + ETAG_NAME, "");
        etagObject.mMilliSeconds = sharedPref.getLong(prepend + TIME_NAME, 0);

        return etagObject;
    }

    public static String getDataVerNumber(String url) {
        String[] split = url.split("/");
        if (split.length > 2) {
            return split[split.length - 2];
        }

        return "";
    }

    public static void removeOldVersionFiles(Context context, String fileName) {
        File dataDirPath = new File(context.getApplicationInfo().dataDir);
        File[] fileList = dataDirPath.listFiles();

        for (File file : fileList) {
            if (file.getAbsoluteFile().toString().endsWith(fileName)) {
                file.delete();
            }
        }
    }

    public static byte[] readLocalFile(File path) {
        byte[] buffer = null;

        FileInputStream inputStream = null;
        try {
            if (!path.exists()) {
                return null;
            }
            inputStream = new FileInputStream(path.getAbsolutePath());
            int size = inputStream.available();
            buffer = new byte[size];
            int n = - 1;
            int bytesOffset = 0;
            byte[] tempBuffer = new byte[ADBlockUtils.BUFFER_TO_READ];
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

    public static byte[] readData(Context context, String fileName, String urlString, String eTagPrepend, String verNumber,
            boolean downloadOnly) {
        File dataPath = new File(context.getApplicationInfo().dataDir, verNumber + fileName);
        long oldFileSize = dataPath.length();
        EtagObject previousEtag = ADBlockUtils.getETagInfo(context, eTagPrepend);
        long milliSeconds = Calendar.getInstance().getTimeInMillis();
        if (0 == oldFileSize || (milliSeconds - previousEtag.mMilliSeconds >= ADBlockUtils.MILLISECONDS_IN_A_DAY)) {
            ADBlockUtils.downloadDatFile(context, oldFileSize, previousEtag, milliSeconds, fileName, urlString, eTagPrepend, verNumber);
        }

        if (downloadOnly) {
            return null;
        }

        return readLocalFile(dataPath);
    }

    public static void downloadDatFile(Context context, long oldFileSize, EtagObject previousEtag, long currentMilliSeconds,
                                       String fileName, String urlString, String eTagPrepend, String verNumber) {
        byte[] buffer = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            String etag = connection.getHeaderField("ETag");
            int length = connection.getContentLength();
            if (null == etag) {
                etag = "";
            }
            boolean downloadFile = true;
            if (oldFileSize == length && etag.equals(previousEtag.mEtag)) {
                downloadFile = false;
            }
            previousEtag.mEtag = etag;
            previousEtag.mMilliSeconds = currentMilliSeconds;
            ADBlockUtils.saveETagInfo(context, eTagPrepend, previousEtag);
            if (!downloadFile) {
                return;
            }
            ADBlockUtils.removeOldVersionFiles(context, fileName);

            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }

            File path = new File(context.getApplicationInfo().dataDir, verNumber + fileName);
            FileOutputStream outputStream = new FileOutputStream(path);
            inputStream = connection.getInputStream();
            buffer = new byte[ADBlockUtils.BUFFER_TO_READ];
            int n = - 1;
            int totalReadSize = 0;
            try {
                while ((n = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, n);
                    totalReadSize += n;
                }
            }
            catch (IllegalStateException exc) {
                // Sometimes it gives us that exception, found that we should do that way to avoid it:
                // Each HttpURLConnection instance is used to make a single request but the
                // underlying network connection to the HTTP server may be transparently shared by other instance.
                // But we do that way, so just wrapped it for now and we will redownload the file on next request
            }
            outputStream.close();
            if (length != totalReadSize) {
                ADBlockUtils.removeOldVersionFiles(context, fileName);
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

            if (connection != null)
                connection.disconnect();
        }

        return;
    }
}
