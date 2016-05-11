/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.MainService;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.db.DatabaseHelper;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.Util;
import com.squareup.otto.Subscribe;

import org.mozilla.gecko.favicons.Favicons;
import org.mozilla.gecko.favicons.LoadFaviconTask;
import org.mozilla.gecko.favicons.OnFaviconLoadedListener;
import org.mozilla.gecko.widget.FaviconView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class HistoryActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private TextView mMessageView;
    private ListView mListView;
    private HistoryAdapter mHistoryAdapter;
    private List<HistoryRecord> mHistoryRecords;

    private static int sInstanceCount = 0;
    private static Favicons sFavicons = null;
    private static final int FAVICON_CACHE_SIZE = 4 * 1024 * 1024;
    public boolean mStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sInstanceCount++;
        if (sInstanceCount == 1) {
            sFavicons = new Favicons(FAVICON_CACHE_SIZE);
        }

        setContentView(R.layout.activity_history);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mMessageView = (TextView) findViewById(R.id.message_view);
        mListView = (ListView) findViewById(R.id.listview);

        ((MainApplication)getApplicationContext()).getBus().register(this);
    }

    @Override
    protected void onDestroy() {
        sInstanceCount--;
        if (sInstanceCount == 0) {
            sFavicons = null;
        }

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        mStarted = true;
        mHistoryRecords = MainApplication.sDatabaseHelper.getAllHistoryRecords();
        setupListView();

        MainApplication.checkRestoreCurrentTabs(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mStarted = false;
    }

    private void setupListView() {
        if (mHistoryRecords == null || mHistoryRecords.size() == 0) {
            showNoHistoryView();
            return;
        }

        mMessageView.setVisibility(View.GONE);
        mHistoryAdapter = new HistoryAdapter(this);

        mListView.setAdapter(mHistoryAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        final SwipeDismissListViewTouchListener swipeDismissTouchListener =
                new SwipeDismissListViewTouchListener(
                        mListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            public boolean canDismiss(int position) {
                                if (mHistoryRecords != null && position < mHistoryRecords.size()) {
                                    return true;
                                }
                                return false;
                            }

                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                DatabaseHelper databaseHelper = MainApplication.sDatabaseHelper;

                                for (int position : reverseSortedPositions) {
                                    Object item = listView.getItemAtPosition(position);
                                    if (item instanceof HistoryRecord) {
                                        if (databaseHelper.deleteHistoryRecord((HistoryRecord)item)) {
                                            mHistoryRecords.remove(item);
                                        }
                                    }
                                }

                                if (mHistoryRecords.size() == 0) {
                                    showNoHistoryView();
                                } else {
                                    if (mHistoryAdapter != null) {
                                        mHistoryAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        });
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(swipeDismissTouchListener.makeScrollListener());
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return swipeDismissTouchListener.onTouch(view, motionEvent);
            }
        });

        mListView.setItemsCanFocus(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_clear_history: {
                if (mHistoryAdapter == null) {
                    Toast.makeText(this, R.string.history_already_empty, Toast.LENGTH_SHORT).show();
                    return true;
                }

                final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(R.string.erase_all_history_title);
                alertDialog.setMessage(getString(R.string.erase_all_history_message));
                alertDialog.setCancelable(true);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainApplication.sDatabaseHelper.deleteAllHistoryRecords();
                        mHistoryRecords = null;
                        if (mHistoryAdapter != null) {
                            mHistoryAdapter.notifyDataSetChanged();
                        }
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                Util.showThemedDialog(alertDialog);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof HistoryItem) {
            HistoryItem historyItem = (HistoryItem)view.getTag();
            MainApplication.openLink(this, historyItem.mHistoryRecord.getUrl(), Analytics.OPENED_URL_FROM_HISTORY);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof HistoryItem) {
            final HistoryItem historyItem = (HistoryItem)view.getTag();
            Resources resources = getResources();

            final ArrayList<String> longClickSelections = new ArrayList<String>();

            final String shareLabel = resources.getString(R.string.action_share);
            longClickSelections.add(shareLabel);

            String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();

            final String leftConsumeBubbleLabel = Settings.get().getConsumeBubbleLabel(Constant.BubbleAction.ConsumeLeft);
            if (leftConsumeBubbleLabel != null) {
                if (defaultBrowserLabel == null || defaultBrowserLabel.equals(leftConsumeBubbleLabel) == false) {
                    longClickSelections.add(leftConsumeBubbleLabel);
                }
            }

            final String rightConsumeBubbleLabel = Settings.get().getConsumeBubbleLabel(Constant.BubbleAction.ConsumeRight);
            if (rightConsumeBubbleLabel != null) {
                if (defaultBrowserLabel == null || defaultBrowserLabel.equals(rightConsumeBubbleLabel) == false) {
                    longClickSelections.add(rightConsumeBubbleLabel);
                }
            }

            final String copyLinkLabel = resources.getString(R.string.action_copy_to_clipboard);
            longClickSelections.add(copyLinkLabel);

            Collections.sort(longClickSelections);

            final String openInNewBubbleLabel = resources.getString(R.string.action_open_in_new_bubble);
            longClickSelections.add(0, openInNewBubbleLabel);

            final String openInBrowserLabel = defaultBrowserLabel != null ?
                    String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel) : null;
            if (openInBrowserLabel != null) {
                longClickSelections.add(1, openInBrowserLabel);
            }

            final AlertDialog longPressAlertDialog = new AlertDialog.Builder(this).create();

            ListView listView = new ListView(this);
            listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                    longClickSelections.toArray(new String[0])));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String string = longClickSelections.get(position);
                    String urlAsString = historyItem.mHistoryRecord.getUrl();
                    if (string.equals(openInNewBubbleLabel)) {
                        if (MainController.get() != null) {
                            MainController.get().openUrl(urlAsString, System.currentTimeMillis(), false, Analytics.OPENED_URL_FROM_HISTORY);
                        } else {
                            MainApplication.openLink(getApplicationContext(), urlAsString, Analytics.OPENED_URL_FROM_HISTORY);
                        }
                    } else if (openInBrowserLabel != null && string.equals(openInBrowserLabel)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(urlAsString));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        MainApplication.openInBrowser(HistoryActivity.this, intent, true);
                    } else if (string.equals(shareLabel)) {
                        AlertDialog alertDialog = ActionItem.getShareAlert(HistoryActivity.this, false, new ActionItem.OnActionItemSelectedListener() {
                            @Override
                            public void onSelected(ActionItem actionItem) {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.setClassName(actionItem.mPackageName, actionItem.mActivityClassName);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra(Intent.EXTRA_TEXT, historyItem.mHistoryRecord.getUrl());
                                startActivity(intent);
                            }
                        });
                        Util.showThemedDialog(alertDialog);
                    } else if (leftConsumeBubbleLabel != null && string.equals(leftConsumeBubbleLabel)) {
                        MainApplication.handleBubbleAction(HistoryActivity.this, Constant.BubbleAction.ConsumeLeft, urlAsString, -1);
                    } else if (rightConsumeBubbleLabel != null && string.equals(rightConsumeBubbleLabel)) {
                        MainApplication.handleBubbleAction(HistoryActivity.this, Constant.BubbleAction.ConsumeRight, urlAsString, -1);
                    } else if (string.equals(copyLinkLabel)) {
                        MainApplication.copyLinkToClipboard(HistoryActivity.this, urlAsString, R.string.link_copied_to_clipboard);
                    }

                    if (longPressAlertDialog != null) {
                        longPressAlertDialog.dismiss();
                    }
                }
            });

            longPressAlertDialog.setView(listView);
            Util.showThemedDialog(longPressAlertDialog);

            return true;
        }

        return false;
    }

    void showNoHistoryView() {
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.setText(R.string.empty);
    }

    private class HistoryAdapter extends BaseAdapter {

        LayoutInflater mInflater;

        public HistoryAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mHistoryRecords != null ? mHistoryRecords.size() + 1 : 0;
        }

        @Override
        public Object getItem(int position) {
            return mHistoryRecords != null ? mHistoryRecords.get(position) : position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position == mHistoryRecords.size() ? 1 : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (position == mHistoryRecords.size()) {
                TextView noMoreView;
                if (convertView == null || convertView instanceof TextView == false) {
                    noMoreView = new TextView(HistoryActivity.this);
                    noMoreView.setGravity(Gravity.CENTER);
                    noMoreView.setText("○");
                    noMoreView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Config.dpToPx(40)));
                } else {
                    noMoreView = (TextView)convertView;
                }
                return noMoreView;
            }

            HistoryItem historyItem;
            HistoryRecord historyRecord = mHistoryRecords.get(position);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.history_item, parent, false);
                historyItem = new HistoryItem();
                historyItem.mTitleTextView = (TextView) convertView.findViewById(R.id.page_title);
                historyItem.mUrlTextView = (TextView) convertView.findViewById(R.id.page_url);
                historyItem.mTimeTextView = (TextView) convertView.findViewById(R.id.page_date);
                historyItem.mFaviconImageView = (FaviconView) convertView.findViewById(R.id.favicon);
                historyItem.mFaviconImageView.mFavicons = sFavicons;
            } else {
                historyItem = (HistoryItem) convertView.getTag();
            }

            historyItem.mHistoryRecord = historyRecord;
            historyItem.mDate.setTime(historyRecord.getTime());
            historyItem.mFaviconSet = false;

            historyItem.mTitleTextView.setText(historyRecord.getTitle());
            historyItem.mUrlTextView.setText(historyRecord.getHost());
            historyItem.mTimeTextView.setText(Util.getPrettyDate(historyItem.mDate));

            int flags = Settings.get().isIncognitoMode() ? 0 : LoadFaviconTask.FLAG_PERSIST;
            String host = historyRecord.getHost();
            String faviconUrl = "http://" + host + "/favicon.ico";

            historyItem.mFaviconUrl = faviconUrl;
            historyItem.mFaviconImageView.clearImage();
            sFavicons.getFaviconForSize(host, faviconUrl, Integer.MAX_VALUE, flags, historyItem.mOnFaviconLoadedListener);
            if (historyItem.mFaviconSet == false) {
                historyItem.mFaviconImageView.showDefaultFavicon();
            }

            convertView.setTag(historyItem);

            return convertView;
        }
    }

    private static class HistoryItem {
        TextView mTitleTextView;
        TextView mUrlTextView;
        TextView mTimeTextView;
        FaviconView mFaviconImageView;
        HistoryRecord mHistoryRecord;
        Date mDate = new Date();
        String mFaviconUrl;
        boolean mFaviconSet;
        OnFaviconLoadedListener mOnFaviconLoadedListener = new OnFaviconLoadedListener() {
            @Override
            public void onFaviconLoaded(String url, String faviconURL, Bitmap favicon) {
                // Ensure the favicon passed in matches the one we want. This can be false as HistoryAdapter recycles
                // Views and favicons are loaded in different orders to that which they are requested.
                if (mFaviconUrl.equals(faviconURL) == false) {
                    return;
                }
                if (favicon != null) {
                    mFaviconSet = true;
                    mFaviconImageView.updateImage(favicon, faviconURL, true);
                }
            }
        };
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHistoryRecordChangedEvent(HistoryRecord.ChangedEvent event) {
        if (!mStarted) {
            return;
        }
        boolean setupList = false;
        if (mHistoryRecords == null) {
            mHistoryRecords = new ArrayList<HistoryRecord>();
            setupList = true;
        }

        HistoryRecord historyRecord = event.mHistoryRecord;
        // find out if the item exists on the list already. This will be true if a HistoryRecord for a URL was updated
        boolean onList = false;
        for (HistoryRecord existing : mHistoryRecords) {
            if (existing.getId() == historyRecord.getId()) {
                mHistoryRecords.remove(existing);
                break;
            }
        }

        // Add it at the top of the list. This assumes the item had it's date updated to 'now',
        // which is the current behaviour.
        mHistoryRecords.add(0, historyRecord);

        if (setupList) {
            setupListView();
        } else {
            if (mMessageView != null) {
                mMessageView.setVisibility(View.GONE);
            }
            if (mHistoryAdapter != null) {
                mHistoryAdapter.notifyDataSetChanged();
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void OnDestroyMainServiceEvent(MainService.OnDestroyMainServiceEvent event) {
        finish();
    }
}
