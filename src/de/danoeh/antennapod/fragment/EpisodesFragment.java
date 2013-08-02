package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

import java.util.List;

public class EpisodesFragment extends SherlockFragment {
	private static final String TAG = "EpisodesFragment";

	private static final int EVENTS = EventDistributor.QUEUE_UPDATE
			| EventDistributor.UNREAD_ITEMS_UPDATE
			| EventDistributor.FEED_LIST_UPDATE
			| EventDistributor.DOWNLOAD_HANDLED
			| EventDistributor.DOWNLOAD_QUEUED;

	private ExpandableListView listView;
	private ExternalEpisodesListAdapter adapter;

    private List<FeedItem> queue;
    private List<FeedItem> unreadItems;

	protected FeedItem selectedItem = null;
	protected long selectedGroupId = -1;
	protected boolean contextMenuClosed = true;

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventDistributor.getInstance().unregister(contentUpdate);
	}

	@Override
	public void onResume() {
		super.onResume();

		EventDistributor.getInstance().register(contentUpdate);
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
				adapterCallback, groupActionCallback, itemAccess);
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
        loadData();
		registerForContextMenu(listView);

	}

    ExternalEpisodesListAdapter.ItemAccess itemAccess = new ExternalEpisodesListAdapter.ItemAccess() {

        @Override
        public int getQueueSize() {
            return (queue != null) ? queue.size() : 0;
        }

        @Override
        public int getUnreadItemsSize() {
            return (unreadItems != null) ? unreadItems.size() : 0;
        }

        @Override
        public FeedItem getQueueItemAt(int position) {
            return (queue != null) ? queue.get(position) : null;
        }

        @Override
        public FeedItem getUnreadItemAt(int position) {
            return (unreadItems != null) ? unreadItems.get(position) : null;
        }
    };

    private void loadData() {
        AsyncTask<Void, Void, Void> loadTask = new AsyncTask<Void, Void, Void>() {
            private volatile List<FeedItem> queueRef;
            private volatile List<FeedItem> unreadItemsRef;

            @Override
            protected Void doInBackground(Void... voids) {
                if (AppConfig.DEBUG) Log.d(TAG, "Starting to load list data");
                Context context = EpisodesFragment.this.getActivity();
                if (context != null) {
                    queueRef = DBReader.getQueue(context);
                    unreadItemsRef = DBReader.getUnreadItemsList(context);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (queueRef != null && unreadItemsRef != null) {
                    if (AppConfig.DEBUG) Log.d(TAG, "Done loading list data");
                    queue = queueRef;
                    unreadItems = unreadItemsRef;
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    if (queueRef == null) {
                        Log.e(TAG, "Could not load queue");
                    }
                    if (unreadItemsRef == null) {
                        Log.e(TAG, "Could not load unread items");
                    }
                }
            }
        };
        loadTask.execute();
    }

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((EVENTS & arg) != 0) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Received contentUpdate Intent.");
				loadData();
			}
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
                    }, selectedItem, false, QueueAccess.ItemListAccess(queue));

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
				DBWriter.clearQueue(getActivity());
				break;
			case R.id.download_all_item:
				DBTasks.downloadAllItemsInQueue(getActivity());
				break;
			default:
				handled = false;
			}
		} else if (selectedGroupId == ExternalEpisodesListAdapter.GROUP_POS_UNREAD) {
			handled = true;
			switch (item.getItemId()) {
			case R.id.mark_all_read_item:
				DBWriter.markAllItemsRead(getActivity());
				break;
			case R.id.enqueue_all_item:
				DBTasks.enqueueAllNewItems(getActivity());
				break;
			default:
				handled = false;
			}
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
