package de.danoeh.antennapod.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.activity.ItemviewActivity;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;

/** Displays a list of FeedItems. */
public class ItemlistFragment extends SherlockListFragment implements
		ActionMode.Callback {

	private static final String TAG = "ItemlistFragment";
	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.danoeh.antennapod.activity.selected_feeditem";
	public static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";
	protected FeedItemlistAdapter fila;
	protected FeedManager manager;
	protected DownloadRequester requester;

	/** The feed which the activity displays */
	protected List<FeedItem> items;
	/**
	 * This is only not null if the fragment displays the items of a specific
	 * feed
	 */
	protected Feed feed;

	protected FeedItem selectedItem;
	protected ActionMode mActionMode;

	/** Argument for FeeditemlistAdapter */
	protected boolean showFeedtitle;

	public ItemlistFragment(List<FeedItem> items, boolean showFeedtitle) {
		super();
		this.items = items;
		this.showFeedtitle = showFeedtitle;
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
	}

	public ItemlistFragment() {
	}

	/**
	 * Creates new ItemlistFragment which shows the Feeditems of a specific
	 * feed. Sets 'showFeedtitle' to false
	 * 
	 * @param feedId
	 *            The id of the feed to show
	 * @return the newly created instance of an ItemlistFragment
	 */
	public static ItemlistFragment newInstance(long feedId) {
		ItemlistFragment i = new ItemlistFragment();
		i.showFeedtitle = false;
		Bundle b = new Bundle();
		b.putLong(ARGUMENT_FEED_ID, feedId);
		i.setArguments(b);
		return i;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.feeditemlist, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (items == null) {
			long feedId = getArguments().getLong(ARGUMENT_FEED_ID);
			feed = FeedManager.getInstance().getFeed(feedId);
			items = feed.getItems();
		}
		fila = new FeedItemlistAdapter(getActivity(), 0, items,
				onButActionClicked, showFeedtitle);
		setListAdapter(fila);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mActionMode != null) {
			mActionMode.finish();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(contentUpdate);

	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				fila.notifyDataSetChanged();
			}
		});
		updateProgressBarVisibility();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
		filter.addAction(FeedManager.ACTION_QUEUE_UPDATE);
		filter.addAction(FeedManager.ACTION_UNREAD_ITEMS_UPDATE);

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
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received contentUpdate Intent.");
			if (intent.getAction().equals(
					DownloadRequester.ACTION_DOWNLOAD_QUEUED)) {
				updateProgressBarVisibility();
			} else {
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						fila.notifyDataSetChanged();
						updateProgressBarVisibility();
					}

				});
			}
		}
	};

	private void updateProgressBarVisibility() {
		if (feed != null) {
			if (DownloadService.isRunning
					&& DownloadRequester.getInstance().isDownloadingFile(feed)) {
				getSherlockActivity()
						.setSupportProgressBarIndeterminateVisibility(true);
			} else {
				getSherlockActivity()
						.setSupportProgressBarIndeterminateVisibility(false);
			}
			getSherlockActivity().invalidateOptionsMenu();
		}
	}

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
							ItemlistFragment.this);
					fila.setSelectedItemIndex(index);
				} else {
					mActionMode.finish();
				}

			}
		}
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		this.getListView().setItemsCanFocus(true);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return FeedItemMenuHandler.onPrepareMenu(menu, selectedItem);
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		mActionMode = null;
		selectedItem = null;
		fila.setSelectedItemIndex(FeedItemlistAdapter.SELECTION_NONE);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		return FeedItemMenuHandler.onCreateMenu(mode.getMenuInflater(), menu);

	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		boolean handled = FeedItemMenuHandler.onMenuItemClicked(
				getSherlockActivity(), item, selectedItem);
		if (handled) {
			fila.notifyDataSetChanged();
		}
		mode.finish();
		return handled;
	}

	public FeedItemlistAdapter getListAdapter() {
		return fila;
	}

}
