package de.danoeh.antennapod.activity;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockListActivity;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.feed.FeedComponent;
import de.danoeh.antennapod.feed.FeedSearcher;
import de.danoeh.antennapod.feed.SearchResult;

public class SearchActivity extends SherlockListActivity {
	private static final String TAG = "SearchActivity";

	private SearchlistAdapter searchAdapter;
	private ArrayList<SearchResult> content;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Starting search");
			String query = getIntent().getStringExtra(SearchManager.QUERY);
			startSearch(query);
		}
	}

	@SuppressLint({ "NewApi", "NewApi" })
	private void startSearch(String query) {
		AsyncTask<String, Void, ArrayList<SearchResult>> executor = new AsyncTask<String, Void, ArrayList<SearchResult>>() {

			@Override
			protected ArrayList<SearchResult> doInBackground(String... params) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Starting background work");
				return FeedSearcher.performSearch(params[0]);
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

			}

		};
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			executor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
		} else {
			executor.execute(query);
		}
	}
}
