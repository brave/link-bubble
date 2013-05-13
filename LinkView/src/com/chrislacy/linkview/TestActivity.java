package com.chrislacy.linkload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TestActivity extends Activity {

    public static final String LINKLOAD_TEST = "LINKLOAD_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);

        findViewById(R.id.button_instagram).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl("http://instagram.com/p/QdCMAaweST/");
            }
        });

        findViewById(R.id.button_play_store).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl("https://play.google.com/store/apps/details?id=com.chrislacy.actionlauncher.pro");
            }
        });

        findViewById(R.id.button_google_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl("https://plus.google.com/107337299762605675178/posts");
            }
        });

        findViewById(R.id.button_maps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl("https://maps.google.com.au/maps?q=sydney&hl=en&sll=-27.40739,153.002859&sspn=1.99931,2.460938&t=h&hnear=Sydney+New+South+Wales&z=9");
            }
        });

        findViewById(R.id.button_twitter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //loadUrl("https://twitter.com/chrismlacy");
                loadUrl("http://bit.ly/TzViGL");
            }
        });

        findViewById(R.id.button_url).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUrl("http://www.techmeme.com/");
            }
        });
    }

    void loadUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.putExtra(LINKLOAD_TEST, true);
        startActivity(intent);
    }
}