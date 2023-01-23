package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.FeedDiscoverAdapter;
import de.danoeh.antennapod.event.DiscoveryDefaultUpdateEvent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;


public class QuickFeedDiscoveryFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "FeedDiscoveryFragment";
    private static final int NUM_SUGGESTIONS = 12;
    public static final int NUM_LOADING_SUGGESTIONS = 25;

    private Disposable disposable;
    private FeedDiscoverAdapter adapter;
    private GridView discoverGridLayout;
    private TextView errorTextView;
    private TextView poweredByTextView;
    private LinearLayout errorView;
    private Button errorRetry;
    private List<Feed> subscribedFeeds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.quick_feed_discovery, container, false);
        View discoverMore = root.findViewById(R.id.discover_more);
        discoverMore.setOnClickListener(v ->
                ((MainActivity) getActivity()).loadChildFragment(new DiscoveryFragment()));

        discoverGridLayout = root.findViewById(R.id.discover_grid);
        errorView = root.findViewById(R.id.discover_error);
        errorTextView = root.findViewById(R.id.discover_error_txtV);
        errorRetry = root.findViewById(R.id.discover_error_retry_btn);
        poweredByTextView = root.findViewById(R.id.discover_powered_by_itunes);

        adapter = new FeedDiscoverAdapter((MainActivity) getActivity());
        discoverGridLayout.setAdapter(adapter);
        discoverGridLayout.setOnItemClickListener(this);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        if (screenWidthDp > 600) {
            discoverGridLayout.setNumColumns(6);
        } else {
            discoverGridLayout.setNumColumns(4);
        }

        // Fill with dummy elements to have a fixed height and
        // prevent the UI elements below from jumping on slow connections
        List<PodcastSearchResult> dummies = new ArrayList<>();
        for (int i = 0; i < NUM_SUGGESTIONS; i++) {
            dummies.add(PodcastSearchResult.dummy());
        }

        adapter.updateData(dummies);
        subscribedFeeds = new ArrayList<>();

        EventBus.getDefault().register(this);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void onDiscoveryDefaultUpdateEvent(DiscoveryDefaultUpdateEvent event) {
        loadToplist();
    }

    private void loadUserSubscriptions() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(DBReader::getFeedList)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    result -> {
                                        subscribedFeeds = result;
                                        loadToplist();
                                    }, error -> {
                                        Log.e(TAG, Log.getStackTraceString(error));
                                        handleLoadDataErrors(error, (listener) -> loadUserSubscriptions());
                                    });
    }

    private void loadToplist() {
        errorView.setVisibility(View.GONE);
        errorRetry.setVisibility(View.INVISIBLE);
        errorRetry.setText(R.string.retry_label);
        poweredByTextView.setVisibility(View.VISIBLE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        SharedPreferences prefs = getActivity().getSharedPreferences(ItunesTopListLoader.PREFS, MODE_PRIVATE);
        String countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE,
                Locale.getDefault().getCountry());
        if (prefs.getBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, false)) {
            errorTextView.setText(R.string.discover_is_hidden);
            errorView.setVisibility(View.VISIBLE);
            discoverGridLayout.setVisibility(View.GONE);
            errorRetry.setVisibility(View.GONE);
            poweredByTextView.setVisibility(View.GONE);
            return;
        }
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("free") && prefs.getBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, true)) {
            errorTextView.setText("");
            errorView.setVisibility(View.VISIBLE);
            discoverGridLayout.setVisibility(View.VISIBLE);
            errorRetry.setVisibility(View.VISIBLE);
            errorRetry.setText(R.string.discover_confirm);
            poweredByTextView.setVisibility(View.VISIBLE);
            errorRetry.setOnClickListener(v -> {
                prefs.edit().putBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, false).apply();
                loadToplist();
            });
            return;
        }

        disposable = loader.loadToplist(countryCode, NUM_LOADING_SUGGESTIONS)
                .subscribe(
                        podcasts -> {
                            errorView.setVisibility(View.GONE);
                            if (podcasts.size() == 0) {
                                errorTextView.setText(getResources().getText(R.string.search_status_no_results));
                                errorView.setVisibility(View.VISIBLE);
                                discoverGridLayout.setVisibility(View.INVISIBLE);
                            } else {
                                discoverGridLayout.setVisibility(View.VISIBLE);
                                adapter.updateData(
                                        removeSubscribedFromSuggested(podcasts, subscribedFeeds, NUM_SUGGESTIONS));
                            }
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            handleLoadDataErrors(error, (listener) -> loadToplist());
                        });
    }

    private void handleLoadDataErrors(Throwable error, View.OnClickListener listener) {
        errorTextView.setText(error.getLocalizedMessage());
        errorView.setVisibility(View.VISIBLE);
        discoverGridLayout.setVisibility(View.INVISIBLE);
        errorRetry.setVisibility(View.VISIBLE);
        errorRetry.setOnClickListener(listener);
    }

    public static List<PodcastSearchResult> removeSubscribedFromSuggested(
            List<PodcastSearchResult> suggestedPodcasts, List<Feed> subscribedFeeds, int limit) {
        Set<String> subscribedPodcastsSet = new HashSet<>();
        for (Feed subscribedFeed : subscribedFeeds) {
            String subscribedTitle = subscribedFeed.getTitle().trim() + " - "
                    + subscribedFeed.getAuthor().trim();
            subscribedPodcastsSet.add(subscribedTitle);
        }
        List<PodcastSearchResult> suggestedNotSubscribed = new ArrayList<>();
        int elementsAdded = 0;

        for (PodcastSearchResult suggested : suggestedPodcasts) {
            if (!isSuggestedSubscribed(suggested, subscribedPodcastsSet)) {
                suggestedNotSubscribed.add(suggested);
                elementsAdded++;
                if (elementsAdded == limit) {
                    return suggestedNotSubscribed;
                }
            }
        }
        return suggestedNotSubscribed;
    }

    private static boolean isSuggestedSubscribed(
            PodcastSearchResult suggested, Set<String> subscribedPodcastsSet) {
        return subscribedPodcastsSet.contains(suggested.title.trim());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        PodcastSearchResult podcast = adapter.getItem(position);
        if (TextUtils.isEmpty(podcast.feedUrl)) {
            return;
        }
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserSubscriptions();
    }
}
