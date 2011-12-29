package de.podfetcher.activity;

import de.podfetcher.R;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.gui.FeedlistAdapter;
import de.podfetcher.service.FeedSyncService;
import de.podfetcher.storage.DownloadRequester;
import greendroid.app.GDListActivity;
import android.os.Bundle;
import android.view.View;
import greendroid.widget.ActionBarItem.Type;
import greendroid.widget.ActionBarItem;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;


public class FeedlistActivity extends GDListActivity {
	
	private FeedManager manager;
	private FeedlistAdapter fla;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		manager = FeedManager.getInstance();
		fla = new FeedlistAdapter(this, R.layout.feedlist_item, 0, manager.getFeeds());
		setListAdapter(fla);

		addActionBarItem(Type.Add, R.id.action_bar_add);
		addActionBarItem(Type.Refresh, R.id.action_bar_refresh);

	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(FeedSyncService.ACTION_FEED_SYNC_COMPLETED);
		filter.addAction(DownloadRequester.ACTION_IMAGE_DOWNLOAD_COMPLETED);

		registerReceiver(contentUpdate, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(contentUpdate);
	}

	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		switch(item.getItemId()) {
			case R.id.action_bar_add:
				startActivity(new Intent(this, AddFeedActivity.class));
				return true;
			default:
				return super.onHandleActionBarItemClick(item, position);
		}
	}

	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			fla.notifyDataSetChanged();
		}
	};
}
