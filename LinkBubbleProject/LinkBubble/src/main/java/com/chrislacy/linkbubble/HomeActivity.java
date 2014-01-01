package com.chrislacy.linkbubble;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;

import java.util.Vector;

public class HomeActivity extends Activity {

    private static final String TAG = "HomeActivity";

    FrameLayout mContent;
    ImageView logo;
    View bg;

    int mCount;
    final Handler mHandler = new Handler();
    //static final int BGCOLOR = 0xffed1d24;
    static final int BGCOLOR = 0xff3fcdfd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
        //Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);

        mContent = new FrameLayout(this);
        //mContent.setBackgroundColor(0xC0000000);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;

        logo = new ImageView(this);
        logo.setImageResource(R.drawable.text_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setVisibility(View.INVISIBLE);

        bg = new View(this);
        bg.setBackgroundColor(BGCOLOR);
        bg.setAlpha(0f);

        /*
        final TextView tv = new TextView(this);
        if (light != null) tv.setTypeface(light);
        tv.setTextSize(30);
        tv.setPadding(p, p, p, p);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setTransformationMethod(new AllCapsTransformationMethod(this));
        tv.setText("Android " + Build.VERSION.RELEASE);
        tv.setVisibility(View.INVISIBLE);
        */

        mContent.addView(bg);
        mContent.addView(logo, lp);

        //final FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(lp);
        //lp2.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        //lp2.bottomMargin = 10*p;

        //mContent.addView(tv, lp2);

        setContentView(mContent);

        if (Settings.get().debugAutoLoadUrl()) {
            MainApplication.openLink(this, "http://abc.net.au");
            //MainApplication.openLink(getActivity(), "https://twitter.com/lokibartleby/status/412160702707539968", false);
        }

        Vector<String> urls = Settings.get().loadCurrentBubbles();
        for (String url : urls) {
            MainApplication.openLink(this, url);
        }

        /*
        if (MainController.get() != null && MainController.get().getBubbleCount() > 0) {
            FancyCoverFlow fancyCoverFlow = (FancyCoverFlow) findViewById(R.id.fancyCoverFlow);

            BubbleCoverFlowAdapter adapter = new BubbleCoverFlowAdapter(this, MainController.get().getBubbles(), false);
            fancyCoverFlow.setAdapter(adapter);
            fancyCoverFlow.setSelection(adapter.getStartIndex(), false);
        }*/

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setIcon(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        animateOn();
    }

    void animateOn() {

        bg.setScaleX(0.0f);
        bg.animate().alpha(1f).scaleX(1f).start();

        logo.setAlpha(0f);
        logo.setVisibility(View.VISIBLE);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .start();

        //tv.setAlpha(0f);
        //tv.setVisibility(View.VISIBLE);
        //tv.animate().alpha(1f).setDuration(1000).setStartDelay(1000).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_activity, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_history:
                startActivityForResult(new Intent(this, HistoryActivity.class), 0);
                break;

            case R.id.action_settings: {
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
            }

            /*
            case R.id.action_upgrade_to_pro: {
                Intent intent = Config.getStoreIntent(this, Config.STORE_PRO_URL);
                if (intent != null) {
                    startActivity(intent);
                    return true;
                }
            }*/
        }

        return super.onOptionsItemSelected(item);
    }

}
