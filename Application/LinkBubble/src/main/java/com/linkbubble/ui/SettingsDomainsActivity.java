/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.linkbubble.R;
import com.linkbubble.Settings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SettingsDomainsActivity extends AppCompatActivity {

    Adapter adapter;
    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.fab) FloatingActionButton addButton;
    @Bind(R.id.root_view) View rootView;
    LinearLayoutManager linearLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings_domains);

        ButterKnife.bind(this);

        adapter = new Adapter(this);

        recyclerView.setAdapter(adapter);

        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.preference_domains_title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRedirectDialog();
            }
        });
    }

    void showAddRedirectDialog() {
        View layout = LayoutInflater.from(this).inflate(R.layout.view_add_domain, null);
        final EditText editText = (EditText)layout.findViewById(R.id.edit_text);
        new AlertDialog.Builder(this)
                .setTitle(R.string.preference_add_domain_title)
                .setView(layout)
                .setPositiveButton(R.string.action_add,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String host = editText.getText().toString();
                        boolean added = false;
                        if (!TextUtils.isEmpty(host)) {
                            host = host.replace("\"", "");
                            int protocolIndex = host.indexOf("://");
                            if (protocolIndex > -1) {
                                host = host.substring(protocolIndex + "://".length());
                            }
                            if (host.contains(".") && !host.contains(" ")) {
                                int slashIndex = host.indexOf("/");
                                if (slashIndex > -1) {
                                    host = host.substring(0, slashIndex);
                                }
                                try {
                                    URL url = new URL("http", host, "/");
                                    adapter.addDomain(url.getHost());
                                    Settings.get().addFallbackRedirectHost(url.getHost());
                                    added = true;
                                }
                                catch(MalformedURLException e){
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (!added) {
                            showSnackbar(String.format(getString(R.string.add_domain_error),
                                    editText.getText().toString()));
                        }
                    }
                })
                .create()
                .show();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 300);
    }
    private Handler mHandler = new Handler();

    private Snackbar currentSnackbar;
    void showSnackbar(String message) {
        showSnackbar(message, null, null);
    }

    void showSnackbar(String message, String action, View.OnClickListener onActionClickListener) {
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
            currentSnackbar = null;
        }

        currentSnackbar =
                Snackbar.make(rootView,
                        message,
                        action != null ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT)
                        .setAction(action, onActionClickListener);
        currentSnackbar.show();
    }

    static abstract class BaseItem {
        Context context;
        String title;
        int width;
        int height;

        BaseItem(Context context, String title) {
            this.context = context;
            this.title = title;

            width = ViewGroup.LayoutParams.MATCH_PARENT;
            height = context.getResources().getDimensionPixelSize(R.dimen.settings_item_height);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View itemView) {
                super(itemView);
            }

            void setTag(Object tag) {
                itemView.setTag(tag);
            }

            void bind(BaseItem baseItem) {
                setTag(baseItem);

                ViewGroup.LayoutParams lp = itemView.getLayoutParams();
                if (lp == null) {
                    lp = new ViewGroup.LayoutParams(baseItem.width, baseItem.height);
                } else {
                    lp.width = baseItem.width;
                    lp.height = baseItem.height;
                }
                if (baseItem.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    // wrap content based on text container rather than root layout to ensure
                    // selectableItemBackground extends correctly to visible bounds.
                    View textContainer = itemView.findViewById(R.id.settings_text_container);
                    if (textContainer != null) {
                        int vPadding = baseItem.context.getResources().getDimensionPixelSize(R.dimen.default_margin_half);
                        textContainer.setPadding(itemView.getPaddingLeft(), vPadding, itemView.getPaddingRight(), vPadding);
                    }
                }
                itemView.setLayoutParams(lp);
            }
        }
    }

    static class HeadingItem extends BaseItem {
        HeadingItem(Context context, String title) {
            super(context, title);
            height = context.getResources().getDimensionPixelSize(R.dimen.settings_group_title_item_height);
        }

        static class ViewHolder extends BaseItem.ViewHolder {

            TextView titleView;

            public ViewHolder(View itemView) {
                super(itemView);

                titleView = (TextView) itemView.findViewById(R.id.settings_title);
            }

            void bind(HeadingItem headingItem) {
                super.bind(headingItem);
                titleView.setText(headingItem.title);
            }
        }
    }

    static class DomainItem extends BaseItem {
        DomainItem(Context context, String title) {
            super(context, title);
            height = context.getResources().getDimensionPixelSize(R.dimen.settings_domain_item_height);
        }

        static class ViewHolder extends BaseItem.ViewHolder {

            @Bind(R.id.settings_title) TextView titleView;
            @Bind(R.id.settings_divider) View divider;
            @Bind(R.id.remove_icon) ImageView removeIcon;

            public ViewHolder(View itemView) {
                super(itemView);

                ButterKnife.bind(this, this.itemView);

                itemView.findViewById(R.id.settings_summary).setVisibility(View.GONE);
                itemView.findViewById(R.id.app_icon).setVisibility(View.GONE);
            }

            void bind(DomainItem domainItem, boolean showDivider, View.OnClickListener onRemoveClickListener) {
                super.bind(domainItem);
                titleView.setText(domainItem.title);
                divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
                removeIcon.setOnClickListener(onRemoveClickListener);
            }
        }
    }

    static final int VIEW_TYPE_HEADING = 0;
    static final int VIEW_TYPE_DOMAIN = 1;

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        Context context;
        ArrayList<BaseItem> items = new ArrayList<>();

        Adapter(Context context) {
            this.context = context;
            items.add(new HeadingItem(context, getString(R.string.preference_redirects_title)));

            Set<String> redirectHosts = Settings.get().getFallbackRedirectHosts();
            if (redirectHosts.size() > 0) {
                ArrayList<String> strings = new ArrayList<>(redirectHosts);
                Collections.sort(strings);
                for (String string : strings) {
                    items.add(new DomainItem(context, string));
                }
            }
        }

        void addDomain(String host) {
            DomainItem item = new DomainItem(context, host);
            addItem(item);
        }

        void addItem(BaseItem item) {
            items.add(item);
            notifyItemInserted(items.indexOf(item));
        }

        void removeItem(BaseItem baseItem) {
            int index = items.indexOf(baseItem);
            items.remove(index);
            notifyItemRemoved(index);
        }

        @Override
        public int getItemViewType(int position) {
            BaseItem baseItem = items.get(position);
            if (baseItem instanceof HeadingItem) {
                return VIEW_TYPE_HEADING;
            }
            return VIEW_TYPE_DOMAIN;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_HEADING:
                    return new HeadingItem.ViewHolder(LayoutInflater.from(context)
                            .inflate(R.layout.view_settings_group_title, null));

                case VIEW_TYPE_DOMAIN:
                    return new DomainItem.ViewHolder(LayoutInflater.from(context)
                            .inflate(R.layout.view_settings_item, null));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DomainItem.ViewHolder) {
                final DomainItem item = (DomainItem) items.get(position);
                ((DomainItem.ViewHolder) holder).bind(item,
                        !(items.get(position - 1) instanceof HeadingItem),
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                removeItem(item);
                                Settings.get().removeFallbackRedirectHost(item.title);
                                showSnackbar("Removed " + item.title + ".",
                                        getString(R.string.action_undo),
                                        new View.OnClickListener() {

                                            @Override
                                            public void onClick(View v) {
                                                addItem(item);
                                                Settings.get().addFallbackRedirectHost(item.title);
                                            }
                                        });
                            }
                        });

            } else if (holder instanceof HeadingItem.ViewHolder) {
                ((HeadingItem.ViewHolder) holder).bind((HeadingItem) items.get(position));
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
