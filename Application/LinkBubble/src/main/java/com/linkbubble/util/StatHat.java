package com.linkbubble.util;

import android.util.Log;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

class StatHat {

    private static final String KEY = "ws7pLkHbVaQdOH8x";

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
                        Log.d("stathat", "log() - " + s);
                    }
                })
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .build();

        mStatHatService = restAdapter.create(StatHatService.class);
    }

    private StatHatService mStatHatService;

    void ezPostValue(String statName, Double value) {
        try {
            mStatHatService.ezValue(KEY, statName, URLEncoder.encode(value.toString(), "UTF-8"), new Callback<StatHatService.Result>() {

                @Override
                public void success(StatHatService.Result result, Response response) {

                }

                @Override
                public void failure(RetrofitError retrofitError) {

                }
            });
        }
        catch (Exception e) {
            System.err.println("ezPostValue exception:  " + e);
        }
    }

    void ezPostCount(String statName, Integer count) {
        try {
            mStatHatService.ezCount(KEY, statName, URLEncoder.encode(count.toString(), "UTF-8"), new Callback<StatHatService.Result>() {

                @Override
                public void success(StatHatService.Result result, Response response) {
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                }
            });
        }
        catch (Exception e) {
            System.err.println("ezPostCount exception:  " + e);
        }
    }
}
