/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

public class SearchURLSuggestions {

    public enum SearchEngine {
        DUCKDUCKGO,
        GOOGLE,
        YAHOO,
        AMAZON,
        NONE
    }

    public SearchURLSuggestions() {
        EngineToUse = SearchEngine.NONE;
    }

    public String Name;
    public SearchEngine EngineToUse;
}
