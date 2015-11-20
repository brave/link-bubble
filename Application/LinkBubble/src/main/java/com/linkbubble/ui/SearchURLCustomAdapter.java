package com.linkbubble.ui;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.linkbubble.R;

import java.util.ArrayList;
import java.util.List;


public class SearchURLCustomAdapter extends ArrayAdapter<SearchURLSuggestions> {

    public String mRealUrlBarConstraint = "";

    private LayoutInflater layoutInflater;
    List<SearchURLSuggestions> mSuggestions;
    private int viewResourceId;

    private Filter mFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            return ((SearchURLSuggestions)resultValue).Name;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraintInCome) {
            FilterResults results = new FilterResults();

            String constraint = mRealUrlBarConstraint;
            if (constraint != null && constraint.length() != 0) {
                ArrayList<SearchURLSuggestions> suggestions = new ArrayList<SearchURLSuggestions>();
                for (SearchURLSuggestions suggestion : mSuggestions) {
                    // Note: change the "startsWith" to "contains" if you only want starting matches
                    if (suggestion.Name.toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                        suggestions.add(suggestion);
                    }
                }


                // For search engines
                SearchURLSuggestions searchSuggestion1 = new SearchURLSuggestions();
                SearchURLSuggestions searchSuggestion2 = new SearchURLSuggestions();
                SearchURLSuggestions searchSuggestion3 = new SearchURLSuggestions();
                SearchURLSuggestions searchSuggestion4 = new SearchURLSuggestions();
                searchSuggestion1.Value = String.format(getContext().getString(R.string.search_for_with), "<font color=" +
                        getContext().getString(R.string.url_bar_constraint_text_color) + ">" + constraint +
                        "</font>", getContext().getString(R.string.duck_duck_go));
                searchSuggestion1.Name = constraint.toString();
                searchSuggestion1.EngineToUse = SearchURLSuggestions.SearchEngine.DUCKDUCKGO;
                searchSuggestion2.Value = String.format(getContext().getString(R.string.search_for_with), "<font color=" +
                        getContext().getString(R.string.url_bar_constraint_text_color) + ">" + constraint +
                        "</font>", getContext().getString(R.string.google));
                searchSuggestion2.Name = constraint.toString();
                searchSuggestion2.EngineToUse = SearchURLSuggestions.SearchEngine.GOOGLE;
                searchSuggestion3.Value = String.format(getContext().getString(R.string.search_for_with), "<font color=" +
                        getContext().getString(R.string.url_bar_constraint_text_color) + ">" + constraint +
                        "</font>", getContext().getString(R.string.yahoo));
                searchSuggestion3.Name = constraint.toString();
                searchSuggestion3.EngineToUse = SearchURLSuggestions.SearchEngine.YAHOO;
                searchSuggestion4.Value = String.format(getContext().getString(R.string.search_for_on), "<font color=" +
                        getContext().getString(R.string.url_bar_constraint_text_color) + ">" + constraint +
                        "</font>", getContext().getString(R.string.amazon));
                searchSuggestion4.Name = constraint.toString();
                searchSuggestion4.EngineToUse = SearchURLSuggestions.SearchEngine.AMAZON;

                suggestions.add(searchSuggestion1);
                suggestions.add(searchSuggestion2);
                suggestions.add(searchSuggestion3);
                suggestions.add(searchSuggestion4);
                //
                results.values = suggestions;
                results.count = suggestions.size();
            }
            else {
                results.values = mSuggestions;
                results.count = mSuggestions.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            clear();
            if (results != null && results.count > 0) {
                // We have filtered results
                addAll((ArrayList<SearchURLSuggestions>) results.values);
            }
            notifyDataSetChanged();
        }
    };

    public SearchURLCustomAdapter(Context context, int textViewResourceId, List<SearchURLSuggestions> suggestions) {
        super(context, textViewResourceId, suggestions);
        // Copy all the customers into a master list
        mSuggestions = new ArrayList<SearchURLSuggestions>(suggestions.size());
        mSuggestions.addAll(suggestions);
        layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewResourceId = textViewResourceId;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView name = (TextView) view;
        SearchURLSuggestions suggestion = getItem(position);
        name.setText(Html.fromHtml(suggestion.Value));

        return view;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }
}
