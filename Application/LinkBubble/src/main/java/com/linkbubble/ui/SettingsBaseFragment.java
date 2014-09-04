package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.ScrollView;
import android.widget.TextView;

import com.linkbubble.BuildConfig;
import com.linkbubble.DRM;
import com.linkbubble.R;
import com.linkbubble.util.Util;

public class SettingsBaseFragment extends PreferenceFragment {

    void showProBanner(Preference preference) {
        if (DRM.isLicensed() == false) {
            preference.setLayoutResource(R.layout.preference_pro_banner);
        }
    }

    long mLastUpsellTime;
    void upsellPro(int stringId) {
        if (System.currentTimeMillis() - mLastUpsellTime < 100) {
            return;
        }

        TextView textView = new TextView(getActivity());
        int padding = getResources().getDimensionPixelSize(R.dimen.upgrade_to_pro_dialog_padding);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(getString(stringId) + "\n\n" + getString(R.string.upgrade_from_settings_summary));

        ScrollView layout = new ScrollView(getActivity());
        layout.addView(textView);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(layout);
        builder.setIcon(0);
        builder.setPositiveButton(R.string.upgrade, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.STORE_PRO_URL));
                startActivity(intent);
            }
        });
        builder.setTitle(R.string.upgrade_to_pro);

        AlertDialog alertView = builder.create();
        Util.showThemedDialog(alertView);
        mLastUpsellTime = System.currentTimeMillis();
    }

}
