/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.linkbubble.R;

public class ThemedListPreference extends ListPreference implements ListView.OnItemClickListener {

    public static final String TAG = "ThemedListPreference";

    private int mClickedDialogEntryIndex;

    private CharSequence mDialogTitle;

    public ThemedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemedListPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        // inflate custom layout with custom title & listview
        View view = View.inflate(getContext(), R.layout.view_preference_list_view, null);

        mDialogTitle = getDialogTitle();
        if(mDialogTitle == null) mDialogTitle = getTitle();
        ((TextView) view.findViewById(R.id.dialog_title)).setText(mDialogTitle);

        ListView list = (ListView) view.findViewById(android.R.id.list);
        // note the layout we're providing for the ListView entries
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                getContext(), R.layout.view_preference_list_view_item,
                getEntries());

        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setItemChecked(findIndexOfValue(getValue()), true);
        list.setOnItemClickListener(this);

        return view;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // adapted from ListPreference
        if (getEntries() == null || getEntryValues() == null) {
            // throws exception
            super.onPrepareDialogBuilder(builder);
            return;
        }

        mClickedDialogEntryIndex = findIndexOfValue(getValue());

        // .setTitle(null) to prevent default (blue)
        // title+divider from showing up
        builder.setTitle(null);

        builder.setPositiveButton(null, null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        mClickedDialogEntryIndex = position;
        ThemedListPreference.this.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
        getDialog().dismiss();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // adapted from ListPreference
        super.onDialogClosed(positiveResult);

        if (positiveResult && mClickedDialogEntryIndex >= 0
                && getEntryValues() != null) {
            String value = getEntryValues()[mClickedDialogEntryIndex]
                    .toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }
}