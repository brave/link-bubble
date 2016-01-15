/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.content.res.Resources;

import com.linkbubble.MainApplication;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.util.Util;
import com.linkbubble.R;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchURLSuggestionsContainer {

    public static final int HISTORY_ROWS_TO_GET = 50;

    public static CopyOnWriteArrayList<SearchURLSuggestions> mSuggestions;

    private int mTotalHistoryRecords = 0;

    public void loadSuggestions(Context context, Resources resources) {
        if (null == mSuggestions) {
            mSuggestions = new CopyOnWriteArrayList<SearchURLSuggestions>();
        }
        if (0 != mSuggestions.size()) {
            return;
        }

        // Fill suggestion list with history URL's
        List<HistoryRecord> historyRecords = MainApplication.sDatabaseHelper.getRecentNHistoryRecords(HISTORY_ROWS_TO_GET);
        mTotalHistoryRecords = historyRecords.size();
        for (HistoryRecord historyRecord : historyRecords) {
            String historyUrl = Util.getUrlWithoutHttpHttpsWww(context, historyRecord.getUrl());
            // Looking on duplications
            if (suggestedAlreadyAdded(historyUrl, mSuggestions)) {
                continue;
            }
            SearchURLSuggestions suggestion = new SearchURLSuggestions();
            suggestion.Name = historyUrl;
            suggestion.EngineToUse = SearchURLSuggestions.SearchEngine.NONE;
            mSuggestions.add(suggestion);
        }
        // Set an adapter for search URL control for top 500 websites
        String[] top500websites = resources.getStringArray(R.array.top500websites);
        for (int i = 0; i < top500websites.length; i++) {
            // Looking on duplications
            if (suggestedAlreadyAdded(top500websites[i], mSuggestions)) {
                continue;
            }
            SearchURLSuggestions suggestion = new SearchURLSuggestions();
            suggestion.Name = top500websites[i];
            suggestion.EngineToUse = SearchURLSuggestions.SearchEngine.NONE;
            mSuggestions.add(suggestion);
        }
    }

    public void addUrlToAutoSuggestion(String urlToAdd, Context context, Resources resources) {
        String newUrlToAdd = Util.getUrlWithoutHttpHttpsWww(context, urlToAdd);
        MainApplication.sSearchURLSuggestionsContainer.loadSuggestions(context, resources);
        for (SearchURLSuggestions suggestion : MainApplication.sSearchURLSuggestionsContainer.mSuggestions) {
            if (suggestion.Name.equals(newUrlToAdd)) {
                return;
            }
        }
        if (mTotalHistoryRecords >= HISTORY_ROWS_TO_GET
                && MainApplication.sSearchURLSuggestionsContainer.mSuggestions.size() > HISTORY_ROWS_TO_GET) {
            MainApplication.sSearchURLSuggestionsContainer.mSuggestions.remove(mTotalHistoryRecords - 1);
        }
        else {
            mTotalHistoryRecords++;
        }
        SearchURLSuggestions suggestion = new SearchURLSuggestions();
        suggestion.Name = newUrlToAdd;
        suggestion.EngineToUse = SearchURLSuggestions.SearchEngine.NONE;
        MainApplication.sSearchURLSuggestionsContainer.mSuggestions.add(0, suggestion);
    }

    // Checks if we have added that suggestion already
    private boolean suggestedAlreadyAdded(String urlSuggestion, CopyOnWriteArrayList<SearchURLSuggestions> suggestionsList) {
        for (SearchURLSuggestions suggestion: suggestionsList) {
            if (urlSuggestion.equals(suggestion.Name)) {
                return true;
            }
        }

        return false;
    }
}
