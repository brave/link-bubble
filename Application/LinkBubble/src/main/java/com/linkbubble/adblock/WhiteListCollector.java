/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.adblock;

import android.content.Context;

import com.linkbubble.R;
import com.linkbubble.util.CrashTracking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WhiteListCollector {

    public WhiteListCollector(Context context) {
        mContext = context;
        mWhiteList = new HashSet<String>();
        mLock = new ReentrantReadWriteLock();
        try {
            File dataPath = new File(mContext.getApplicationInfo().dataDir, mContext.getString(R.string.whitelist_localfilename));

            byte[] buffer = null;
            if (dataPath.exists()) {
                buffer = ADBlockUtils.readLocalFile(dataPath);
            }
            if (null != buffer) {
                String[] array = new String(buffer).split(",");
                for (int i = 0; i < array.length; i++) {
                    if (array[i].equals("")) {
                        continue;
                    }

                    mWhiteList.add(array[i]);
                }
            }
        }
        catch (Exception exc) {
            CrashTracking.logHandledException(exc);
        }
    }

    public boolean isInWhiteList(String host) {
        if (null != host && host.startsWith("www.")) {
            host = host.substring("www.".length());
        }
        try {
            mLock.readLock().lock();

            if (mWhiteList.contains(host)) {
                return true;
            }
        }
        finally {
            mLock.readLock().unlock();
        }

        return false;
    }

    public void addHostToWhiteList(String host) {
        if (null != host && host.startsWith("www.")) {
            host = host.substring("www.".length());
        }
        File dataPath = new File(mContext.getApplicationInfo().dataDir, mContext.getString(R.string.whitelist_localfilename));
        try {
            mLock.writeLock().lock();
            mWhiteList.add(host);

            saveWhiteList(dataPath.getAbsolutePath());
        }
        finally {
            mLock.writeLock().unlock();
        }
    }

    public void removeHostFromWhiteList(String host) {
        if (null != host && host.startsWith("www.")) {
            host = host.substring("www.".length());
        }
        File dataPath = new File(mContext.getApplicationInfo().dataDir, mContext.getString(R.string.whitelist_localfilename));
        try {
            mLock.writeLock().lock();
            mWhiteList.remove(host);

            saveWhiteList(dataPath.getAbsolutePath());
        }
        finally {
            mLock.writeLock().unlock();
        }
    }

    private void saveWhiteList(String dataPath) {
        try {
            FileOutputStream outputStream = new FileOutputStream(dataPath);
            boolean firstIteration = true;
            for (String whiteListHost : mWhiteList) {
                if (!firstIteration) {
                    outputStream.write(",".getBytes(), 0, ",".length());
                }
                outputStream.write(whiteListHost.getBytes(), 0, whiteListHost.length());
                firstIteration = false;
            }
            outputStream.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashSet<String> mWhiteList;
    private ReentrantReadWriteLock mLock;
    Context mContext;
}
