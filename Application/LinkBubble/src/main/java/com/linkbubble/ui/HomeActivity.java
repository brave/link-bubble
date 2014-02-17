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
import android.widget.Toast;
import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
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
    View mTimeSavedPerLinkContainerView;
    CondensedTextView mTimeSavedPerLinkTextView;
    CondensedTextView mTimeSavedTotalTextView;

    boolean mPlayedIntroAnimation;

    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        setContentView(R.layout.activity_home);

        mBackgroundView = findViewById(R.id.background);
        mContentView = findViewById(R.id.content);
        mTopButtonsContainerView = findViewById(R.id.top_buttons_container);
        mHistoryCircleButtonView = findViewById(R.id.history_circle);
        mSettingsCircleButtonView = findViewById(R.id.settings_circle);
        mActionButtonView = (Button)findViewById(R.id.big_white_button);
        mStatsFlipView = (FlipView) findViewById(R.id.stats_flip_view);
        mTimeSavedPerLinkContainerView = mStatsFlipView.getDefaultView();
        mTimeSavedPerLinkTextView = (CondensedTextView) mTimeSavedPerLinkContainerView.findViewById(R.id.time_per_link);
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

        if (Settings.get().getWelcomeMessageDisplayed() == false) {
            boolean showWelcomeUrl = true;
            if (MainController.get() != null && MainController.get().isUrlActive(Constant.WELCOME_MESSAGE_URL)) {
                showWelcomeUrl = false;
            }
            if (showWelcomeUrl) {
                MainApplication.openLink(this, Constant.WELCOME_MESSAGE_URL);
            }
        }

        if (Settings.get().debugAutoLoadUrl()) {
            MainApplication.openLink(this, "https://s3.amazonaws.com/linkbubble/test.html");
            //MainApplication.openLink(getActivity(), "https://twitter.com/lokibartleby/status/412160702707539968", false);
        }

        Vector<String> urls = Settings.get().loadCurrentTabs();
        if (urls.size() > 0) {
            MainApplication.restoreLinks(this, urls.toArray(new String[urls.size()]));
        }

        configureForDrmState();

        mActionButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DRM.isLicensed()) {
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class), v, false);
                } else {
                    Intent intent = MainApplication.getStoreIntent(HomeActivity.this, BuildConfig.STORE_PRO_URL);
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

    private void configureForDrmState() {
        if (DRM.isLicensed()) {
            mActionButtonView.setText(R.string.history);
            mHistoryCircleButtonView.setVisibility(View.GONE);
        } else {
            mActionButtonView.setText(R.string.action_upgrade_to_pro);
        }
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

        configureForDrmState();

        MainApplication.checkForProVersion(getApplicationContext());
        Util.checkForTamper(this, mTamperPromptEventListener);
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
        } else {
            // The "time saved so far == 0" link is a better one to display when there's no data yet
            if (mStatsFlipView.getDefaultView() == mTimeSavedPerLinkContainerView) {
                mStatsFlipView.toggleFlip(false);
            }
        }

        long totalTimeSaved = Settings.get().getTotalTimeSaved();
        if (totalTimeSaved > -1) {
            String prettyTimeElapsed = Util.getPrettyTimeElapsed(getResources(), totalTimeSaved, "\n");
            mTimeSavedTotalTextView.setText(prettyTimeElapsed);
            Log.d(Settings.LOAD_TIME_TAG, "*** " + (prettyTimeElapsed.replace("\n", " ")));
        }
    }

    Prompt.OnPromptEventListener mTamperPromptEventListener = new Prompt.OnPromptEventListener() {
        @Override
        public void onClick() {
            MainApplication.openAppStore(HomeActivity.this, BuildConfig.STORE_FREE_URL);
        }

        @Override
        public void onClose() {

        }
    };

    void startActivity(Intent intent, View view, boolean tamperCheck) {

        if (tamperCheck) {
            if (Util.checkForTamper(this, mTamperPromptEventListener)) {
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
    public void onStateChangedEvent(MainApplication.StateChangedEvent event) {
        configureForDrmState();
        if (event.mOldState != DRM.LICENSE_VALID && event.mState == DRM.LICENSE_VALID) {
            Toast.makeText(this, R.string.valid_license_detected, Toast.LENGTH_LONG).show();
        }
    }
}
