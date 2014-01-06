package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.crashlytics.android.Crashlytics;
import com.linkbubble.MainApplication;
import com.linkbubble.R;

public class ContentActivity extends Activity {

    static final boolean DEBUG_DRAW = false;

    public class ContentActivityResumedEvent {
        public ContentActivity mActivity;
        ContentActivityResumedEvent(ContentActivity activity) {
            mActivity = activity;
        }
    };

    public class ContentActivityPausedEvent {
        ContentActivity mActivity;
        ContentActivityPausedEvent(ContentActivity activity) {
            mActivity = activity;
        }
    };

    View mBackgroundView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        setContentView(R.layout.activity_content);

        mBackgroundView = findViewById(R.id.background);
        if (DEBUG_DRAW) {
            updateBackgroundColor(0x00000000);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainApplication)getApplicationContext()).getBus().post(new ContentActivityResumedEvent(this));
    }

    @Override
    public void onPause() {
        ((MainApplication)getApplicationContext()).getBus().post(new ContentActivityPausedEvent(this));
        super.onPause();
    }

    public void updateBackgroundColor(int color) {
        mBackgroundView.setBackgroundColor(DEBUG_DRAW ? 0x5500ff00 : color);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}