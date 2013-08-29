package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import android.widget.TextView;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.ItemviewActivity;
import de.danoeh.antennapod.adapter.ActionButtonCallback;
import de.danoeh.antennapod.adapter.InternalFeedItemlistAdapter;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

import java.util.Iterator;
import java.util.List;

/** Displays a list of FeedItems. */
@SuppressLint("ValidFragment")
public class ItemlistFragment extends ListFragment {
	private static final String TAG = "ItemlistFragment";

	private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
			| EventDistributor.DOWNLOAD_QUEUED
			| EventDistributor.QUEUE_UPDATE
			| EventDistributor.UNREAD_ITEMS_UPDATE;

	public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.danoeh.antennapod.activity.selected_feeditem";
	public static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";
	protected InternalFeedItemlistAdapter fila;

	private Feed feed;
    protected List<Long> queue;

	protected FeedItem selectedItem = null;
	protected boolean contextMenuClosed = true;

	/** Argument for FeeditemlistAdapter */
	protected boolean showFeedtitle;

	public ItemlistFragment(boolean showFeedtitle) {
		super();
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

    private InternalFeedItemlistAdapter.ItemAccess itemAccessRef;
    protected InternalFeedItemlistAdapter.ItemAccess itemAccess() {
        if (itemAccessRef == null) {
            itemAccessRef = new InternalFeedItemlistAdapter.ItemAccess() {

                @Override
                public FeedItem getItem(int position) {
                    return (feed != null) ? feed.getItemAtIndex(true, position) : null;
                }

                @Override
                public int getCount() {
                    return (feed != null) ? feed.getNumOfItems(true) : 0;
                }

                @Override
                public boolean isInQueue(FeedItem item) {
                    return (queue != null) && queue.contains(item.getId());
                }
            };
        }
        return itemAccessRef;
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.feeditemlist, container, false);
	}

    @Override
    public void onStart() {
        super.onStart();
        loadData();
    }

    protected void loadData() {
        final long feedId;
        if (feed == null) {
            feedId = getArguments().getLong(ARGUMENT_FEED_ID);
        } else {
            feedId = feed.getId();
        }
        AsyncTask<Long, Void, Feed> loadTask = new AsyncTask<Long, Void, Feed>(){
            private volatile List<Long> queueRef;

            @Override
            protected Feed doInBackground(Long... longs) {
                Context context = ItemlistFragment.this.getActivity();
                if (context != null) {
                    Feed result = DBReader.getFeed(context, longs[0]);
                    if (result != null) {
                        result.setItems(DBReader.getFeedItemList(context, result));
                        queueRef = DBReader.getQueueIDList(context);
                        return result;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Feed result) {
                super.onPostExecute(result);
                if (result != null && result.getItems() != null) {
                    feed = result;
                    if (queueRef != null) {
                        queue = queueRef;
                    } else {
                        Log.e(TAG, "Could not load queue");
                    }
                    setEmptyViewIfListIsEmpty();
                    if (fila != null) {
                        fila.notifyDataSetChanged();
                    }
                } else {
                    if (result == null) {
                        Log.e(TAG, "Could not load feed with id " + feedId);
                    } else if (result.getItems() == null) {
                        Log.e(TAG, "Could not load feed items");
                    }
                }
            }
        };
        loadTask.execute(feedId);
    }

    private void setEmptyViewIfListIsEmpty() {
        if (getListView() != null && feed != null && feed.getItems() != null) {
            if (feed.getItems().isEmpty()) {
                ((TextView) getActivity().findViewById(android.R.id.empty)).setText(R.string.no_items_label);
            }
        }
    }

	protected InternalFeedItemlistAdapter createListAdapter() {
		return new InternalFeedItemlistAdapter(getActivity(), itemAccess(),
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
                    if (feed != null) {
                        loadData();
                    }
					updateProgressBarVisibility();
				}
			}
		}
	};

	private void updateProgressBarVisibility() {
		if (feed != null) {
			if (DownloadService.isRunning
					&& DownloadRequester.getInstance().isDownloadingFile(feed)) {
                ((ActionBarActivity) getActivity())
						.setSupportProgressBarIndeterminateVisibility(true);
			} else {
                ((ActionBarActivity) getActivity())
						.setSupportProgressBarIndeterminateVisibility(false);
			}
            getActivity().supportInvalidateOptionsMenu();
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
                    }, selectedItem, false, QueueAccess.IDListAccess(queue));

		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		boolean handled = false;

		if (selectedItem != null) {

			try {
				handled = FeedItemMenuHandler.onMenuItemClicked(
						getActivity(), item.getItemId(), selectedItem);
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

	public InternalFeedItemlistAdapter getListAdapter() {
		return fila;
	}

}
