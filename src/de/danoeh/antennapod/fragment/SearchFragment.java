package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.dialog.FeedItemDialog;
import de.danoeh.antennapod.feed.*;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.FeedSearcher;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.util.menuhandler.NavDrawerActivity;

import java.util.List;

/**
 * Performs a search operation on all feeds or one specific feed and displays the search result.
 */
public class SearchFragment extends ListFragment {
    private static final String TAG = "SearchFragment";

    private static final String ARG_QUERY = "query";
    private static final String ARG_FEED = "feed";

    private SearchlistAdapter searchAdapter;
    private List<SearchResult> searchResults;

    private boolean viewCreated = false;
    private boolean itemsLoaded = false;

    private QueueAccess queue;

    private FeedItemDialog feedItemDialog;
    private FeedItemDialog.FeedItemDialogSavedInstance feedItemDialogSavedInstance;

    /**
     * Create a new SearchFragment that searches all feeds.
     */
    public static SearchFragment newInstance(String query) {
        if (query == null) query = "";
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putLong(ARG_FEED, 0);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new SearchFragment that searches one specific feed.
     */
    public static SearchFragment newInstance(String query, long feed) {
        SearchFragment fragment = newInstance(query);
        fragment.getArguments().putLong(ARG_FEED, feed);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        startSearchTask();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopSearchTask();
        EventDistributor.getInstance().unregister(contentUpdate);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopSearchTask();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchAdapter = null;
        viewCreated = false;
        if (feedItemDialog != null) {
            feedItemDialogSavedInstance = feedItemDialog.save();
        }
        feedItemDialog = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(R.string.search_label);
        viewCreated = true;
        if (itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        SearchResult result = (SearchResult) l.getAdapter().getItem(position);
        FeedComponent comp = result.getComponent();
        if (comp.getClass() == Feed.class) {
            ((MainActivity)getActivity()).loadFeedFragment(comp.getId());
        } else {
            if (comp.getClass() == FeedItem.class) {
                feedItemDialog = FeedItemDialog.newInstance(getActivity(), (FeedItem) comp, queue);
                feedItemDialog.show();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            MenuItem item = menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label);
            MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            final SearchView sv = new SearchView(getActivity());
            sv.setQueryHint(getString(R.string.search_hint));
            sv.setQuery(getArguments().getString(ARG_QUERY), false);
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    getArguments().putString(ARG_QUERY, s);
                    itemsLoaded = false;
                    startSearchTask();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });
            MenuItemCompat.setActionView(item, sv);
        }
    }

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & (EventDistributor.DOWNLOAD_QUEUED)) != 0) {
                feedItemDialog.updateMenuAppearance();
            }
            if ((arg & (EventDistributor.UNREAD_ITEMS_UPDATE
                    | EventDistributor.DOWNLOAD_HANDLED
                    | EventDistributor.QUEUE_UPDATE)) != 0) {
                startSearchTask();
            }
        }
    };

    private void onFragmentLoaded() {
        if (searchAdapter == null) {
            searchAdapter = new SearchlistAdapter(getActivity(), itemAccess);
            setListAdapter(searchAdapter);
        }
        searchAdapter.notifyDataSetChanged();
        setListShown(true);
        if (feedItemDialog != null && feedItemDialog.isShowing()) {
            feedItemDialog.setQueue(queue);
            for (SearchResult result : searchResults) {
                FeedComponent comp = result.getComponent();
                if (comp.getClass() == FeedItem.class && ((FeedItem) comp).getId() == feedItemDialog.getItem().getId()) {
                    feedItemDialog.setItem((FeedItem) comp);
                }
            }
            feedItemDialog.updateMenuAppearance();
        } else if (feedItemDialogSavedInstance != null) {
            feedItemDialog = FeedItemDialog.newInstance(getActivity(), feedItemDialogSavedInstance);
        }
    }

    private final SearchlistAdapter.ItemAccess itemAccess = new SearchlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (searchResults != null) ? searchResults.size() : 0;
        }

        @Override
        public SearchResult getItem(int position) {
            return (searchResults != null) ? searchResults.get(position) : null;
        }
    };

    private SearchTask searchTask;

    private void startSearchTask() {
        if (searchTask != null) {
            searchTask.cancel(true);
        }
        searchTask = new SearchTask();
        searchTask.execute(getArguments());
    }

    private void stopSearchTask() {
        if (searchTask != null) {
            searchTask.cancel(true);
        }
    }

    private class SearchTask extends AsyncTask<Bundle, Void, Object[]> {
        @Override
        protected Object[] doInBackground(Bundle... params) {
            String query = params[0].getString(ARG_QUERY);
            long feed = params[0].getLong(ARG_FEED);
            Context context = getActivity();
            if (context != null) {
                return new Object[]{FeedSearcher.performSearch(context, query, feed),
                        QueueAccess.IDListAccess(DBReader.getQueueIDList(context))};
            } else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (viewCreated && !itemsLoaded) {
                setListShown(false);
            }
        }

        @Override
        protected void onPostExecute(Object[] results) {
            super.onPostExecute(results);
            if (results != null) {
                itemsLoaded = true;
                searchResults = (List<SearchResult>) results[0];
                queue = (QueueAccess) results[1];
                if (viewCreated) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
