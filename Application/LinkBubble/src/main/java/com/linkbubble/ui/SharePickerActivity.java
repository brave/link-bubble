package com.linkbubble.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.CrashTracking;

import java.net.MalformedURLException;
import java.net.URL;

public class SharePickerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        boolean validUrl = false;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        String urlAsString = getIntent().getDataString();
        if(extras != null && urlAsString == null) {
            if (intent.getAction() == Intent.ACTION_SEND) {
                if (intent.getType().equals("text/plain") && extras.containsKey(Intent.EXTRA_TEXT)) {
                    urlAsString = extras.getString(Intent.EXTRA_TEXT);
                }
            }
        }

        if (urlAsString != null) {
            try {
                new URL(urlAsString);
                final String finalUrlAsString = urlAsString;
                AlertDialog alertDialog = ActionItem.getShareAlert(this, new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.setClassName(actionItem.mPackageName, actionItem.mActivityClassName);
                        intent.putExtra(Intent.EXTRA_TEXT, finalUrlAsString);
                        startActivity(intent);
                        finish();
                    }
                });
                alertDialog.show();
                validUrl = true;
            } catch (MalformedURLException e) {
            }
        }

        if (validUrl == false) {
            Toast.makeText(this, R.string.share_invalid_data, Toast.LENGTH_LONG).show();
            finish();
        }
    }

}
