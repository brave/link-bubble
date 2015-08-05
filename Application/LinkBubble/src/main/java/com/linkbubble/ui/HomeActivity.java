package com.linkbubble.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
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

    LinearLayout mActivityLayout;
    EditText mUrlEntry;
    Button mActionButtonView;
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

        ((MainApplication)getApplicationContext()).registerDrmTracker(this);

        mActivityLayout = (LinearLayout)findViewById(R.id.activity_layout);
        mUrlEntry = (EditText)findViewById(R.id.url_entry);
        mActionButtonView = (Button)findViewById(R.id.big_white_button);
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

        configureForDrmState();

        mUrlEntry.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String userInput = mUrlEntry.getText().toString().trim();
                    MainApplication.openLink(getApplicationContext(), userInput, null);
                    mUrlEntry.setText("");
                    // Focus on the activity layout to hide the caret.
                    mActivityLayout.requestFocus();
                    return true;
                }
                return false;
            }
        });

        final int clearButtonSize = getResources().getDimensionPixelSize(R.dimen.url_box_clear_button_size);
        final Drawable clearButtonDrawable = getResources().getDrawable(R.drawable.ic_highlight_remove_grey600_24dp);
        clearButtonDrawable.setBounds(0, 0, clearButtonSize, clearButtonSize);
        mUrlEntry.setCompoundDrawables(null, null, null, null);
        mUrlEntry.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mUrlEntry.getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > mUrlEntry.getWidth() - mUrlEntry.getPaddingRight() - clearButtonSize) {
                    mUrlEntry.setText("");
                    mUrlEntry.setCompoundDrawables(null, null, null, null);
                }
                return false;
            }
        });

        mUrlEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUrlEntry.setCompoundDrawables(null, null, mUrlEntry.getText().toString().equals("") ? null : clearButtonDrawable, null);
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

        mUrlEntry.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        mActionButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DRM.isLicensed()) {
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class), v);
                } else {
                    Intent intent = MainApplication.getStoreIntent(HomeActivity.this, BuildConfig.STORE_PRO_URL);
                    if (intent != null) {
                        startActivity(intent);
                    }
                }
            }
        });

        MainApplication.registerForBus(this, this);

        Settings.get().getBrowsers();
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_home, menu);
        if (DRM.isLicensed()) {
            menu.removeItem(R.id.action_history);
        }
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

            case R.id.action_history:
                startActivity(new Intent(HomeActivity.this, HistoryActivity.class), item.getActionView());
                return true;
        }

        return false;
    }

    private void configureForDrmState() {
        if (DRM.isLicensed()) {
            mActionButtonView.setText(R.string.history);
        } else {
            mActionButtonView.setText(R.string.action_upgrade_to_pro);
        }
    }

    @Override
    public void onDestroy() {
        ((MainApplication)getApplicationContext()).unregisterDrmTracker(this);
        MainApplication.unregisterForBus(this, this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateLinkLoadTimeStats();

        configureForDrmState();

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

    @SuppressWarnings("unused")
    @Subscribe
    public void onStateChangedEvent(MainApplication.StateChangedEvent event) {
        configureForDrmState();

        if (event.mOldState != DRM.LICENSE_VALID
                && event.mState == DRM.LICENSE_VALID
                && event.mDisplayToast
                && event.mDisplayedToast == false) {
            Toast.makeText(this, R.string.valid_license_detected, Toast.LENGTH_LONG).show();
            event.mDisplayedToast = true;
        }
    }
}
