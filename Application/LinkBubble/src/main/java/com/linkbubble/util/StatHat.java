/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.util.Log;

import java.net.URLEncoder;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

class StatHat {

    private static final String KEY = "key-here";
    private static final String TAG = "StatHat";

    private static StatHat sInstance = null;

    public static StatHat get() {
        if (sInstance == null) {
            sInstance = new StatHat();
        }

        return sInstance;
    }

    StatHat() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://api.stathat.com/")
                .setLog(new RestAdapter.Log() {
                    @Override
                    public void log(String s) {
                        Log.d(TAG, "log() - " + s);
                    }
                })
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .build();

        mStatHatService = restAdapter.create(StatHatService.class);
    }

    private StatHatService mStatHatService;

    void ezPostValue(final String statName, Double value) {
        try {
            mStatHatService.ezValue(KEY, statName, URLEncoder.encode(value.toString(), "UTF-8"), new Callback<StatHatService.Result>() {

                @Override
                public void success(StatHatService.Result result, Response response) {
                    Log.d(TAG, "ezPostValue() - success for " + statName + ", " + result.msg);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.d(TAG, "ezPostValue() - failure for " + statName);
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, "ezPostCount exception:  " + e.getLocalizedMessage(), e);
        }
    }

    void ezPostCount(final String statName, Integer count) {
        try {
            mStatHatService.ezCount(KEY, statName, URLEncoder.encode(count.toString(), "UTF-8"), new Callback<StatHatService.Result>() {

                @Override
                public void success(StatHatService.Result result, Response response) {
                    Log.d(TAG, "ezPostCount() - success for " + statName + ", " + result.msg);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.d(TAG, "ezPostCount() - failure for " + statName);
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, "ezPostCount exception:  " + e.getLocalizedMessage(), e);
        }
    }
}
