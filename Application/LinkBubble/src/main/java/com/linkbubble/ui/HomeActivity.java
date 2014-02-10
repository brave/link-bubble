package com.linkbubble.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;
import com.squareup.otto.Subscribe;

import java.util.Vector;

public class HomeActivity extends Activity {

    private static final String TAG = "HomeActivity";

    private static final String PLAYED_INTRO_ANIM_KEY = "PlayedIntroAnimation";

    View mContentView;
    View mBackgroundView;
    View mTopButtonsContainerView;
    Button mActionButtonView;
    View mHistoryCircleButtonView;
    View mSettingsCircleButtonView;
    FlipView mStatsFlipView;
    CondensedTextView mTimeSavedPerLinkTextView;
    CondensedTextView mTimeSavedTotalTextView;

    boolean mPlayedIntroAnimation;

    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        setContentView(R.layout.activity_home);

        boolean isLicensed = DRM.isLicensed();

        mBackgroundView = findViewById(R.id.background);
        mContentView = findViewById(R.id.content);
        mTopButtonsContainerView = findViewById(R.id.top_buttons_container);
        mHistoryCircleButtonView = findViewById(R.id.history_circle);
        mSettingsCircleButtonView = findViewById(R.id.settings_circle);
        mActionButtonView = (Button)findViewById(R.id.big_white_button);
        mStatsFlipView = (FlipView) findViewById(R.id.stats_flip_view);
        mTimeSavedPerLinkTextView = (CondensedTextView) mStatsFlipView.getDefaultView().findViewById(R.id.time_per_link);
        mTimeSavedPerLinkTextView.setText("");
        mTimeSavedTotalTextView = (CondensedTextView) mStatsFlipView.getFlippedView().findViewById(R.id.time_total);
        mTimeSavedTotalTextView.setText("");

        if (savedInstanceState != null) {
            mPlayedIntroAnimation = savedInstanceState.getBoolean(PLAYED_INTRO_ANIM_KEY);
        }

        if (mPlayedIntroAnimation) {
            mBackgroundView.setAlpha(1.f);
            mContentView.setVisibility(View.VISIBLE);
        }

        if (Settings.get().debugAutoLoadUrl()) {
            MainApplication.openLink(this, "https://s3.amazonaws.com/linkbubble/test.html");
            //MainApplication.openLink(getActivity(), "https://twitter.com/lokibartleby/status/412160702707539968", false);
        }

        Vector<String> urls = Settings.get().loadCurrentTabs();
        if (urls.size() > 0) {
            MainApplication.restoreLinks(this, urls.toArray(new String[urls.size()]));
        }

        if (isLicensed) {
            mActionButtonView.setText(R.string.history);
            mHistoryCircleButtonView.setVisibility(View.GONE);
        } else {
            mActionButtonView.setText(R.string.action_upgrade_to_pro);
        }

        mActionButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DRM.isLicensed()) {
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class), v, false);
                } else {
                    Intent intent = Config.getStoreIntent(HomeActivity.this, Config.STORE_PRO_URL);
                    if (intent != null) {
                        startActivity(intent);
                    }
                }
            }
        });

        mHistoryCircleButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, HistoryActivity.class), v, true);
            }
        });

        mSettingsCircleButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class), v, true);
            }
        });

        MainApplication.registerForBus(this, this);
    }

    @Override
    public void onDestroy() {
        MainApplication.unregisterForBus(this, this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLinkLoadTimeStats();

        if (mPlayedIntroAnimation == false) {
            animateOn();
            mPlayedIntroAnimation = true;
        }

        MainApplication.checkForProVersion(getApplicationContext());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(PLAYED_INTRO_ANIM_KEY, mPlayedIntroAnimation);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mPlayedIntroAnimation = savedInstanceState.getBoolean(PLAYED_INTRO_ANIM_KEY);
    }

    void animateOn() {

        mBackgroundView.setScaleX(0.0f);
        mBackgroundView.animate().alpha(1f).scaleX(1f).start();

        mContentView.setAlpha(0f);
        mContentView.setVisibility(View.VISIBLE);
        mContentView.setScaleX(0.5f);
        mContentView.setScaleY(0.5f);
        mContentView.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .start();

        mActionButtonView.setAlpha(0f);
        mActionButtonView.setVisibility(View.VISIBLE);
        mActionButtonView.animate().alpha(1f).setDuration(250).setStartDelay(750).start();

        mTopButtonsContainerView.setAlpha(0f);
        mTopButtonsContainerView.setVisibility(View.VISIBLE);
        mTopButtonsContainerView.animate().alpha(1f).setDuration(250).setStartDelay(750).start();
    }

    private void updateLinkLoadTimeStats() {
        long timeSavedPerLink = Settings.get().getTimeSavedPerLink();
        if (timeSavedPerLink > -1) {
            String prettyTimeElapsed = Util.getPrettyTimeElapsed(getResources(), timeSavedPerLink, "\n");
            mTimeSavedPerLinkTextView.setText(prettyTimeElapsed);
            Log.d(Settings.LOAD_TIME_TAG, "*** " + (prettyTimeElapsed.replace("\n", " ")));
        }

        long totalTimeSaved = Settings.get().getTotalTimeSaved();
        if (totalTimeSaved > -1) {
            String prettyTimeElapsed = Util.getPrettyTimeElapsed(getResources(), totalTimeSaved, "\n");
            mTimeSavedTotalTextView.setText(prettyTimeElapsed);
            Log.d(Settings.LOAD_TIME_TAG, "*** " + (prettyTimeElapsed.replace("\n", " ")));
        }
    }

    void startActivity(Intent intent, View view, boolean tamperCheck) {

        if (tamperCheck) {
            boolean tampered = Util.showTamperPrompt(this, new Prompt.OnPromptEventListener() {
                @Override
                public void onClick() {
                    Config.openAppStore(HomeActivity.this);
                }

                @Override
                public void onClose() {

                }
            });

            if (tampered) {
                return;
            }
        }

        boolean useLaunchAnimation = (view != null) &&
                !intent.hasExtra(Constant.INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);

        if (useLaunchAnimation) {
            ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                    view.getMeasuredWidth(), view.getMeasuredHeight());

            startActivity(intent, opts.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onLinkLoadTimeStatsUpdatedEvent(Settings.LinkLoadTimeStatsUpdatedEvent event) {
        updateLinkLoadTimeStats();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStateChangedEvent(DRM.StateChangedEvent event) {
        //android.os.Process.killProcess(android.os.Process.myPid());
        //finish();
        //startActivity(getIntent());
    }
}
