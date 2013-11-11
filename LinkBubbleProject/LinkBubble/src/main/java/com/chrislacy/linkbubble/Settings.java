package com.chrislacy.linkbubble;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import com.chrislacy.linkbubble.R;

public class Settings {

    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE = "preference_left_consume_bubble";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME = "preference_left_consume_bubble_package_name";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME = "preference_left_consume_bubble_activity_class_name";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL = "preference_left_consume_bubble_label";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_TYPE = "preference_left_consume_bubble_type";

    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE = "preference_right_consume_bubble";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME = "preference_right_consume_bubble_package_name";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME = "preference_right_consume_bubble_activity_class_name";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL = "preference_right_consume_bubble_label";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_TYPE = "preference_right_consume_bubble_type";

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

    void setConsumeBubble(Config.BubbleAction action, Config.ActionType type, String label, String packageName, String activityClassName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        switch (action) {
            case ConsumeLeft:
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, label);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, packageName);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, activityClassName);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_TYPE, type.name());
                break;

            case ConsumeRight:
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, label);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, packageName);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, activityClassName);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_TYPE, type.name());
                break;
        }
        editor.commit();
    }

    String getConsumeBubbleLabel(Config.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, null);
        }
        return null;
    }

    String getConsumeBubblePackageName(Config.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        }
        return null;
    }

    String getConsumeBubbleActivityClassName(Config.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);
        }
        return null;
    }

    Config.ActionType getConsumeBubbleActionType(Config.BubbleAction action) {
        String actionTypeAsString = null;
        switch (action) {
            case ConsumeLeft:
                actionTypeAsString = mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_TYPE, null);
                break;

            case ConsumeRight:
                actionTypeAsString = mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_TYPE, null);
                break;
        }

        if (actionTypeAsString != null) {
            if (actionTypeAsString.equals(Config.ActionType.Share.name())) {
                return Config.ActionType.Share;
            } else if (actionTypeAsString.equals(Config.ActionType.View.name())) {
                return Config.ActionType.View;
            }
        }

        return Config.ActionType.Unknown;
    }

    Drawable getConsumeBubbleIcon(Config.BubbleAction action) {
        PackageManager packageManager = mContext.getPackageManager();
        try {
            String packageName = getConsumeBubblePackageName(action);
            if (packageName != null) {
                return packageManager.getApplicationIcon(packageName);
            }
        } catch (OutOfMemoryError ex) {
        } catch (PackageManager.NameNotFoundException e) {
        }
        return mContext.getResources().getDrawable(R.drawable.ic_launcher);
    }

}
