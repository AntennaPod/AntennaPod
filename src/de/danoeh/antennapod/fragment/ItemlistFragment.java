package de.danoeh.antennapod.fragment;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.ItemviewActivity;
import de.danoeh.antennapod.adapter.AbstractFeedItemlistAdapter;
import de.danoeh.antennapod.adapter.ActionButtonCallback;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

/** Displays a list of FeedItems. */
public class ItemlistFragment extends SherlockListFragment {

	private static final String TAG = "ItemlistFragment";
	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.danoeh.antennapod.activity.selected_feeditem";
	public static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";
	protected AbstractFeedItemlistAdapter fila;
	protected FeedManager manager;
	protected DownloadRequester requester;

	/** The feed which the activity displays */
	protected List<FeedItem> items;
	/**
	 * This is only not null if the fragment displays the items of a specific
	 * feed
	 */
	protected Feed feed;

	protected static final int NO_SELECTION = -1;
	protected int selectedPosition = NO_SELECTION;
	protected boolean contextMenuClosed = true;

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

		fila = createListAdapter();
		setListAdapter(fila);
	}
	
	protected AbstractFeedItemlistAdapter createListAdapter() {
		return new FeedItemlistAdapter(getActivity(), 0, items,
				adapterCallback, showFeedtitle);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			getActivity().unregisterReceiver(contentUpdate);
		} catch (IllegalArgumentException e) {
			Log.w(TAG,
					"IllegalArgumentException when trying to unregister contentUpdate receiver.");
		}
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
				fila.notifyDataSetChanged();
				updateProgressBarVisibility();
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

	protected ActionButtonCallback adapterCallback = new ActionButtonCallback() {

		@Override
		public void onActionButtonPressed(int position) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "adapterCallback; position = " + position);
			selectedPosition = position;
			contextMenuClosed = true;
			getListView().showContextMenu();
		}
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		this.getListView().setItemsCanFocus(true);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(getListView());
		getListView().setOnItemLongClickListener(null);
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (!contextMenuClosed) { // true if context menu was cancelled before
			selectedPosition = NO_SELECTION;
		}
		contextMenuClosed = false;
		getListView().setOnItemLongClickListener(null);
		if (selectedPosition != NO_SELECTION) {
			new MenuInflater(ItemlistFragment.this.getActivity()).inflate(
					R.menu.feeditem, menu);
			FeedItem selection = fila.getItem(selectedPosition);
			if (selection != null) {
				menu.setHeaderTitle(selection.getTitle());
				FeedItemMenuHandler.onPrepareMenu(
						new FeedItemMenuHandler.MenuInterface() {

							@Override
							public void setItemVisibility(int id,
									boolean visible) {
								menu.findItem(id).setVisible(visible);
							}
						}, selection, false);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		boolean handled = false;

		if (selectedPosition != NO_SELECTION) {
			FeedItem selectedItem = fila.getItem(selectedPosition);

			if (selectedItem != null) {
				try {
					handled = FeedItemMenuHandler.onMenuItemClicked(
							getSherlockActivity(), item.getItemId(),
							selectedItem);
				} catch (DownloadRequestException e) {
					e.printStackTrace();
					DownloadRequestErrorDialogCreator.newRequestErrorDialog(
							getActivity(), e.getMessage());
				}
				if (handled) {
					fila.notifyDataSetChanged();
				}
			}
		}
		selectedPosition = NO_SELECTION;
		contextMenuClosed = true;
		return handled;
	}

	public AbstractFeedItemlistAdapter getListAdapter() {
		return fila;
	}

}
