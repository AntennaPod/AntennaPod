package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.discovery.FyydPodcastSearcher;
import de.danoeh.antennapod.discovery.GpodnetPodcastSearcher;
import de.danoeh.antennapod.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.discovery.PodcastSearchResult;
import de.danoeh.antennapod.discovery.PodcastSearcher;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class CombinedSearchFragment extends Fragment {

    private static final String TAG = "CombinedSearchFragment";

    /**
     * Adapter responsible with the search results
     */
    private ItunesAdapter adapter;
    private GridView gridView;
    private ProgressBar progressBar;
    private TextView txtvError;
    private Button butRetry;
    private TextView txtvEmpty;

    /**
     * List of podcasts retreived from the search
     */
    private List<PodcastSearchResult> searchResults = new ArrayList<>();
    private List<Disposable> disposables = new ArrayList<>();

    /**
     * Constructor
     */
    public CombinedSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_itunes_search, container, false);
        gridView = root.findViewById(R.id.gridView);
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>());
        gridView.setAdapter(adapter);

        //Show information about the podcast when the list item is clicked
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            PodcastSearchResult podcast = searchResults.get(position);
            Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
            intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
            intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, podcast.title);
            startActivity(intent);
        });
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposeAll();
        adapter = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.itunes_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        MenuItemUtils.adjustTextColor(getActivity(), sv);
        sv.setQueryHint(getString(R.string.search_label));
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
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
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
        MenuItemCompat.expandActionView(searchItem);
    }

    private void search(String query) {
        disposeAll();

        showOnlyProgressBar();

        List<PodcastSearcher> searchProviders = new ArrayList<>();
        searchProviders.add(new FyydPodcastSearcher(query));
        searchProviders.add(new ItunesPodcastSearcher(getContext(), query));
        searchProviders.add(new GpodnetPodcastSearcher(query));

        List<List<PodcastSearchResult>> singleResults = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(searchProviders.size());
        for (PodcastSearcher searchProvider : searchProviders) {
            disposables.add(searchProvider.search(e -> {
                    singleResults.add(e);
                    latch.countDown();
                }, throwable -> {
                    Toast.makeText(getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    latch.countDown();
                }
            ));
        }

        disposables.add(Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) subscriber -> {
            latch.await();

            HashMap<String, Float> resultRanking = new HashMap<>();
            HashMap<String, PodcastSearchResult> urlToResult = new HashMap<>();
            for (List<PodcastSearchResult> providerResults : singleResults) {
                for (int position = 0; position < providerResults.size(); position++) {
                    PodcastSearchResult result = providerResults.get(position);
                    urlToResult.put(result.feedUrl, result);

                    float ranking = 0;
                    if (resultRanking.containsKey(result.feedUrl)) {
                        ranking = resultRanking.get(result.feedUrl);
                    }
                    ranking += 1.f / (position + 1.f);
                    resultRanking.put(result.feedUrl, ranking);
                }
            }
            List<Map.Entry<String, Float>> sortedResults = new ArrayList<>(resultRanking.entrySet());
            Collections.sort(sortedResults, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));

            List<PodcastSearchResult> results = new ArrayList<>();
            for (Map.Entry<String, Float> res : sortedResults) {
                results.add(urlToResult.get(res.getKey()));
            }
            subscriber.onSuccess(results);
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                searchResults = result;
                progressBar.setVisibility(View.GONE);

                adapter.clear();
                adapter.addAll(searchResults);
                adapter.notifyDataSetInvalidated();
                gridView.setVisibility(!searchResults.isEmpty() ? View.VISIBLE : View.GONE);
                txtvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);

            }, error -> {
                Log.e(TAG, Log.getStackTraceString(error));
                progressBar.setVisibility(View.GONE);
                txtvError.setText(error.toString());
                txtvError.setVisibility(View.VISIBLE);
                butRetry.setOnClickListener(v -> search(query));
                butRetry.setVisibility(View.VISIBLE);
            }));
    }

    private void disposeAll() {
        for (Disposable d : disposables) {
            d.dispose();
        }
        disposables.clear();
    }

    private void showOnlyProgressBar() {
        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return (Comparator<Map.Entry<K, V>> & Serializable)
                (c1, c2) -> c1.getValue().compareTo(c2.getValue());
    }
}
