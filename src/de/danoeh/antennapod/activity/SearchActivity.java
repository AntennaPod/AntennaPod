package de.danoeh.antennapod.activity;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.storage.FeedSearcher;
import de.danoeh.antennapod.feed.SearchResult;
import de.danoeh.antennapod.fragment.FeedlistFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;
import de.danoeh.antennapod.preferences.UserPreferences;

/**
 * Displays the results when the user searches for FeedItems or Feeds.
 */
public class SearchActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "SearchActivity";

    public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.searchactivity.extra.feedId";

    private SearchlistAdapter searchAdapter;

    /**
     * ID of the feed that is being searched or null if the search is global.
     */
    private long feedID;

    private ListView listView;
    private TextView txtvStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.searchlist);
        listView = (ListView) findViewById(android.R.id.list);
        txtvStatus = (TextView) findViewById(android.R.id.empty);

        listView.setOnItemClickListener(this);
        searchAdapter = new SearchlistAdapter(this, 0, new ArrayList<SearchResult>());
        listView.setAdapter(searchAdapter);
        listView.setEmptyView(txtvStatus);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if (intent.hasExtra(SearchActivity.EXTRA_FEED_ID)) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Found bundle extra");
                feedID = intent.getLongExtra(SearchActivity.EXTRA_FEED_ID, 0);
            }
            if (AppConfig.DEBUG)
                Log.d(TAG, "Starting search");
            String query = intent.getStringExtra(SearchManager.QUERY);
            getSupportActionBar()
                    .setSubtitle(
                            getString(R.string.search_term_label) + "\""
                                    + query + "\"");
            handleSearchRequest(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label)
                .setIcon(
                        obtainStyledAttributes(
                                new int[]{R.attr.action_search})
                                .getDrawable(0)),
                (MenuItem.SHOW_AS_ACTION_IF_ROOM));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.search_item:
                onSearchRequested();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onSearchRequested() {
        Bundle extra = null;
        if (feedID != 0) {
            extra = new Bundle();
            extra.putLong(EXTRA_FEED_ID, feedID);
        }
        startSearch(null, false, extra, false);
        return true;
    }

    @SuppressLint({"NewApi", "NewApi"})
    private void handleSearchRequest(final String query) {
        if (searchAdapter != null) {
            searchAdapter.clear();
            searchAdapter.notifyDataSetChanged();
        }
        txtvStatus.setText(R.string.search_status_searching);

        Thread thread = new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "Starting background work");
                final List<SearchResult> result = FeedSearcher
                        .performSearch(SearchActivity.this, query, feedID);
                if (SearchActivity.this != null) {
                    SearchActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "Background work finished");
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "Found " + result.size()
                                        + " results");

                            searchAdapter.clear();
                            searchAdapter.addAll(result);
                            searchAdapter.notifyDataSetChanged();
                            txtvStatus
                                    .setText(R.string.search_status_no_results);
                            if (!searchAdapter.isEmpty()) {
                                txtvStatus.setVisibility(View.GONE);
                            } else {
                                txtvStatus.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        };
        thread.start();

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        SearchResult selection = searchAdapter.getItem(position);
        if (selection.getComponent().getClass() == Feed.class) {
            Feed feed = (Feed) selection.getComponent();
            Intent launchIntent = new Intent(this, FeedItemlistActivity.class);
            launchIntent.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED,
                    feed.getId());
            startActivity(launchIntent);

        } else if (selection.getComponent().getClass() == FeedItem.class) {
            FeedItem item = (FeedItem) selection.getComponent();
            Intent launchIntent = new Intent(this, ItemviewActivity.class);
            launchIntent.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED, item
                    .getFeed().getId());
            launchIntent.putExtra(ItemlistFragment.EXTRA_SELECTED_FEEDITEM,
                    item.getId());
            startActivity(launchIntent);
        }
    }
}
