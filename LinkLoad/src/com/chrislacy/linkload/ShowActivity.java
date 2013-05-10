package com.chrislacy.linkload;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.List;

public class ShowActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (intent.getAction() == Intent.ACTION_VIEW) {

            boolean appEnabled = preferences.getBoolean(SettingsActivity.KEY_APP_ENABLED, true);
            if (appEnabled) {
                //Uri data = intent.getData();
                //String scheme = data.getScheme();

                Boolean linkLoadTest = intent.getBooleanExtra(TestActivity.LINKLOAD_TEST, false);

                ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RecentTaskInfo> list = activityManager.getRecentTasks(20, 0);
                int listSize = list.size();
                if(listSize > 0) {
                    for (int i = 0; i < listSize; i++) {
                        Intent caller = list.get(i).baseIntent;
                        if (caller != null && caller.getComponent() != null) {
                            String packageName = caller.getComponent().getPackageName();
                            if (packageName.equals("com.chrislacy.linkload") && linkLoadTest == false) {
                                continue;
                            }
                            if (packageName.equals("com.google.android.apps.plus")
                                    || packageName.equals("com.twitter.android")
                                    || (packageName.equals("com.chrislacy.linkload") && linkLoadTest)) {
                                intent.setComponent(new ComponentName(this, LinkLoadOverlayService.class));
                                startService(intent);
                                break;
                            } else {
                                loadInBrowser(intent);
                                break;
                            }
                        }
                    }
                }
            } else {
                loadInBrowser(intent);
            }
            finish();
        } else {
            finish();
            //startActivity(new Intent(this, SettingsActivity.class));
            startActivity(new Intent(this, TestActivity.class));
        }
	}

    void loadInBrowser(Intent intent) {
        Intent browserIntent = getPackageManager().getLaunchIntentForPackage("com.android.chrome");
        if (browserIntent != null) {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(browserIntent.getComponent());
            intent.setPackage(browserIntent.getPackage());
            startActivity(intent);
        }
    }
    
}
