package com.chrislacy.linkbubble;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Settings.initModule(this);
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        Settings.deinitModule();

        super.onTerminate();
    }

    public static void openLink(Context context, String url, boolean recordHistory) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("url", url);
        serviceIntent.putExtra("record_history", recordHistory);
        context.startService(serviceIntent);
    }

    public static void loadInBrowser(Context context, Intent intent) {
        boolean activityStarted = false;
        String defaultBrowserPackageName = Settings.get().getDefaultBrowserPackageName();
        if (defaultBrowserPackageName != null) {
            Intent browserIntent = context.getPackageManager().getLaunchIntentForPackage(defaultBrowserPackageName);
            if (browserIntent != null) {
                intent.setComponent(browserIntent.getComponent());
                intent.setPackage(browserIntent.getPackage());
                context.startActivity(intent);
                activityStarted = true;
            }
        }

        if (activityStarted == false) {
            Toast.makeText(context, R.string.no_default_browser, Toast.LENGTH_LONG).show();
        }
    }
}
