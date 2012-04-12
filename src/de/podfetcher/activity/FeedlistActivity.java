package de.podfetcher.activity;

import de.podfetcher.R;
import de.podfetcher.feed.*;
import de.podfetcher.adapter.FeedlistAdapter;
import de.podfetcher.service.FeedSyncService;
import de.podfetcher.storage.DownloadRequester;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


public class FeedlistActivity extends SherlockListActivity {
	private static final String TAG = "FeedlistActivity";
	public static final String EXTRA_SELECTED_FEED = "extra.de.podfetcher.activity.selected_feed";
	
	private FeedManager manager;
	private FeedlistAdapter fla;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		manager = FeedManager.getInstance();
		fla = new FeedlistAdapter(this, 0, manager.getFeeds());
		setListAdapter(fla);

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.feedlist, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch(item.getItemId()) {
	        case R.id.add_feed:
	            startActivity(new Intent(this, AddFeedActivity.class));
				return true;
			default:
			    return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(FeedSyncService.ACTION_FEED_SYNC_COMPLETED);

		registerReceiver(contentUpdate, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(contentUpdate);
	}

	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fla.notifyDataSetChanged();
		}
	};

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Feed selection = fla.getItem(position);
		Intent showFeed = new Intent(this, FeedItemlistActivity.class);
		showFeed.putExtra(EXTRA_SELECTED_FEED, selection.getId());

		startActivity(showFeed);
	}
}
