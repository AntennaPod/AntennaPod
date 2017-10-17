package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;
import de.danoeh.antennapod.core.storage.FeedSearcher;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

    private Subscription subscription;

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
        search();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(subscription != null) {
            subscription.unsubscribe();
        }
        EventDistributor.getInstance().unregister(contentUpdate);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchAdapter = null;
        viewCreated = false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.search_label);
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
            ((MainActivity) getActivity()).loadFeedFragmentById(comp.getId(), null);
        } else {
            if (comp.getClass() == FeedItem.class) {
                FeedItem item = (FeedItem) comp;
                ((MainActivity) getActivity()).loadChildFragment(ItemFragment.newInstance(item.getId()));
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (itemsLoaded) {
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
                    search();
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
            if ((arg & (EventDistributor.UNREAD_ITEMS_UPDATE
                    | EventDistributor.DOWNLOAD_HANDLED)) != 0) {
                search();
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

        String query = getArguments().getString(ARG_QUERY);
        setEmptyText(getString(R.string.no_results_for_query, query));
    }

    private final SearchlistAdapter.ItemAccess itemAccess = new SearchlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (searchResults != null) ? searchResults.size() : 0;
        }

        @Override
        public SearchResult getItem(int position) {
            if (searchResults != null && 0 <= position && position < searchResults.size()) {
                return searchResults.get(position);
            } else {
                return null;
            }
        }
    };


    private void search() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if (viewCreated && !itemsLoaded) {
            setListShown(false);
        }
        subscription = Observable.fromCallable(this::performSearch)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        itemsLoaded = true;
                        searchResults = result;
                        if (viewCreated) {
                            onFragmentLoaded();
                        }
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private List<SearchResult> performSearch() {
        Bundle args = getArguments();
        String query = args.getString(ARG_QUERY);
        long feed = args.getLong(ARG_FEED);
        Context context = getActivity();
        return FeedSearcher.performSearch(context, query, feed);
    }

}
