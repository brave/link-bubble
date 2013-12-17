package com.chrislacy.linkbubble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;

public class ContentActivity extends Activity {

    class ContentActivityResumedEvent {
        ContentActivity mActivity;
        ContentActivityResumedEvent(ContentActivity activity) {
            mActivity = activity;
        }
    };

    class ContentActivityPausedEvent {
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

    void updateBackgroundColor(int color) {
        mBackgroundView.setBackgroundColor(color);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}