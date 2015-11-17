package com.linkbubble.ui;

public class SearchURLSuggestions {

    public enum SearchEngine {
        DUCKDUCKGO,
        GOOGLE,
        YAHOO,
        AMAZON,
        NONE
    }

    public String Name;
    public String Value;
    public SearchEngine EngineToUse;
}
