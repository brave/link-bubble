package samples.jawsware.interactiveoverlay;

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

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

public class SampleOverlayShowActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent.getAction() == Intent.ACTION_VIEW) {

            Uri data = intent.getData();
            String scheme = data.getScheme();

            ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RecentTaskInfo> list = activityManager.getRecentTasks(1, 0);
            if(list.size() > 0) {
                Intent caller = list.get(0).baseIntent;
                if (caller != null && caller.getComponent() != null) {
                    String packageName = caller.getComponent().getPackageName();
                    if (packageName.equals("com.google.android.apps.plus")) {
                        //startService(new Intent(this, SampleOverlayService.class));
                        intent.setComponent(new ComponentName(this, SampleOverlayService.class));
                        startService(intent);
                        //startSer
                    } else {
                        Intent browserIntent = getPackageManager().getLaunchIntentForPackage("com.android.chrome");
                        if (browserIntent != null) {
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.setComponent(browserIntent.getComponent());
                            intent.setPackage(browserIntent.getPackage());
                            startActivity(intent);
                        }
                    }
                }
            }
        }

		finish();
		
	}
    
}
