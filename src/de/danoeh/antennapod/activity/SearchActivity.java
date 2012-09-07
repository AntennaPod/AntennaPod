package de.danoeh.antennapod.activity;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedSearcher;
import de.danoeh.antennapod.feed.SearchResult;
import de.danoeh.antennapod.fragment.FeedlistFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;

/** Displays the results when the user searches for FeedItems or Feeds. */
public class SearchActivity extends SherlockListActivity {
	private static final String TAG = "SearchActivity";

	public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.searchactivity.extra.feedId";

	private SearchlistAdapter searchAdapter;
	private ArrayList<SearchResult> content;

	/** Feed that is being searched or null if the search is global. */
	private Feed selectedFeed;

	private TextView txtvStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.searchlist);
		txtvStatus = (TextView) findViewById(android.R.id.empty);
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
			Bundle extra = intent.getBundleExtra(SearchManager.APP_DATA);
			if (extra != null) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Found bundle extra");
				long feedId = extra.getLong(EXTRA_FEED_ID);
				selectedFeed = FeedManager.getInstance().getFeed(feedId);
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, "Starting search");
			String query = intent.getStringExtra(SearchManager.QUERY);
			getSupportActionBar().setSubtitle(
					getString(R.string.search_term_label) + "\"" + query + "\"");
			handleSearchRequest(query);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label)
				.setIcon(R.drawable.action_search)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
		if (selectedFeed != null) {
			extra = new Bundle();
			extra.putLong(EXTRA_FEED_ID, selectedFeed.getId());
		}
		startSearch(null, false, extra, false);
		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
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

	@SuppressLint({ "NewApi", "NewApi" })
	private void handleSearchRequest(String query) {
		AsyncTask<String, Void, ArrayList<SearchResult>> executor = new AsyncTask<String, Void, ArrayList<SearchResult>>() {

			@Override
			protected void onPreExecute() {
				if (searchAdapter != null) {
					searchAdapter.clear();
					searchAdapter.notifyDataSetChanged();
				}
				txtvStatus.setText(R.string.search_status_searching);
			}

			@Override
			protected ArrayList<SearchResult> doInBackground(String... params) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Starting background work");
				return FeedSearcher.performSearch(params[0], selectedFeed);
			}

			@Override
			protected void onPostExecute(ArrayList<SearchResult> result) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Background work finished");
				if (AppConfig.DEBUG)
					Log.d(TAG, "Found " + result.size() + " results");
				content = result;

				searchAdapter = new SearchlistAdapter(SearchActivity.this, 0,
						content);
				getListView().setAdapter(searchAdapter);
				searchAdapter.notifyDataSetChanged();
				if (content.isEmpty()) {
					txtvStatus.setText(R.string.search_status_no_results);
				}

			}

		};
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
		} else {
			executor.execute(query);
		}
	}
}
