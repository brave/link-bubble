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
    public String Value;
    public SearchEngine EngineToUse;
}
