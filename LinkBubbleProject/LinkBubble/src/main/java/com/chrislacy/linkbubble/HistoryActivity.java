package com.chrislacy.linkbubble;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.viewpagerindicator.TabPageIndicator;


public class HistoryActivity extends FragmentActivity {

    private static final int[] CONTENT = new int[] { R.string.histroy_all, R.string.histroy_unread };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        FragmentPagerAdapter adapter = new GoogleMusicAdapter(getSupportFragmentManager());

        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);

        TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(pager);
    }

    class GoogleMusicAdapter extends FragmentPagerAdapter {
        public GoogleMusicAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return HistoryFragment.newInstance(getString(CONTENT[position % CONTENT.length]));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(CONTENT[position % CONTENT.length]).toUpperCase();
        }

        @Override
        public int getCount() {
            return CONTENT.length;
        }
    }

}
