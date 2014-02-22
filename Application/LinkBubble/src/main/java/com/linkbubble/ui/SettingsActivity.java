package com.linkbubble.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.util.CrashTracking;


public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);
    }

    @Override
    public void onStart() {
        super.onStart();
        MainApplication.checkRestoreCurrentTabs(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }
}