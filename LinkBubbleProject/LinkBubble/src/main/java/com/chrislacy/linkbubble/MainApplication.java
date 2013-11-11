package com.chrislacy.linkbubble;

import android.app.Application;


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
}
