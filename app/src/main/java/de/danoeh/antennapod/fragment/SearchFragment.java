package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SearchlistAdapter;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.FeedSearcher;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Performs a search operation on all feeds or one specific feed and displays the search result.
 */
public class SearchFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "SearchFragment";

    private static final String ARG_QUERY = "query";
    private static final String ARG_FEED = "feed";

    private SearchlistAdapter searchAdapter;
    private List<FeedComponent> searchResults = new ArrayList<>();
    private Disposable disposable;
    private ProgressBar progressBar;
    private EmptyViewHandler emptyViewHandler;

    /**
     * Create a new SearchFragment that searches all feeds.
     */
    public static SearchFragment newInstance(String query) {
        if (query == null) {
            query = "";
        }
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
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.search_label);

        View layout = inflater.inflate(R.layout.search_fragment, container, false);
        ListView listView = layout.findViewById(R.id.listview);
        progressBar = layout.findViewById(R.id.progressBar);
        searchAdapter = new SearchlistAdapter(getActivity(), itemAccess);
        listView.setAdapter(searchAdapter);
        listView.setOnItemClickListener(this);

        emptyViewHandler = new EmptyViewHandler(getContext());
        emptyViewHandler.attachToListView(listView);
        emptyViewHandler.setIcon(R.attr.action_search);
        emptyViewHandler.setTitle(R.string.search_status_no_results);
        EventBus.getDefault().register(this);
        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FeedComponent comp = searchAdapter.getItem(position);
        if (comp.getClass() == Feed.class) {
            ((MainActivity) getActivity()).loadFeedFragmentById(comp.getId(), null);
        } else if (comp.getClass() == FeedItem.class) {
            FeedItem item = (FeedItem) comp;
            ((MainActivity) getActivity()).loadChildFragment(ItemPagerFragment.newInstance(item.getId()));
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item = menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        final SearchView sv = new SearchView(getActivity());
        sv.setQueryHint(getString(R.string.search_label));
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

    private void onSearchResults(List<FeedComponent> results) {
        progressBar.setVisibility(View.GONE);
        searchResults = results;
        searchAdapter.notifyDataSetChanged();
        String query = getArguments().getString(ARG_QUERY);
        emptyViewHandler.setMessage(getString(R.string.no_results_for_query, query));
    }

    private final SearchlistAdapter.ItemAccess itemAccess = new SearchlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return searchResults.size();
        }

        @Override
        public FeedComponent getItem(int position) {
            if (0 <= position && position < searchResults.size()) {
                return searchResults.get(position);
            } else {
                return null;
            }
        }
    };

    private void search() {
        if (disposable != null) {
            disposable.dispose();
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyViewHandler.hide();
        disposable = Observable.fromCallable(this::performSearch)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSearchResults, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private List<FeedComponent> performSearch() {
        Bundle args = getArguments();
        String query = args.getString(ARG_QUERY);
        long feed = args.getLong(ARG_FEED);
        Context context = getActivity();
        return FeedSearcher.performSearch(context, query, feed);
    }
}
