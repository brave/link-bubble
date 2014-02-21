package com.linkbubble.util;


import retrofit.Callback;
import retrofit.http.POST;
import retrofit.http.Query;

public interface StatHatService {

    @SuppressWarnings("unused")
    static class Result {
        int status;
        String msg;
    }

    @POST("/ez")
    void ezCount(@Query("ezkey") String key, @Query("stat") String stat, @Query("count") String count, Callback<Result> callback);

    @POST("/ez")
    void ezValue(@Query("ezkey") String key, @Query("stat") String stat, @Query("value") String value, Callback<Result> callback);
}