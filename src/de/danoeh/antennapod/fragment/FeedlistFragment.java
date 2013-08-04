package de.danoeh.antennapod.fragment;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.FeedItemlistActivity;
import de.danoeh.antennapod.adapter.FeedlistAdapter;
import de.danoeh.antennapod.asynctask.FeedRemover;
import de.danoeh.antennapod.dialog.ConfirmationDialog;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.FeedItemStatistics;
import de.danoeh.antennapod.util.menuhandler.FeedMenuHandler;

public class FeedlistFragment extends SherlockFragment implements
        ActionMode.Callback, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {
    private static final String TAG = "FeedlistFragment";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
            | EventDistributor.DOWNLOAD_QUEUED
            | EventDistributor.FEED_LIST_UPDATE
            | EventDistributor.UNREAD_ITEMS_UPDATE;

    public static final String EXTRA_SELECTED_FEED = "extra.de.danoeh.antennapod.activity.selected_feed";

    private FeedlistAdapter fla;
    private List<Feed> feeds;
    private List<FeedItemStatistics> feedItemStatistics;

    private Feed selectedFeed;
    private ActionMode mActionMode;

    private GridView gridView;
    private ListView listView;
    private TextView txtvEmpty;

    private FeedlistAdapter.ItemAccess itemAccess = new FeedlistAdapter.ItemAccess() {

        @Override
        public Feed getItem(int position) {
            if (feeds != null) {
                return feeds.get(position);
            } else {
                return null;
            }
        }

        @Override
        public FeedItemStatistics getFeedItemStatistics(int position) {
            if (feedItemStatistics != null && position < feedItemStatistics.size()) {
                return feedItemStatistics.get(position);
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            if (feeds != null) {
                return feeds.size();
            } else {
                return 0;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppConfig.DEBUG)
            Log.d(TAG, "Creating");
        fla = new FeedlistAdapter(getActivity(), itemAccess);
        loadFeeds();
    }

    private void loadFeeds() {
        AsyncTask<Void, Void, List[]> loadTask = new AsyncTask<Void, Void, List[]>() {
            @Override
            protected List[] doInBackground(Void... params) {
                return new List[]{DBReader.getFeedList(getActivity()),
                        DBReader.getFeedStatisticsList(getActivity())};
            }

            @Override
            protected void onPostExecute(List[] result) {
                super.onPostExecute(result);
                if (result != null) {
                    feeds = result[0];
                    feedItemStatistics = result[1];
                    if (fla != null) {
                        fla.notifyDataSetChanged();
                    }
                } else {
                    Log.e(TAG, "Failed to load feeds");
                }
            }
        };
        loadTask.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.feedlist, container, false);
        listView = (ListView) result.findViewById(android.R.id.list);
        gridView = (GridView) result.findViewById(R.id.grid);
        txtvEmpty = (TextView) result.findViewById(android.R.id.empty);

        return result;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (listView != null) {
            listView.setOnItemClickListener(this);
            listView.setOnItemLongClickListener(this);
            listView.setAdapter(fla);
            listView.setEmptyView(txtvEmpty);
            if (AppConfig.DEBUG)
                Log.d(TAG, "Using ListView");
        } else {
            gridView.setOnItemClickListener(this);
            gridView.setOnItemLongClickListener(this);
            gridView.setAdapter(fla);
            gridView.setEmptyView(txtvEmpty);
            if (AppConfig.DEBUG)
                Log.d(TAG, "Using GridView");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AppConfig.DEBUG)
            Log.d(TAG, "Resuming");
        EventDistributor.getInstance().register(contentUpdate);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventDistributor.getInstance().unregister(contentUpdate);
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EVENTS & arg) != 0) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Received contentUpdate Intent.");
                loadFeeds();
            }
        }
    };

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        FeedMenuHandler.onCreateOptionsMenu(mode.getMenuInflater(), menu);
        mode.setTitle(selectedFeed.getTitle());
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return FeedMenuHandler.onPrepareOptionsMenu(menu, selectedFeed);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        try {
            if (FeedMenuHandler.onOptionsItemClicked(getSherlockActivity(),
                    item, selectedFeed)) {
                loadFeeds();
            } else {
                switch (item.getItemId()) {
                    case R.id.remove_item:
                        final FeedRemover remover = new FeedRemover(
                                getSherlockActivity(), selectedFeed) {
                            @Override
                            protected void onPostExecute(Void result) {
                                super.onPostExecute(result);
                                loadFeeds();
                            }
                        };
                        ConfirmationDialog conDialog = new ConfirmationDialog(
                                getActivity(), R.string.remove_feed_label,
                                R.string.feed_delete_confirmation_msg) {

                            @Override
                            public void onConfirmButtonPressed(
                                    DialogInterface dialog) {
                                dialog.dismiss();
                                remover.executeAsync();
                            }
                        };
                        conDialog.createNewDialog().show();
                        break;
                }
            }
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(
                    getActivity(), e.getMessage());
        }
        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        selectedFeed = null;
        fla.setSelectedItemIndex(FeedlistAdapter.SELECTION_NONE);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long id) {
        Feed selection = fla.getItem(position);
        Intent showFeed = new Intent(getActivity(), FeedItemlistActivity.class);
        showFeed.putExtra(EXTRA_SELECTED_FEED, selection.getId());

        getActivity().startActivity(showFeed);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        Feed selection = fla.getItem(position);
        if (AppConfig.DEBUG)
            Log.d(TAG, "Selected Feed with title " + selection.getTitle());
        if (selection != null) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            fla.setSelectedItemIndex(position);
            selectedFeed = selection;
            mActionMode = getSherlockActivity().startActionMode(
                    FeedlistFragment.this);

        }
        return true;
    }
}
