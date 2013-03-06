package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.ItemviewActivity;
import de.danoeh.antennapod.adapter.AbstractFeedItemlistAdapter;
import de.danoeh.antennapod.adapter.ActionButtonCallback;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

/** Displays a list of FeedItems. */
@SuppressLint("ValidFragment")
public class ItemlistFragment extends SherlockListFragment {
	private static final String TAG = "ItemlistFragment";

	private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
			| EventDistributor.DOWNLOAD_QUEUED
			| EventDistributor.QUEUE_UPDATE
			| EventDistributor.UNREAD_ITEMS_UPDATE;

	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.danoeh.antennapod.activity.selected_feeditem";
	public static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";
	protected AbstractFeedItemlistAdapter fila;
	protected FeedManager manager = FeedManager.getInstance();
	protected DownloadRequester requester = DownloadRequester.getInstance();

	private AbstractFeedItemlistAdapter.ItemAccess itemAccess;

	private Feed feed;

	protected FeedItem selectedItem = null;
	protected boolean contextMenuClosed = true;

	/** Argument for FeeditemlistAdapter */
	protected boolean showFeedtitle;

	public ItemlistFragment(AbstractFeedItemlistAdapter.ItemAccess itemAccess,
			boolean showFeedtitle) {
		super();
		this.itemAccess = itemAccess;
		this.showFeedtitle = showFeedtitle;
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
		if (itemAccess == null) {
			long feedId = getArguments().getLong(ARGUMENT_FEED_ID);
			final Feed feed = FeedManager.getInstance().getFeed(feedId);
			this.feed = feed;
			itemAccess = new AbstractFeedItemlistAdapter.ItemAccess() {

				@Override
				public FeedItem getItem(int position) {
					return feed.getItemAtIndex(true, position);
				}

				@Override
				public int getCount() {
					return feed.getNumOfItems(true);
				}
			};
		}
	}

	protected AbstractFeedItemlistAdapter createListAdapter() {
		return new FeedItemlistAdapter(getActivity(), itemAccess,
				adapterCallback, showFeedtitle);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventDistributor.getInstance().unregister(contentUpdate);
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
		EventDistributor.getInstance().register(contentUpdate);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FeedItem selection = fila.getItem(position - l.getHeaderViewsCount());
		Intent showItem = new Intent(getActivity(), ItemviewActivity.class);
		showItem.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED, selection
				.getFeed().getId());
		showItem.putExtra(EXTRA_SELECTED_FEEDITEM, selection.getId());

		startActivity(showItem);
	}

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((EVENTS & arg) != 0) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received contentUpdate Intent.");
				if ((EventDistributor.DOWNLOAD_QUEUED & arg) != 0) {
					updateProgressBarVisibility();
				} else {
					fila.notifyDataSetChanged();
					updateProgressBarVisibility();
				}
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
			getSherlockActivity().supportInvalidateOptionsMenu();
		}
	}

	protected ActionButtonCallback adapterCallback = new ActionButtonCallback() {

		@Override
		public void onActionButtonPressed(FeedItem item) {
			selectedItem = item;
			contextMenuClosed = true;
			getListView().showContextMenu();
		}
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		fila = createListAdapter();
		setListAdapter(fila);
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
			selectedItem = null;
		}
		contextMenuClosed = false;
		getListView().setOnItemLongClickListener(null);
		if (selectedItem != null) {
			new MenuInflater(ItemlistFragment.this.getActivity()).inflate(
					R.menu.feeditem, menu);

			menu.setHeaderTitle(selectedItem.getTitle());
			FeedItemMenuHandler.onPrepareMenu(
					new FeedItemMenuHandler.MenuInterface() {

						@Override
						public void setItemVisibility(int id, boolean visible) {
							menu.findItem(id).setVisible(visible);
						}
					}, selectedItem, false);

		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		boolean handled = false;

		if (selectedItem != null) {

			try {
				handled = FeedItemMenuHandler.onMenuItemClicked(
						getSherlockActivity(), item.getItemId(), selectedItem);
			} catch (DownloadRequestException e) {
				e.printStackTrace();
				DownloadRequestErrorDialogCreator.newRequestErrorDialog(
						getActivity(), e.getMessage());
			}
			if (handled) {
				fila.notifyDataSetChanged();
			}

		}
		selectedItem = null;
		contextMenuClosed = true;
		return handled;
	}

	public AbstractFeedItemlistAdapter getListAdapter() {
		return fila;
	}

}
