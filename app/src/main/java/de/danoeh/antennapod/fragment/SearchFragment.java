package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;
import de.danoeh.antennapod.core.storage.FeedSearcher;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Performs a search operation on all feeds or one specific feed and displays the search result.
 */
public class SearchFragment extends ListFragment {
    private static final String TAG = "SearchFragment";

    private static final String ARG_QUERY = "query";
    private static final String ARG_FEED = "feed";

    private SearchlistAdapter searchAdapter;
    private List<SearchResult> searchResults = new ArrayList<>();
    private Disposable disposable;

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
    }

    @Override
    public void onStart() {
        super.onStart();
        search();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(disposable != null) {
            disposable.dispose();
        }
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

        searchAdapter = new SearchlistAdapter(getActivity(), itemAccess);
        setListAdapter(searchAdapter);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
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
        MenuItem item = menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        final SearchView sv = new SearchView(getActivity());
        sv.setQueryHint(getString(R.string.search_hint));
        sv.setQuery(getArguments().getString(ARG_QUERY), false);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                getArguments().putString(ARG_QUERY, s);
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

    @Subscribe
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        search();
    }

    private void onSearchResults(List<SearchResult> results) {
        searchResults = results;
        searchAdapter.notifyDataSetChanged();
        String query = getArguments().getString(ARG_QUERY);
        setEmptyText(getString(R.string.no_results_for_query, query));
    }

    private final SearchlistAdapter.ItemAccess itemAccess = new SearchlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return searchResults.size();
        }

        @Override
        public SearchResult getItem(int position) {
            if (0 <= position && position < searchResults.size()) {
                return searchResults.get(position);
            } else {
                return null;
            }
        }
    };

    private void search() {
        if(disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::performSearch)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSearchResults, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private List<SearchResult> performSearch() {
        Bundle args = getArguments();
        String query = args.getString(ARG_QUERY);
        long feed = args.getLong(ARG_FEED);
        Context context = getActivity();
        return FeedSearcher.performSearch(context, query, feed);
    }
}
