package de.danoeh.antennapod.ui.discovery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.discovery.EpisodeSearchResult;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.net.discovery.PodcastSearcher;
import de.danoeh.antennapod.net.discovery.PodcastSearcherRegistry;
import de.danoeh.antennapod.net.discovery.SearchFeedBuilder;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.common.Keyboard;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class OnlineSearchFragment extends Fragment
        implements OnlineSearchEpisodeAdapter.OnEpisodeActionListener {

    public interface PlaybackHost {
        void playMedia(Playable media);
    }

    private static final String TAG = "FyydSearchFragment";
    private static final String ARG_SEARCHER = "searcher";
    private static final String ARG_QUERY = "query";

    private OnlineSearchPodcastAdapter podcastAdapter;
    private OnlineSearchEpisodeAdapter episodeAdapter;
    private PodcastSearcher searchProvider;
    private View resultsContainer;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private RecyclerView recyclerViewFeeds;
    private RecyclerView recyclerViewEpisodes;
    private TextView txtvNoPodcasts;
    private TextView txtvNoEpisodes;
    private ProgressBar progressBar;
    private TextView txtvError;
    private Button butRetry;
    private TextView txtvEmpty;
    private MaterialButton btnSubscribeSearch;
    private String currentQuery;
    private List<PodcastSearchResult> currentPodcastResults = new ArrayList<>();
    private List<EpisodeSearchResult> currentEpisodeResults = new ArrayList<>();

    private Disposable disposable;
    private final CompositeDisposable actionDisposables = new CompositeDisposable();

    public static OnlineSearchFragment newInstance(Class<? extends PodcastSearcher> searchProvider) {
        return newInstance(searchProvider, null);
    }

    public static OnlineSearchFragment newInstance(Class<? extends PodcastSearcher> searchProvider, String query) {
        OnlineSearchFragment fragment = new OnlineSearchFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_SEARCHER, searchProvider.getName());
        arguments.putString(ARG_QUERY, query);
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Constructor
     */
    public OnlineSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (PodcastSearcherRegistry.SearcherInfo info : PodcastSearcherRegistry.getSearchProviders()) {
            if (info.searcher.getClass().getName().equals(getArguments().getString(ARG_SEARCHER))) {
                searchProvider = info.searcher;
                break;
            }
        }
        if (searchProvider == null) {
            throw new IllegalArgumentException("Podcast searcher not found");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_online_search, container, false);
        resultsContainer = root.findViewById(R.id.resultsContainer);
        tabLayout = root.findViewById(R.id.tabLayout);
        viewPager = root.findViewById(R.id.viewPager);

        podcastAdapter = new OnlineSearchPodcastAdapter(getContext(), podcast -> {
            startActivity(new OnlineFeedviewActivityStarter(getContext(), podcast.feedUrl).getIntent());
        });
        episodeAdapter = new OnlineSearchEpisodeAdapter(getContext(), this);

        viewPager.setAdapter(new SearchPagerAdapter());
        viewPager.setOffscreenPageLimit(1);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.podcasts_label);
            } else {
                tab.setText(R.string.episodes_label);
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    Keyboard.hide(getActivity());
                }
            }
        });

        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);
        TextView txtvPoweredBy = root.findViewById(R.id.search_powered_by);
        txtvPoweredBy.setText(getString(R.string.search_powered_by, searchProvider.getName()));
        setupToolbar(root.findViewById(R.id.toolbar));

        loadSubscribedFeeds();

        return root;
    }

    @Override
    public void onEpisodeClick(EpisodeSearchResult episode) {
        startActivity(new OnlineFeedviewActivityStarter(getContext(), episode.feedUrl).getIntent());
    }

    @Override
    public void onPlayClick(EpisodeSearchResult episode) {
        playEpisode(episode);
    }

    @Override
    public void onSubscribeClick(EpisodeSearchResult episode) {
        subscribeEpisode(episode);
    }

    private void loadSubscribedFeeds() {
        actionDisposables.add(Single.fromCallable(() ->
                new HashSet<>(DBReader.getFeedListDownloadUrls(true)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(urls -> {
                    if (episodeAdapter != null) {
                        episodeAdapter.setSubscribedFeedUrls(urls);
                    }
                }, Throwable::printStackTrace));
    }

    private void playEpisode(EpisodeSearchResult episode) {
        actionDisposables.add(Completable.fromAction(() -> {
            Context context = getContext();
            if (context == null || episode.episodeUrl == null) {
                return;
            }
            FeedItem playItem = DBReader.getFeedItemByGuidOrEpisodeUrl(episode.episodeGuid, episode.episodeUrl);
            if (playItem == null) {
                Feed existingFeed = null;
                for (Feed f : DBReader.getFeedList()) {
                    if (f.getDownloadUrl().equals(episode.feedUrl)) {
                        existingFeed = f;
                        break;
                    }
                }
                Feed feed = existingFeed != null
                        ? existingFeed : new Feed(episode.feedUrl, null, episode.podcastTitle);
                if (feed.getState() != Feed.STATE_SUBSCRIBED) {
                    feed.setState(Feed.STATE_NOT_SUBSCRIBED);
                }
                if (feed.getItems() == null) {
                    feed.setItems(new ArrayList<>());
                }
                FeedItem item = new FeedItem(0, episode.title,
                        episode.episodeGuid != null ? episode.episodeGuid : episode.episodeUrl,
                        episode.episodeUrl, episode.pubDate, FeedItem.NEW, feed);
                FeedMedia media = new FeedMedia(item, episode.episodeUrl, 0, "audio/*");
                item.setMedia(media);
                feed.getItems().add(item);
                Feed savedFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
                for (FeedItem i : savedFeed.getItems()) {
                    if ((episode.episodeGuid != null && episode.episodeGuid.equals(i.getItemIdentifier()))
                            || (i.getMedia() != null
                            && episode.episodeUrl.equals(i.getMedia().getDownloadUrl()))) {
                        playItem = i;
                        break;
                    }
                }
                if (playItem == null && !savedFeed.getItems().isEmpty()) {
                    playItem = savedFeed.getItems().get(0);
                }
            }
            if (playItem != null && playItem.getMedia() != null) {
                FeedMedia media = playItem.getMedia();
                AndroidSchedulers.mainThread().scheduleDirect(() -> {
                    if (getActivity() instanceof PlaybackHost) {
                        ((PlaybackHost) getActivity()).playMedia(media);
                    }
                });
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe(() -> { }, Throwable::printStackTrace));
    }

    private void subscribeEpisode(EpisodeSearchResult episode) {
        actionDisposables.add(Completable.fromAction(() -> {
            Context context = getContext();
            if (context == null || episode.feedUrl == null) {
                return;
            }
            Feed existingFeed = null;
            List<Feed> feeds = DBReader.getFeedList();
            for (Feed f : feeds) {
                if (f.getDownloadUrl().equals(episode.feedUrl)) {
                    existingFeed = f;
                    break;
                }
            }
            if (existingFeed != null) {
                DBWriter.setFeedState(context, existingFeed, Feed.STATE_SUBSCRIBED);
                FeedUpdateManager.getInstance().runOnce(context, existingFeed);
            } else {
                Feed feed = new Feed(episode.feedUrl, null, episode.podcastTitle);
                feed.setState(Feed.STATE_SUBSCRIBED);
                Feed savedFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
                FeedUpdateManager.getInstance().runOnce(context, savedFeed);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (episodeAdapter != null) {
                        episodeAdapter.addSubscribedFeedUrl(episode.feedUrl);
                    }
                }, Throwable::printStackTrace));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resultsContainer = null;
        tabLayout = null;
        viewPager = null;
        recyclerViewFeeds = null;
        recyclerViewEpisodes = null;
        txtvNoPodcasts = null;
        txtvNoEpisodes = null;
        progressBar = null;
        txtvError = null;
        butRetry = null;
        txtvEmpty = null;
        btnSubscribeSearch = null;
        podcastAdapter = null;
        episodeAdapter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
        actionDisposables.dispose();
    }

    private void setupToolbar(MaterialToolbar toolbar) {
        toolbar.inflateMenu(R.menu.online_search);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        MenuItem searchItem = toolbar.getMenu().findItem(R.id.action_search);
        final SearchView sv = (SearchView) searchItem.getActionView();
        sv.setQueryHint(getString(R.string.search_podcast_hint));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                search(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        sv.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                Keyboard.show(getContext(), view.findFocus());
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            }
        });
        searchItem.expandActionView();

        if (getArguments().getString(ARG_QUERY, null) != null) {
            sv.setQuery(getArguments().getString(ARG_QUERY, null), true);
        }
    }

    private void search(String query) {
        currentQuery = query;
        if (disposable != null) {
            disposable.dispose();
        }
        showOnlyProgressBar();
        disposable = Single.zip(
                searchProvider.search(query)
                        .map(list -> new Pair<List<PodcastSearchResult>, Throwable>(list, null))
                        .onErrorReturn(error -> new Pair<>(null, error)),
                searchProvider.searchEpisodes(query)
                        .map(list -> new Pair<List<EpisodeSearchResult>, Throwable>(list, null))
                        .onErrorReturn(error -> new Pair<>(null, error)),
                Pair::create)
                .flatMap(combined -> {
                    Pair<List<PodcastSearchResult>, Throwable> podcasts = combined.first;
                    Pair<List<EpisodeSearchResult>, Throwable> episodes = combined.second;
                    if (podcasts.second != null && episodes.second != null) {
                        return Single.error(podcasts.second);
                    }
                    List<PodcastSearchResult> podcastList = podcasts.first != null
                            ? podcasts.first : Collections.emptyList();
                    List<EpisodeSearchResult> episodeList = episodes.first != null
                            ? episodes.first : Collections.emptyList();
                    return Single.just(Pair.create(podcastList, episodeList));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    List<PodcastSearchResult> podcastResults = pair.first;
                    List<EpisodeSearchResult> episodeResults = pair.second;
                    currentPodcastResults = podcastResults != null ? podcastResults : Collections.emptyList();
                    currentEpisodeResults = episodeResults != null ? episodeResults : Collections.emptyList();
                    progressBar.setVisibility(View.GONE);
                    boolean hasPodcasts = !currentPodcastResults.isEmpty();
                    boolean hasEpisodes = !currentEpisodeResults.isEmpty();

                    if (!hasPodcasts && !hasEpisodes) {
                        resultsContainer.setVisibility(View.GONE);
                        txtvEmpty.setVisibility(View.VISIBLE);
                        txtvEmpty.setText(getString(R.string.no_results_for_query, query));
                    } else {
                        txtvEmpty.setVisibility(View.GONE);
                        resultsContainer.setVisibility(View.VISIBLE);
                        if (podcastAdapter != null) {
                            podcastAdapter.updateData(currentPodcastResults);
                        }
                        if (episodeAdapter != null) {
                            episodeAdapter.updateData(currentEpisodeResults);
                        }
                        if (recyclerViewFeeds != null && txtvNoPodcasts != null) {
                            recyclerViewFeeds.setVisibility(hasPodcasts ? View.VISIBLE : View.GONE);
                            txtvNoPodcasts.setVisibility(hasPodcasts ? View.GONE : View.VISIBLE);
                        }
                        if (recyclerViewEpisodes != null && txtvNoEpisodes != null) {
                            recyclerViewEpisodes.setVisibility(hasEpisodes ? View.VISIBLE : View.GONE);
                            txtvNoEpisodes.setVisibility(hasEpisodes ? View.GONE : View.VISIBLE);
                        }
                        if (btnSubscribeSearch != null) {
                            btnSubscribeSearch.setVisibility(hasEpisodes ? View.VISIBLE : View.GONE);
                        }
                        if (hasEpisodes) {
                            checkSearchFeedSubscribed();
                        }
                        if (!hasPodcasts && hasEpisodes && viewPager != null) {
                            viewPager.setCurrentItem(1, false);
                        } else if (hasPodcasts && viewPager != null) {
                            viewPager.setCurrentItem(0, false);
                        }
                    }
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    progressBar.setVisibility(View.GONE);
                    txtvError.setText(error.toString());
                    txtvError.setVisibility(View.VISIBLE);
                    butRetry.setOnClickListener(v -> search(query));
                    butRetry.setVisibility(View.VISIBLE);
                });
    }

    private void checkSearchFeedSubscribed() {
        if (currentQuery == null || btnSubscribeSearch == null) {
            return;
        }
        final String query = currentQuery;
        actionDisposables.add(Single.fromCallable(() -> {
            String searchFeedUrl = Feed.PREFIX_SEARCH_FEED + query;
            return DBReader.getFeedListDownloadUrls(true).contains(searchFeedUrl);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isSubscribed -> {
                    if (query.equals(currentQuery)) {
                        updateSubscribeSearchButton(isSubscribed);
                    }
                }, Throwable::printStackTrace));
    }

    private void updateSubscribeSearchButton(boolean isSubscribed) {
        if (btnSubscribeSearch == null) {
            return;
        }
        if (isSubscribed) {
            btnSubscribeSearch.setEnabled(false);
            btnSubscribeSearch.setText(R.string.subscribed_label);
            btnSubscribeSearch.setIconResource(R.drawable.ic_check);
        } else {
            btnSubscribeSearch.setEnabled(true);
            btnSubscribeSearch.setText(R.string.subscribe_search_feed_label);
            btnSubscribeSearch.setIconResource(R.drawable.ic_add);
        }
    }

    private void subscribeSearchFeed() {
        if (currentQuery == null || currentEpisodeResults == null) {
            return;
        }
        final String query = currentQuery;
        final List<EpisodeSearchResult> episodeResults = new ArrayList<>(currentEpisodeResults);
        actionDisposables.add(Completable.fromAction(() -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            String searchFeedUrl = Feed.PREFIX_SEARCH_FEED + query;
            Feed existingFeed = null;
            for (Feed f : DBReader.getFeedList()) {
                if (searchFeedUrl.equals(f.getDownloadUrl())) {
                    existingFeed = f;
                    break;
                }
            }
            if (existingFeed != null) {
                DBWriter.setFeedState(context, existingFeed, Feed.STATE_SUBSCRIBED);
                FeedUpdateManager.getInstance().runOnce(context, existingFeed);
            } else {
                Feed feed = SearchFeedBuilder.buildFeed(context, query, episodeResults);
                Feed savedFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
                FeedUpdateManager.getInstance().runOnce(context, savedFeed);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    updateSubscribeSearchButton(true);
                }, Throwable::printStackTrace));
    }

    private void showOnlyProgressBar() {
        resultsContainer.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private class SearchPagerAdapter extends RecyclerView.Adapter<SearchPagerAdapter.PageViewHolder> {
        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        Keyboard.hide(getActivity());
                    }
                }
            };
            if (viewType == 0) {
                View view = inflater.inflate(R.layout.online_search_podcasts_page, parent, false);
                recyclerViewFeeds = view.findViewById(R.id.recyclerViewFeeds);
                txtvNoPodcasts = view.findViewById(R.id.txtvNoPodcasts);
                recyclerViewFeeds.setLayoutManager(new LinearLayoutManager(parent.getContext()));
                recyclerViewFeeds.setAdapter(podcastAdapter);
                recyclerViewFeeds.addOnScrollListener(scrollListener);
                return new PageViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.online_search_episodes_page, parent, false);
                recyclerViewEpisodes = view.findViewById(R.id.recyclerViewEpisodes);
                txtvNoEpisodes = view.findViewById(R.id.txtvNoEpisodes);
                btnSubscribeSearch = view.findViewById(R.id.btnSubscribeSearch);
                recyclerViewEpisodes.setLayoutManager(new LinearLayoutManager(parent.getContext()));
                recyclerViewEpisodes.setAdapter(episodeAdapter);
                recyclerViewEpisodes.addOnScrollListener(scrollListener);
                btnSubscribeSearch.setOnClickListener(v -> subscribeSearchFeed());
                return new PageViewHolder(view);
            }
        }

        @Override
        @SuppressLint("RecyclerView")
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                boolean hasPodcasts = !currentPodcastResults.isEmpty();
                if (recyclerViewFeeds != null) {
                    recyclerViewFeeds.setVisibility(hasPodcasts ? View.VISIBLE : View.GONE);
                }
                if (txtvNoPodcasts != null) {
                    txtvNoPodcasts.setVisibility(hasPodcasts ? View.GONE : View.VISIBLE);
                }
            } else {
                checkSearchFeedSubscribed();
                boolean hasEpisodes = !currentEpisodeResults.isEmpty();
                if (recyclerViewEpisodes != null) {
                    recyclerViewEpisodes.setVisibility(hasEpisodes ? View.VISIBLE : View.GONE);
                }
                if (txtvNoEpisodes != null) {
                    txtvNoEpisodes.setVisibility(hasEpisodes ? View.GONE : View.VISIBLE);
                }
                if (btnSubscribeSearch != null) {
                    btnSubscribeSearch.setVisibility(hasEpisodes ? View.VISIBLE : View.GONE);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            PageViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
