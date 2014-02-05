package com.linkbubble.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.Settings;

import java.util.Vector;

public class HomeActivity extends Activity {

    private static final String TAG = "HomeActivity";

    private static final String PLAYED_INTRO_ANIM_KEY = "PlayedIntroAnimation";

    View mContentView;
    View mBackgroundView;
    View mHistoryButtonView;
    View mSettingsButtonView;
    FlipView mStatsFlipView;
    CondensedTextView mTimeSavedPerLinkTextView;
    CondensedTextView mTimeSavedTotalTextView;

    boolean mPlayedIntroAnimation;

    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        mBackgroundView = findViewById(R.id.background);
        mContentView = findViewById(R.id.content);
        mSettingsButtonView = findViewById(R.id.settings);
        mHistoryButtonView = findViewById(R.id.history);
        mStatsFlipView = (FlipView) findViewById(R.id.stats_flip_view);
        mTimeSavedPerLinkTextView = (CondensedTextView) mStatsFlipView.getDefaultView().findViewById(R.id.time_per_link);
        mTimeSavedTotalTextView = (CondensedTextView) mStatsFlipView.getFlippedView().findViewById(R.id.time_total);

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

        Vector<String> urls = Settings.get().loadCurrentBubbles();
        if (urls.size() > 0) {
            MainApplication.restoreLinks(this, urls.toArray(new String[urls.size()]));
        }

        View historyButton = findViewById(R.id.history);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, HistoryActivity.class), v);
            }
        });

        View settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class), v);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        long timeSavedPerLink = Settings.get().getTimeSavedPerLink();
        if (timeSavedPerLink > -1) {
            String timeSaved = String.format("%.1f", (float)timeSavedPerLink / 1000.f);
            String text = String.format(getString(R.string.stat_saved_per_link), timeSaved);
            mTimeSavedPerLinkTextView.setText(text);
            Log.d(Settings.LOAD_TIME_TAG, "*** " + text);
        }

        long totalTimeSaved = Settings.get().getTotalTimeSaved();
        if (totalTimeSaved > -1) {
            float secondsSaved = (float)totalTimeSaved / 1000.f;
            String timeSaved = String.format("%.1f", secondsSaved);
            String text = String.format(getString(R.string.stat_saved_per_link), timeSaved);
            mTimeSavedTotalTextView.setText(text);
            Log.d(Settings.LOAD_TIME_TAG, "*** " + text);
        }

        if (mPlayedIntroAnimation == false) {
            animateOn();
            mPlayedIntroAnimation = true;
        }
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

        mHistoryButtonView.setAlpha(0f);
        mHistoryButtonView.setVisibility(View.VISIBLE);
        mHistoryButtonView.animate().alpha(1f).setDuration(250).setStartDelay(750).start();

        mSettingsButtonView.setAlpha(0f);
        mSettingsButtonView.setVisibility(View.VISIBLE);
        mSettingsButtonView.animate().alpha(1f).setDuration(250).setStartDelay(750).start();
    }

    void startActivity(Intent intent, View view) {
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

}
