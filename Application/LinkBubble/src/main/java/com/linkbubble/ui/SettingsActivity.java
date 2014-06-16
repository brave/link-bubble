package com.linkbubble.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.IconCache;


public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        MainApplication mainApplication = (MainApplication) getApplicationContext();
        if (mainApplication.mIconCache == null) {
            mainApplication.mIconCache = new IconCache(mainApplication);
        }

        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item) == true) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }
}