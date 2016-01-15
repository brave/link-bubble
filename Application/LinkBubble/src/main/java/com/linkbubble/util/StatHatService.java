/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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