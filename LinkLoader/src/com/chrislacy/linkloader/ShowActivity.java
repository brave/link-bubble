package com.chrislacy.linkloader;

/*
Copyright 2011 jawsware international

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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

                ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RecentTaskInfo> list = activityManager.getRecentTasks(20, 0);
                int listSize = list.size();
                if(listSize > 0) {
                    for (int i = 0; i < listSize; i++) {
                        Intent caller = list.get(i).baseIntent;
                        if (caller != null && caller.getComponent() != null) {
                            String packageName = caller.getComponent().getPackageName();
                            if (packageName.equals("com.chrislacy.linkloader")) {
                                continue;
                            }
                            if (packageName.equals("com.google.android.apps.plus")
                                    || packageName.equals("com.twitter.android")) {
                                intent.setComponent(new ComponentName(this, WebReaderOverlayService.class));
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
        }

		finish();
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
