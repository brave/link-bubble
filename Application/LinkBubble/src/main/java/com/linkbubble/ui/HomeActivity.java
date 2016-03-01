/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;
import com.squareup.otto.Subscribe;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    Button mActionButtonView;
    Button mNewBubble;
    FlipView mStatsFlipView;
    View mTimeSavedPerLinkContainerView;
    CondensedTextView mTimeSavedPerLinkTextView;
    CondensedTextView mTimeSavedTotalTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Analytics.trackScreenView(HomeActivity.class.getSimpleName());

        mActionButtonView = (Button)findViewById(R.id.big_white_button);
        mNewBubble = (Button)findViewById(R.id.new_bubble);
        mStatsFlipView = (FlipView) findViewById(R.id.stats_flip_view);
        mTimeSavedPerLinkContainerView = mStatsFlipView.getDefaultView();
        mTimeSavedPerLinkTextView = (CondensedTextView) mTimeSavedPerLinkContainerView.findViewById(R.id.time_per_link);
        mTimeSavedPerLinkTextView.setText("");
        mTimeSavedTotalTextView = (CondensedTextView) mStatsFlipView.getFlippedView().findViewById(R.id.time_total);
        mTimeSavedTotalTextView.setText("");

        if (!Settings.get().getTermsAccepted()) {
            final FrameLayout rootView = (FrameLayout)findViewById(android.R.id.content);

            final View acceptTermsView = getLayoutInflater().inflate(R.layout.view_accept_terms, null);
            TextView acceptTermsTextView = (TextView)acceptTermsView.findViewById(R.id.accept_terms_and_privacy_text);
            acceptTermsTextView.setText(Html.fromHtml(getString(R.string.accept_terms_and_privacy)));
            acceptTermsTextView.setMovementMethod(LinkMovementMethod.getInstance());
            Button acceptTermsButton = (Button)acceptTermsView.findViewById(R.id.accept_terms_and_privacy_button);
            acceptTermsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Settings.get().setTermsAccepted(true);
                    if (rootView != null) {
                        rootView.removeView(acceptTermsView);
                    }
                }
            });
            acceptTermsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // do nothing, but prevent clicks from flowing to item underneath
                }
            });

            if (rootView != null) {
                rootView.addView(acceptTermsView);
            }
        }

        if (!Settings.get().getWelcomeMessageDisplayed()) {
            boolean showWelcomeUrl = true;
            if (MainController.get() != null && MainController.get().isUrlActive(Constant.WELCOME_MESSAGE_URL)) {
                showWelcomeUrl = false;
            }
            if (showWelcomeUrl) {
                MainApplication.openLink(this, Constant.WELCOME_MESSAGE_URL, null);
            }
        }

        if (Settings.get().debugAutoLoadUrl()) {
            MainApplication.openLink(this, "file:///android_asset/test.html", null);
        }

        mActionButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, HistoryActivity.class), v);
            }
        });

        mNewBubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to do debug
                //Intent intent = new Intent(HomeActivity.this, BubbleFlowActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //startActivity(intent);
                //
                MainApplication.openLink(HomeActivity.this, HomeActivity.this.getString(R.string.empty_bubble_page),
                        Analytics.OPENED_URL_FROM_MAIN_NEW_TAB);
            }
        });

        MainApplication.registerForBus(this, this);

        Settings.get().getBrowsers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                return true;
            }

            case R.id.action_settings:
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class), item.getActionView());
                return true;
        }

        return false;
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

        MainApplication.postEvent(getApplicationContext(), new MainApplication.CheckStateEvent());
    }

    @Override
    public void onStart() {
        super.onStart();

        MainApplication.checkRestoreCurrentTabs(this);
    }

    private void updateLinkLoadTimeStats() {
        long timeSavedPerLink = Settings.get().getTimeSavedPerLink();
        if (timeSavedPerLink > -1) {
            String prettyTimeElapsed = Util.getPrettyTimeElapsed(getResources(), timeSavedPerLink, "\n");
            mTimeSavedPerLinkTextView.setText(prettyTimeElapsed);
            Log.d(Settings.LOAD_TIME_TAG, "*** " + (prettyTimeElapsed.replace("\n", " ")));
        } else {
            String prettyTimeElapsed = Util.getPrettyTimeElapsed(getResources(), 0, "\n");
            mTimeSavedPerLinkTextView.setText(prettyTimeElapsed);
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onLinkLoadTimeStatsUpdatedEvent(Settings.LinkLoadTimeStatsUpdatedEvent event) {
        updateLinkLoadTimeStats();
    }
}
