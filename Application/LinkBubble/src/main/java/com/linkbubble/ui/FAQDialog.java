/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.R;
import com.linkbubble.util.Util;

import java.util.ArrayList;
import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;


public class FAQDialog {

    private Activity mActivity;
    private boolean[] mExpanded;

    static ArrayList<Integer> sQuestionStringIds;
    static ArrayList<Integer> sAnswerStringIds;
    static int sFAQSize = 0;

    static String[] sFAQEntry = {
        "faq_app_internal_browser",
        "faq_close_tab",
        "faq_article_mode",
        "faq_next_update_eta",
        "faq_translations",

        "faq_back_button_minimize",
        "faq_cant_type_url",
        "faq_future_features",
        "faq_roadmap",

        "faq_close_quickly",
        "faq_change_default_browser",
        "faq_crap_webview",
        "faq_low_spec_performance",
        "faq_copy_text",

        "faq_report_bug",
    };

    static int sBetaIndex = 3;
    static int sTransliationsIndex = 4;
    static int sFunctionalityIndex = 5;
    static int sIssuesIndex = 14;

	public FAQDialog(Activity context) {
        if (sQuestionStringIds == null) {
            sQuestionStringIds = new ArrayList<Integer>();
            sAnswerStringIds = new ArrayList<Integer>();

            Resources resources = context.getResources();
            String packageName = context.getPackageName();

            for (String entry : sFAQEntry) {
                int questionId = resources.getIdentifier("string/" + entry + "_question", "id", packageName);
                int answerId = resources.getIdentifier("string/" + entry + "_answer", "id", packageName);

                if (answerId == 0 && questionId == 0) {
                    break;
                }
                if (answerId > 0 && questionId > 0) {
                    sQuestionStringIds.add(questionId);
                    sAnswerStringIds.add(answerId);
                    sFAQSize = sAnswerStringIds.size();
                }
            }
        }

        mActivity = context;
        mExpanded = new boolean[sFAQSize];
    }

	//Call to show the FAQ
    public void show() {

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.view_faq, null);

        StickyListHeadersListView listView = (StickyListHeadersListView)layout.findViewById(R.id.faq_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == sBetaIndex) {
                    // We do not use it with Brave, maybe have to remove.
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http://linkbubble.com/device/link_bubble_beta.html"));
                    mActivity.startActivity(i);
                } else if (position == sTransliationsIndex) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http://www.getlocalization.com/LinkBubble/"));
                    mActivity.startActivity(i);
                } else if (position == sFAQSize-1) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                                                    Uri.fromParts("mailto", "support+linkbubble@brave.com", null));
                    String appVersion = BuildConfig.VERSION_NAME;
                    String subject = "[Link Bubble] Report a bug (v" + appVersion + ", Android " + Constant.getOSFlavor()
                            + ", " + android.os.Build.MODEL + ", " + Locale.getDefault().getLanguage() + ")";
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "My bug is ...\n\nHow often does the problem occur?\n\nAre you running a ROM and/or a modified framework/kernel? ");
                    mActivity.startActivity(Intent.createChooser(emailIntent, "Send bug report email..."));
                } else {
                    FAQAdapter adapter = (FAQAdapter)view.getTag();
                    adapter.toggle(position);
                }
            }
        });
        listView.setAdapter(new FAQAdapter(mActivity));

        AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        alertDialog.setIcon(Util.getAlertIcon(mActivity));
        alertDialog.setTitle(R.string.faq_title);
        alertDialog.setView(layout);

        /*
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(KEY_HORIZONTAL_PADDING_PREFERENCE, horizontalSeekBar.getProgress());
                editor.putInt(KEY_VERTICAL_PADDING_PREFERENCE, verticalSeekBar.getProgress());
                editor.commit();
            }

        });

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.restore_default_action), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(KEY_HORIZONTAL_PADDING_PREFERENCE, LauncherPreferences.get().getWorkspaceHorizontalPaddingDefaultAsInt());
                editor.putInt(KEY_VERTICAL_PADDING_PREFERENCE, LauncherPreferences.get().getWorkspaceVerticalPaddingDefaultAsInt());
                editor.commit();
            }
        });
        */

        Util.showThemedDialog(alertDialog);
    }

    private class FAQAdapter extends BaseAdapter implements StickyListHeadersAdapter {

        LayoutInflater mInflater;

        public FAQAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return sFAQSize;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            FAQItem faqItem;
            if (convertView == null) {
                faqItem = (FAQItem)mInflater.inflate(R.layout.view_faq_item, null);
            } else {
                faqItem = (FAQItem)convertView;
            }

            faqItem.configure(this, sQuestionStringIds.get(position), sAnswerStringIds.get(position), mExpanded[position]);
            return faqItem;
        }

        public void toggle(int position) {
            mExpanded[position] = !mExpanded[position];
            notifyDataSetChanged();
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.view_section_header, parent, false);
            TextView headerLabel = (TextView)convertView.findViewById(R.id.section_text);

            int stringId = R.string.faq_section_issues;
            if (position < sFunctionalityIndex) {
                stringId = R.string.faq_section_general;
            } else if (position < sIssuesIndex) {
                stringId = R.string.faq_section_functionality;
            }

            headerLabel.setText(stringId);
            return convertView;
        }

        @Override
        public long getHeaderId(int position) {
            if (position < sFunctionalityIndex) {
                return 0;
            } else if (position < sIssuesIndex) {
                return 1;
            }

            return 2;
        }
    }
}

