/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.httpseverywhere;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.database.sqlite.SQLiteDatabase;

import com.linkbubble.R;
import com.linkbubble.adblock.ADBlockUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        if (null != targets) {
            ParseJSONObject(new ByteArrayInputStream(targets));
        }

        String dbName = context.getApplicationInfo().dataDir + "/" + mVerNumber +
                context.getString(R.string.https_everywhere_rulesets_localfilename);
        try {
            mDB = SQLiteDatabase.openDatabase(dbName, null, SQLiteDatabase.OPEN_READONLY);
        }
        catch (SQLiteException e) {
            e.printStackTrace();
            mDB = null;
        }
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
        String host = "";
        String protocol = "";
        String path = "";
        try {
            URL url = new URL(originalUrl);
            host = url.getHost();
            protocol = url.getProtocol();
            if (protocol.equals("https")) {
                return originalUrl;
            }
            path = url.getPath();
        } catch (Exception e) {
            return originalUrl;
        }

        String[] domainParts = host.split(Pattern.quote("."));
        if (domainParts.length <= 1) {
            return originalUrl;
        }
        //to do black list
        //
        StringBuilder domainToCheck = new StringBuilder(domainParts[domainParts.length - 1]);
        String ruleIds = "";
        for (int i = domainParts.length - 2; i >= 0; i--) {
            domainToCheck.insert(0, ".");
            domainToCheck.insert(0, domainParts[i]);
            String prefix = "";
            if (i > 0) {
                prefix = "*.";
            }
            List values = mTargets.get(prefix + domainToCheck);
            if (null == values) {
                continue;
            }
            for (int j = 0; j < values.size(); j++) {
                if (0 != ruleIds.length()) {
                    ruleIds += ", ";
                }
                ruleIds += values.get(j).toString();
            }
        }
        if (0 == ruleIds.length()) {
            return originalUrl;
        }

        String newHost = getNewHostFromIds(ruleIds, protocol + "://" + host);
        if (0 != newHost.length()) {
            String[] hostSplit = newHost.split(Pattern.quote("."));
            if (hostSplit.length < 3) {
                newHost = newHost.replace("https://", "https://www.");
            }
            return newHost + path;
        }

        return originalUrl;
    }

    private String getNewHostFromIds(String ruleIds, String originalUrl) {
        if (null == mDB) {
            return "";
        }
        Cursor cursor = mDB.rawQuery("select contents from rulesets where id in (" + ruleIds + ")", null);
        if (0 == cursor.getCount()) {
            return "";
        }
        List<String> results = new ArrayList<String>();
        while (cursor.moveToNext()) {
            results.add(cursor.getString(0));
        }
        if (0 == results.size()) {
            return "";
        }

        String newUrl = "";

        for (String result: results) {
            try {
                JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(result.getBytes()), "UTF-8"));

                reader.beginObject();
                while (reader.hasNext()) {
                    if (!reader.nextName().equals("ruleset")) {
                        break;
                    }
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String topName = reader.nextName();
                        if (topName.equals("$")) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String name = reader.nextName();
                                if (name.equals("default_off") || name.equals("platform")) {
                                    break;
                                }
                                reader.skipValue();
                            }
                            reader.endObject();
                        }
                        else if (topName.equals("exclusion")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                if (!reader.nextName().equals("$")) {
                                    break;
                                }
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    if (!reader.nextName().equals("pattern")) {
                                        break;
                                    }
                                    if (originalUrl.matches(".*" + reader.nextString() + ".*")) {
                                        return newUrl;
                                    }
                                }
                                reader.endObject();
                                reader.endObject();
                            }
                            reader.endArray();
                        }
                        else if (topName.equals("rule")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                if (!reader.nextName().equals("$")) {
                                    break;
                                }
                                reader.beginObject();
                                String from = "";
                                String to = "";
                                while (reader.hasNext()) {
                                    String fromToName = reader.nextName();
                                    if (fromToName.equals("from")) {
                                        from = reader.nextString();
                                    }
                                    else if (fromToName.equals("to")) {
                                        to = reader.nextString();
                                    }
                                    else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                                reader.endObject();
                                if (0 != from.length() && 0 != to.length() && originalUrl.matches(".*" + from + ".*")) {
                                    newUrl = originalUrl.replaceAll(from, to);
                                    if (newUrl.equals(originalUrl)) {
                                        newUrl = "";
                                    }
                                    else {
                                        return newUrl;
                                    }
                                }
                            }
                            reader.endArray();
                        }
                        else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return newUrl;
    }

    private String mVerNumber;
    ConcurrentHashMap<String, List> mTargets;
    SQLiteDatabase mDB;
}
