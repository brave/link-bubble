/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.linkbubble.MainApplication;
import com.linkbubble.MainService;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Analytics;
import com.squareup.otto.Subscribe;

public class TermsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_terms);

        TextView acceptTermsTextView = (TextView)findViewById(R.id.accept_terms_and_privacy_text);
        acceptTermsTextView.setText(Html.fromHtml(getString(R.string.accept_terms_and_privacy)));
        acceptTermsTextView.setMovementMethod(LinkMovementMethod.getInstance());
        Button acceptTermsButton = (Button)findViewById(R.id.accept_terms_and_privacy_button);
        acceptTermsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings settings = Settings.get();
                if (null != settings) {
                    settings.setTermsAccepted(true);
                }
                MainApplication.checkRestoreCurrentTabs(getApplicationContext());
                MainApplication.openLink(getApplicationContext(), getString(R.string.empty_bubble_page), Analytics.OPENED_URL_FROM_MAIN_NEW_TAB);
                finish();
            }
        });
        MainApplication.registerForBus(this, this);
    }

    @Override
    public void onDestroy() {
        MainApplication.unregisterForBus(this, this);
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void OnDestroyMainServiceEvent(MainService.OnDestroyMainServiceEvent event) {
        finish();
    }
}
