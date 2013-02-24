package de.danoeh.antennapod.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.ItemviewActivity;
import de.danoeh.antennapod.activity.OrganizeQueueActivity;
import de.danoeh.antennapod.adapter.ActionButtonCallback;
import de.danoeh.antennapod.adapter.ExternalEpisodesListAdapter;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

public class EpisodesFragment extends SherlockFragment {
	private static final String TAG = "EpisodesFragment";

	private ExpandableListView listView;
	private ExternalEpisodesListAdapter adapter;

	protected FeedItem selectedItem = null;
	protected long selectedGroupId = -1;
	protected boolean contextMenuClosed = true;

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			getActivity().unregisterReceiver(contentUpdate);
		} catch (IllegalArgumentException e) {

		}
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadRequester.ACTION_DOWNLOAD_QUEUED);
		filter.addAction(DownloadService.ACTION_DOWNLOAD_HANDLED);
		filter.addAction(FeedManager.ACTION_QUEUE_UPDATE);
		filter.addAction(FeedManager.ACTION_UNREAD_ITEMS_UPDATE);
		filter.addAction(FeedManager.ACTION_FEED_LIST_UPDATE);

		getActivity().registerReceiver(contentUpdate, filter);
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.episodes_fragment, null);
		listView = (ExpandableListView) v.findViewById(android.R.id.list);
		return v;
	}

	protected ActionButtonCallback adapterCallback = new ActionButtonCallback() {

		@Override
		public void onActionButtonPressed(FeedItem item) {
			resetContextMenuSelection();
			selectedItem = item;
			listView.showContextMenu();
		}
	};

	protected ExternalEpisodesListAdapter.OnGroupActionClicked groupActionCallback = new ExternalEpisodesListAdapter.OnGroupActionClicked() {

		@Override
		public void onClick(long groupId) {
			resetContextMenuSelection();
			selectedGroupId = groupId;
			listView.showContextMenu();
		}
	};

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adapter = new ExternalEpisodesListAdapter(getActivity(),
				adapterCallback, groupActionCallback);
		listView.setAdapter(adapter);
		listView.expandGroup(ExternalEpisodesListAdapter.GROUP_POS_QUEUE);
		listView.expandGroup(ExternalEpisodesListAdapter.GROUP_POS_UNREAD);
		listView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				FeedItem selection = adapter.getChild(groupPosition,
						childPosition);
				if (selection != null) {
					Intent showItem = new Intent(getActivity(),
							ItemviewActivity.class);
					showItem.putExtra(FeedlistFragment.EXTRA_SELECTED_FEED,
							selection.getFeed().getId());
					showItem.putExtra(ItemlistFragment.EXTRA_SELECTED_FEEDITEM,
							selection.getId());

					startActivity(showItem);
					return true;
				}
				return true;
			}
		});
		registerForContextMenu(listView);
	}

	private BroadcastReceiver contentUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received contentUpdate Intent.");
			adapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onCreateContextMenu(final ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (!contextMenuClosed) { // true if context menu was cancelled before
			resetContextMenuSelection();
		}
		contextMenuClosed = false;
		listView.setOnItemLongClickListener(null);
		if (selectedItem != null) {
			new MenuInflater(getActivity()).inflate(R.menu.feeditem, menu);

			menu.setHeaderTitle(selectedItem.getTitle());
			FeedItemMenuHandler.onPrepareMenu(
					new FeedItemMenuHandler.MenuInterface() {

						@Override
						public void setItemVisibility(int id, boolean visible) {
							menu.findItem(id).setVisible(visible);
						}
					}, selectedItem, false);

		} else if (selectedGroupId == ExternalEpisodesListAdapter.GROUP_POS_QUEUE) {
			menu.add(Menu.NONE, R.id.organize_queue_item, Menu.NONE,
					R.string.organize_queue_label);
			menu.add(Menu.NONE, R.id.clear_queue_item, Menu.NONE, getActivity()
					.getString(R.string.clear_queue_label));
			menu.add(Menu.NONE, R.id.download_all_item, Menu.NONE,
					getActivity().getString(R.string.download_all));
		} else if (selectedGroupId == ExternalEpisodesListAdapter.GROUP_POS_UNREAD) {
			menu.add(Menu.NONE, R.id.mark_all_read_item, Menu.NONE,
					getActivity().getString(R.string.mark_all_read_label));
			menu.add(Menu.NONE, R.id.enqueue_all_item, Menu.NONE, getActivity()
					.getString(R.string.enqueue_all_new));
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		boolean handled = false;
		FeedManager manager = FeedManager.getInstance();
		if (selectedItem != null) {
			try {
				handled = FeedItemMenuHandler.onMenuItemClicked(
						getSherlockActivity(), item.getItemId(), selectedItem);
			} catch (DownloadRequestException e) {
				e.printStackTrace();
				DownloadRequestErrorDialogCreator.newRequestErrorDialog(
						getActivity(), e.getMessage());
			}

		} else if (selectedGroupId == ExternalEpisodesListAdapter.GROUP_POS_QUEUE) {
			handled = true;
			switch (item.getItemId()) {
			case R.id.organize_queue_item:
				startActivity(new Intent(getActivity(),
						OrganizeQueueActivity.class));
				break;
			case R.id.clear_queue_item:
				manager.clearQueue(getActivity());
				break;
			case R.id.download_all_item:
				manager.downloadAllItemsInQueue(getActivity());
				break;
			default:
				handled = false;
			}
		} else if (selectedGroupId == ExternalEpisodesListAdapter.GROUP_POS_UNREAD) {
			handled = true;
			switch (item.getItemId()) {
			case R.id.mark_all_read_item:
				manager.markAllItemsRead(getActivity());
				break;
			case R.id.enqueue_all_item:
				manager.enqueueAllNewItems(getActivity());
				break;
			default:
				handled = false;
			}
		}

		if (handled) {
			adapter.notifyDataSetChanged();
		}
		resetContextMenuSelection();
		return handled;
	}

	private void resetContextMenuSelection() {
		selectedItem = null;
		selectedGroupId = -1;
		contextMenuClosed = true;
	}
}
