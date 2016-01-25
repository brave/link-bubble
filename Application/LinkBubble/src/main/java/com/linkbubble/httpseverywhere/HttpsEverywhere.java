/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.httpseverywhere;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.JsonReader;

import com.linkbubble.R;
import com.linkbubble.adblock.ADBlockUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class HttpsEverywhere {

    private static final String ETAG_PREPEND_RULE_SETS = "rs";
    private static final String ETAG_PREPEND_TARGETS = "targ";
    private static final int REDIRECT_BLACK_LIST_COUNT = 5;

    public HttpsEverywhere(Context context) {
        mVerNumber = ADBlockUtils.getDataVerNumber(context.getString(R.string.https_everywhere_rulesets_url));

        ADBlockUtils.readData(context, context.getString(R.string.https_everywhere_rulesets_localfilename),
                context.getString(R.string.https_everywhere_rulesets_url), ETAG_PREPEND_RULE_SETS, mVerNumber, true);

        byte[] targets = ADBlockUtils.readData(context, context.getString(R.string.https_everywhere_targets_localfilename),
                context.getString(R.string.https_everywhere_targets_url), ETAG_PREPEND_RULE_SETS, mVerNumber, false);

        mTargets = new ConcurrentHashMap<String, List>();
        ParseJSONObject(new ByteArrayInputStream(targets));
    }

    private void ParseJSONObject(InputStream in) {
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));

            readJSON(reader);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readJSON(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            int value = 0;

            reader.beginArray();
            List values = new ArrayList();
            while (reader.hasNext()) {
                values.add(reader.nextInt());
            }
            reader.endArray();
            mTargets.put(name, values);
        }
        reader.endObject();
    }

    public String getRealUrl(String originalUrl) {
        String host;
        try {
            host = new URL(originalUrl).getHost();
        } catch (Exception e) {
            return originalUrl;
        }
        String[] domainParts = host.split(Pattern.quote("."));
        if (domainParts.length <= 1) {
            return originalUrl;
        }

        return originalUrl;
    }

    private String mVerNumber;
    ConcurrentHashMap<String, List> mTargets;
    ConcurrentHashMap<String, Integer> mHostRedirectCounter;
}
