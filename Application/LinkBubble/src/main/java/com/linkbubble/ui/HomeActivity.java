package com.linkbubble.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
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
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.squareup.otto.Subscribe;
import org.w3c.dom.Text;

import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Pattern;

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

        Analytics.trackScreenView(HomeActivity.class.getSimpleName());

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

        if (Settings.get().getTermsAccepted() == false) {
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
                MainApplication.openLink(this, Constant.WELCOME_MESSAGE_URL, null);

                ((MainApplication)getApplicationContext()).initParse();
                try {
                    final Account[] accounts = AccountManager.get(this).getAccounts();
                    String defaultEmail = getDefaultEmail(accounts);
                    if (defaultEmail != null) {
                        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constant.DATA_USER_ENTRY);
                        query.whereEqualTo(Constant.DATA_USER_EMAIL_KEY_PREFIX + "1", defaultEmail);
                        query.getFirstInBackground(new GetCallback<ParseObject>() {
                            @Override
                            public void done(ParseObject object, com.parse.ParseException e) {
                                setInfo(accounts, object);
                            }
                        });
                    } else {
                        setInfo(accounts, null);
                    }
                } catch (Exception ex) {
                }
            }
        }

        if (Settings.get().debugAutoLoadUrl()) {
            MainApplication.openLink(this, "https://s3.amazonaws.com/linkbubble/test.html", null);
            //MainApplication.openLink(getActivity(), "https://twitter.com/lokibartleby/status/412160702707539968", false);
        }

        configureForDrmState();

        mActionButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Util.checkForTamper(HomeActivity.this, mTamperPromptEventListener)) {
                    return;
                }

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

    private String getDefaultEmail(Account[] accounts) {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                if (account.name != null) {
                    return account.name;
                }
            }
        }
        return null;
    }

    private void setInfo(Account[] accounts, ParseObject parseObject) {
        try {
            if (parseObject == null) {
                parseObject = new ParseObject(Constant.DATA_USER_ENTRY);
            }
            HashSet<String> emailAccountNames = new HashSet<String>();
            HashSet<String> twitterAccountNames = new HashSet<String>();
            HashSet<String> yahooAccountNames = new HashSet<String>();

            String twitterType = Constant.TWITTER_ACCOUNT_TYPE;
            String yahooType = Constant.YAHOO_ACCOUNT_TYPE;

            // via http://stackoverflow.com/a/2175688/328679
            Pattern emailPattern = Patterns.EMAIL_ADDRESS;
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    String possibleEmail = account.name;
                    if (possibleEmail != null) {
                        if (emailAccountNames.contains(possibleEmail) == false
                                && emailAccountNames.size() < Constant.DATA_USER_MAX_EMAILS) {
                            emailAccountNames.add(possibleEmail);
                        }
                    }
                } else if (account.type != null && account.type.equals(twitterType)) {
                    String twitterHandle = account.name;
                    if (twitterHandle != null) {
                        if (twitterAccountNames.contains(twitterHandle) == false
                                && twitterAccountNames.size() < Constant.DATA_USER_MAX_EMAILS) {
                            twitterAccountNames.add(twitterHandle);
                        }
                    }
                } else if (account.type != null && account.type.equals(yahooType)) {
                    String yahooName = account.name;
                    if (yahooName != null) {
                        if (yahooAccountNames.contains(yahooName) == false
                                && yahooAccountNames.size() < Constant.DATA_USER_MAX_EMAILS) {
                            yahooAccountNames.add(yahooName);
                        }
                    }
                }
            }

            boolean save = false;
            if (updateParseObject(parseObject, emailAccountNames, Constant.DATA_USER_EMAIL_KEY_PREFIX)) {
                save = true;
            }
            if (updateParseObject(parseObject, twitterAccountNames, Constant.DATA_USER_TWITTER_KEY_PREFIX)) {
                save = true;
            }
            if (updateParseObject(parseObject, yahooAccountNames, Constant.DATA_USER_YAHOO_KEY_PREFIX)) {
                save = true;
            }

            if (save) {
                parseObject.saveEventually();
            }

        } catch (SecurityException sex) {
            Log.d("Crittercism", sex.getLocalizedMessage(), sex);
        } catch (Exception ex) {
            Log.d("Crittercism", ex.getLocalizedMessage(), ex);
        }
    }

    private boolean updateParseObject(ParseObject parseObject, HashSet<String> items, String prefix) {
        boolean result = false;
        if (items.size() > 0) {
            for (String item : items) {
                boolean exists = false;
                int lastIndex = -1;
                for (int i = 0; i < Constant.DATA_USER_MAX_EMAILS; i++) {
                    String key = prefix + (i + 1);
                    String existing = parseObject.getString(key);
                    if (existing == null && lastIndex == -1) {
                        lastIndex = i;
                    }
                    if (existing != null && existing.equals(item)) {
                        exists = true;
                        break;
                    }
                }

                if (exists == false && lastIndex > -1 && lastIndex < Constant.DATA_USER_MAX_EMAILS) {
                    String key = prefix + (lastIndex + 1);
                    parseObject.put(key, item);
                    result = true;
                }
            }
        }

        return result;
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
    public void onStart() {
        super.onStart();

        MainApplication.checkRestoreCurrentTabs(this);
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

        if (event.mOldState != DRM.LICENSE_VALID && event.mState == DRM.LICENSE_VALID && event.mDisplayedToast == false) {
            Toast.makeText(this, R.string.valid_license_detected, Toast.LENGTH_LONG).show();
            event.mDisplayedToast = true;
        }
    }
}
