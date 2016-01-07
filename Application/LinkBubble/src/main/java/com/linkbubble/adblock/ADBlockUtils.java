package com.linkbubble.adblock;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by serg on 16-01-07.
 */
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
}
