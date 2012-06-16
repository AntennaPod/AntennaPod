package de.podfetcher.activity;

import com.actionbarsherlock.app.SherlockListActivity;
import android.view.View;
import android.widget.ListView;
import android.os.Bundle;
import android.content.Intent;
import de.podfetcher.feed.*;
import de.podfetcher.adapter.FeedItemlistAdapter;
import de.podfetcher.fragment.FeedlistFragment;
import android.util.Log;

/** Displays a List of FeedItems */
public class FeedItemlistActivity extends SherlockListActivity {
	private static final String TAG = "FeedItemlistActivity";
	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.podfetcher.activity.selected_feeditem";

	private FeedItemlistAdapter fila;
	private FeedManager manager;

	/** The feed which the activity displays */
	private Feed feed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		long feedId = getIntent().getLongExtra(FeedlistFragment.EXTRA_SELECTED_FEED, -1);
		if(feedId == -1) Log.e(TAG, "Received invalid feed selection.");

		feed = manager.getFeed(feedId);
		
		fila = new FeedItemlistAdapter(this, 0, feed.getItems());
		setListAdapter(fila);

		setTitle(feed.getTitle());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		FeedItem selection = fila.getItem(position);
		Intent showItem = new Intent(this, ItemviewActivity.class);
		showItem.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED, feed.getId());
		showItem.putExtra(EXTRA_SELECTED_FEEDITEM, selection.getId());

		startActivity(showItem);
	}
	
	public void onButActionClicked(View v) {
		Log.d(TAG, "Button clicked");
	}
}
