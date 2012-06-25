package de.podfetcher.fragment;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.activity.ItemviewActivity;
import de.podfetcher.adapter.FeedItemlistAdapter;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.service.DownloadService;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.util.FeedItemMenuHandler;

/** Displays a list of FeedItems. */
public class ItemlistFragment extends SherlockListFragment {

	private static final String TAG = "FeedItemlistFragment";
	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.podfetcher.activity.selected_feeditem";
	public static final String ARGUMENT_FEED_ID = "argument.de.podfetcher.feed_id";
	protected FeedItemlistAdapter fila;
	protected FeedManager manager;
	protected DownloadRequester requester;

	/** The feed which the activity displays */
	protected ArrayList<FeedItem> items;

	protected FeedItem selectedItem;
	protected ActionMode mActionMode;

	public ItemlistFragment(ArrayList<FeedItem> items) {
		super();
		this.items = items;
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
	}

	public ItemlistFragment() {
	}

	public static ItemlistFragment newInstance(long feedId) {
		ItemlistFragment i = new ItemlistFragment();
		Bundle b = new Bundle();
		b.putLong(ARGUMENT_FEED_ID, feedId);
		i.setArguments(b);
		return i;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (items == null) {
			long feedId = getArguments().getLong(ARGUMENT_FEED_ID);
			items = FeedManager.getInstance().getFeed(feedId).getItems();
		}
		fila = new FeedItemlistAdapter(getActivity(), 0, items,
				onButActionClicked);
		setListAdapter(fila);
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(contentUpdate);
		if (mActionMode != null) {
			mActionMode.finish();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		fila.notifyDataSetChanged();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);

		getActivity().registerReceiver(contentUpdate, filter);
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FeedItem selection = fila.getItem(position);
		Intent showItem = new Intent(getActivity(), ItemviewActivity.class);
		showItem.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED, selection
				.getFeed().getId());
		showItem.putExtra(EXTRA_SELECTED_FEEDITEM, selection.getId());

		startActivity(showItem);
	}
	
	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received contentUpdate Intent.");
			fila.notifyDataSetChanged();
		}
	};

	private final OnClickListener onButActionClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int index = getListView().getPositionForView(v);
			if (index != ListView.INVALID_POSITION) {
				FeedItem newSelectedItem = items.get(index);
				if (newSelectedItem != selectedItem) {
					if (mActionMode != null) {
						mActionMode.finish();
					}

					selectedItem = newSelectedItem;
					mActionMode = getSherlockActivity().startActionMode(
							mActionModeCallback);
				} else {
					mActionMode.finish();
				}

			}
		}
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		this.getListView().setItemsCanFocus(true);
	}
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return FeedItemMenuHandler.onPrepareMenu(menu, selectedItem);
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			selectedItem = null;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			return FeedItemMenuHandler.onCreateMenu(mode.getMenuInflater(), menu);

		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			FeedItemMenuHandler.onMenuItemClicked(getSherlockActivity(), item, selectedItem);
			fila.notifyDataSetChanged();
			mode.finish();
			return true;
		}
	};
	
	public FeedItemlistAdapter getListAdapter() {
		return fila;
	}
	

}
