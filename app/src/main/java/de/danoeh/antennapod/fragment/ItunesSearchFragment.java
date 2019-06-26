package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.annotation.NonNull;
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

import com.afollestad.materialdialogs.MaterialDialog;

import de.danoeh.antennapod.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.discovery.PodcastSearchResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//Searches iTunes store for given string and displays results in a list
public class ItunesSearchFragment extends Fragment {

    private static final String TAG = "ItunesSearchFragment";


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
    private List<PodcastSearchResult> searchResults;
    private List<PodcastSearchResult> topList;
    private Disposable disposable;

    /**
     * Replace adapter data with provided search results from SearchTask.
     * @param result List of Podcast objects containing search results
     */
    private void updateData(List<PodcastSearchResult> result) {
        this.searchResults = result;
        adapter.clear();
        if (result != null && result.size() > 0) {
            gridView.setVisibility(View.VISIBLE);
            txtvEmpty.setVisibility(View.GONE);
            for (PodcastSearchResult p : result) {
                adapter.add(p);
            }
            adapter.notifyDataSetInvalidated();
        } else {
            gridView.setVisibility(View.GONE);
            txtvEmpty.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Constructor
     */
    public ItunesSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_itunes_search, container, false);
        gridView = root.findViewById(R.id.gridView);
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>());
        gridView.setAdapter(adapter);

        //Show information about the podcast when the list item is clicked
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            PodcastSearchResult podcast = searchResults.get(position);
            if(podcast.feedUrl == null) {
                return;
            }
            if (!podcast.feedUrl.contains("itunes.apple.com")) {
                Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
                intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
                intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, "iTunes");
                startActivity(intent);
            } else {
                gridView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                disposable = Single.create((SingleOnSubscribe<String>) emitter -> {
                            OkHttpClient client = AntennapodHttpClient.getHttpClient();
                            Request.Builder httpReq = new Request.Builder()
                                    .url(podcast.feedUrl)
                                    .header("User-Agent", ClientConfig.USER_AGENT);
                            try {
                                Response response = client.newCall(httpReq.build()).execute();
                                if (response.isSuccessful()) {
                                    String resultString = response.body().string();
                                    JSONObject result = new JSONObject(resultString);
                                    JSONObject results = result.getJSONArray("results").getJSONObject(0);
                                    String feedUrl = results.getString("feedUrl");
                                    emitter.onSuccess(feedUrl);
                                } else {
                                    String prefix = getString(R.string.error_msg_prefix);
                                    emitter.onError(new IOException(prefix + response));
                                }
                            } catch (IOException | JSONException e) {
                                if (!disposable.isDisposed()) {
                                    emitter.onError(e);
                                }
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(feedUrl -> {
                            progressBar.setVisibility(View.GONE);
                            gridView.setVisibility(View.VISIBLE);
                            Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
                            intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, feedUrl);
                            intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, "iTunes");
                            startActivity(intent);
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            progressBar.setVisibility(View.GONE);
                            gridView.setVisibility(View.VISIBLE);
                            String prefix = getString(R.string.error_msg_prefix);
                            new MaterialDialog.Builder(getActivity())
                                    .content(prefix + " " + error.getMessage())
                                    .neutralText(android.R.string.ok)
                                    .show();
                        });
            }
        });
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        loadToplist();

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
        adapter = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.itunes_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        MenuItemUtils.adjustTextColor(getActivity(), sv);
        sv.setQueryHint(getString(R.string.search_itunes_label));
        sv.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
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
                if(searchResults != null) {
                    searchResults = null;
                    updateData(topList);
                }
                return true;
            }
        });
    }

    private void loadToplist() {
        if (disposable != null) {
            disposable.dispose();
        }
        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        disposable = Single.create((SingleOnSubscribe<List<PodcastSearchResult>>) emitter -> {
                    String lang = Locale.getDefault().getLanguage();
                    OkHttpClient client = AntennapodHttpClient.getHttpClient();
                    String feedString;
                    try {
                        try {
                            feedString = getTopListFeed(client, lang);
                        } catch (IOException e) {
                            feedString = getTopListFeed(client, "us");
                        }
                        List<PodcastSearchResult> podcasts = parseFeed(feedString);
                        emitter.onSuccess(podcasts);
                    } catch (IOException | JSONException e) {
                        if (!disposable.isDisposed()) {
                            emitter.onError(e);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(podcasts -> {
                    progressBar.setVisibility(View.GONE);
                    topList = podcasts;
                    updateData(topList);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    progressBar.setVisibility(View.GONE);
                    txtvError.setText(error.toString());
                    txtvError.setVisibility(View.VISIBLE);
                    butRetry.setOnClickListener(v -> loadToplist());
                    butRetry.setVisibility(View.VISIBLE);
                });
    }

    private String getTopListFeed(OkHttpClient client, String language) throws IOException {
        String url = "https://itunes.apple.com/%s/rss/toppodcasts/limit=25/explicit=true/json";
        Request.Builder httpReq = new Request.Builder()
                .header("User-Agent", ClientConfig.USER_AGENT)
                .url(String.format(url, language));

        try (Response response = client.newCall(httpReq.build()).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            }
            String prefix = getString(R.string.error_msg_prefix);
            throw new IOException(prefix + response);
        }
    }

    private List<PodcastSearchResult> parseFeed(String jsonString) throws JSONException {
        JSONObject result = new JSONObject(jsonString);
        JSONObject feed = result.getJSONObject("feed");
        JSONArray entries = feed.getJSONArray("entry");

        List<PodcastSearchResult> results = new ArrayList<>();
        for (int i=0; i < entries.length(); i++) {
            JSONObject json = entries.getJSONObject(i);
            results.add(PodcastSearchResult.fromItunesToplist(json));
        }

        return results;
    }

    private void search(String query) {
        if (disposable != null) {
            disposable.dispose();
        }
        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        ItunesPodcastSearcher searcher = new ItunesPodcastSearcher(getContext(), query);
        disposable = searcher.search(podcasts -> {
            progressBar.setVisibility(View.GONE);
            updateData(podcasts);
        }, error -> {
            Log.e(TAG, Log.getStackTraceString(error));
            progressBar.setVisibility(View.GONE);
            txtvError.setText(error.toString());
            txtvError.setVisibility(View.VISIBLE);
            butRetry.setOnClickListener(v -> search(query));
            butRetry.setVisibility(View.VISIBLE);
        });
    }

}
