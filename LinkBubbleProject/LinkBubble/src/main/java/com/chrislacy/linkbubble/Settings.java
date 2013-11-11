package com.chrislacy.linkbubble;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class Settings {

    /*
	 *
	 */
    public static void initModule(Context context) {
        mInstance = new Settings(context);
    }

    /*
     *
     */
    public static void deinitModule() {
        mInstance = null;
    }

    /*
     *
     */
    public static Settings get()	{
        return mInstance;
    }

    private static Settings mInstance = null;

    private SharedPreferences mSharedPreferences;
    private Context mContext;

    Settings(Context context) {
        mContext = context;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        Resources resources = mContext.getResources();
    }

}
