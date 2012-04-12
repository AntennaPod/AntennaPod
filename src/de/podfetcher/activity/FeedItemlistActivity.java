package de.podfetcher.activity;

import com.actionbarsherlock.app.SherlockListActivity;
import android.view.View;
import android.widget.ListView;
import android.os.Bundle;
import de.podfetcher.feed.*;
import de.podfetcher.adapter.FeedItemlistAdapter;
import android.util.Log;

/** Displays a List of FeedItems */
public class FeedItemlistActivity extends SherlockListActivity {
	private static final String TAG = "FeedItemlistActivity";

	private FeedItemlistAdapter fila;
	private FeedManager manager;

	/** The feed which the activity displays */
	private Feed feed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		long feedId = getIntent().getLongExtra(FeedlistActivity.EXTRA_SELECTED_FEED, -1);
		if(feedId == -1) Log.e(TAG, "Received invalid feed selection.");

		feed = manager.getFeed(feedId);
		
		fila = new FeedItemlistAdapter(this, 0, feed.getItems());
		setListAdapter(fila);

		setTitle(feed.getTitle());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
	
	}
}
