package com.chrislacy.linkbubble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import com.flavienlaurent.notboringactionbar.KenBurnsView;

import java.util.Vector;

public class HomeActivity extends Activity {

    private static final String TAG = "HomeActivity";
    private KenBurnsView mHeaderPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        mHeaderPicture = (KenBurnsView) findViewById(R.id.header_picture);
        mHeaderPicture.setResourceIds(R.drawable.the_internet, R.drawable.the_internet);

        if (Settings.get().debugAutoLoadUrl()) {
            MainApplication.openLink(this, "http://abc.net.au");
            //MainApplication.openLink(getActivity(), "https://twitter.com/lokibartleby/status/412160702707539968", false);
        }

        Vector<String> urls = Settings.get().loadCurrentBubbles();
        for (String url : urls) {
            MainApplication.openLink(this, url);
        }

        if (MainController.get() != null && MainController.get().getBubbleCount() > 0) {
            FancyCoverFlow fancyCoverFlow = (FancyCoverFlow) findViewById(R.id.fancyCoverFlow);

            BubbleCoverFlowAdapter adapter = new BubbleCoverFlowAdapter(this, MainController.get().getBubbles(), false);
            fancyCoverFlow.setAdapter(adapter);
            fancyCoverFlow.setSelection(adapter.getStartIndex(), false);
        }
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

            case R.id.action_upgrade_to_pro: {
                Intent intent = Config.getStoreIntent(this, Config.STORE_PRO_URL);
                if (intent != null) {
                    startActivity(intent);
                    return true;
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

}
