package de.podfetcher.fragment;

import de.podfetcher.R;
import de.podfetcher.feed.*;
import de.podfetcher.activity.*;
import de.podfetcher.adapter.FeedlistAdapter;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.service.DownloadService;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.util.Log;


public class FeedlistFragment extends SherlockListFragment {
	private static final String TAG = "FeedlistFragment";
	public static final String EXTRA_SELECTED_FEED = "extra.de.podfetcher.activity.selected_feed";
	
	private FeedManager manager;
	private FeedlistAdapter fla;
	private SherlockFragmentActivity pActivity;


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		pActivity = (SherlockFragmentActivity) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		pActivity = null;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Creating");	
		manager = FeedManager.getInstance();
		fla = new FeedlistAdapter(pActivity, 0, manager.getFeeds());
		setListAdapter(fla);

	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.feedlist, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_FEED_SYNC_COMPLETED);

		pActivity.registerReceiver(contentUpdate, filter);
        fla.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		pActivity.unregisterReceiver(contentUpdate);
	}

	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received contentUpdate Intent.");
			fla.notifyDataSetChanged();
		}
	};

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Feed selection = fla.getItem(position);
		Intent showFeed = new Intent(pActivity, FeedItemlistActivity.class);
		showFeed.putExtra(EXTRA_SELECTED_FEED, selection.getId());

		pActivity.startActivity(showFeed);
	}
}
