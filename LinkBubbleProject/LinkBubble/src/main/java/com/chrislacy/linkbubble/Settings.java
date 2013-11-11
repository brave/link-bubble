package com.chrislacy.linkbubble;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {

    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE = "preference_left_consume_bubble";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME = "preference_left_consume_bubble_package_name";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME = "preference_left_consume_bubble_activity_class_name";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL = "preference_left_consume_bubble_label";

    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE = "preference_right_consume_bubble";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME = "preference_right_consume_bubble_package_name";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME = "preference_right_consume_bubble_activity_class_name";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL = "preference_right_consume_bubble_label";

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
    }

    void setConsumeBubble(Bubble.BubbleType bubbleType, String label, String packageName, String activityClassName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        switch (bubbleType) {
            case Consume_Left:
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, label);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, packageName);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, activityClassName);
                break;

            case Consume_Right:
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, label);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, packageName);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, activityClassName);
                break;
        }
        editor.commit();
    }

    String getConsumeBubbleLabel(Bubble.BubbleType bubbleType) {
        switch (bubbleType) {
            case Consume_Left:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, null);

            case Consume_Right:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, null);
        }
        return null;
    }

}
