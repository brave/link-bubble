/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.adinsert;

import android.content.Context;
import android.content.res.AssetManager;

import com.linkbubble.adblock.ADBlockUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Ad insertion list worker
 */
public class AdInserter {
    private static String DATA_FILE_NAME = "data/adInfo.dat";

    public AdInserter(Context context) {
        mHosts = new HashMap<String, String>();
        String datObject = loadData(context);
        parseDatObject(datObject);
    }

    public String getHostObjects(String host) {
        String result = mHosts.get(host);
        if (null == result) {
            result = "";
        }

        return result;
    }

    private String loadData(Context context) {
        AssetManager assetManager = context.getResources().getAssets();
        byte[] buffer = null;

        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(DATA_FILE_NAME);
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
        if (null == buffer) {
            return "";
        }

        return new String(buffer);
    }

    // We could use Json object here in future maybe, but unfortunately
    // JSONObject isn't working properly for me maybe because of the structure's size.
    private void parseDatObject(String datObject) {
        int currentIndex = 0;
        while(datObject.length() > currentIndex) {
            if ('\"' != datObject.charAt(currentIndex)) {
                break;
            }
            int index = datObject.indexOf('\"', currentIndex + 1);
            if (-1 == index) {
                break;
            }
            String mapKey = datObject.substring(currentIndex + 1, index);
            if (datObject.length() < index + 2) {
                break;
            }
            int endIndex = datObject.indexOf("}]", index + 2);
            if (-1 == endIndex) {
                break;
            }
            String mapValue = datObject.substring(index + 2, endIndex + 2);
            mHosts.put(mapKey, mapValue);
            currentIndex = endIndex + 3;
        }
    }

    private HashMap<String, String> mHosts;
}
