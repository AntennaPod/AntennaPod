package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static de.danoeh.antennapod.adapter.itunes.ItunesAdapter.Podcast;

//Searches iTunes store for given string and displays results in a list
public class ItunesSearchFragment extends Fragment {

    private static final String TAG = "ItunesSearchFragment";

    private static final String API_URL = "https://itunes.apple.com/search?media=podcast&term=%s";

    /**
     *  Search input field
     */
    private SearchView searchView;

    /**
     * Adapter responsible with the search results
     */
    private ItunesAdapter adapter;

    /**
     * List of podcasts retreived from the search
     */
    private List<Podcast> searchResults;

    private Subscription subscription;

    /**
     * Replace adapter data with provided search results from SearchTask.
     * @param result List of Podcast objects containing search results
     */
    void updateData(List<Podcast> result) {
        this.searchResults = result;
        adapter.clear();

        //ArrayAdapter.addAll() requires minsdk > 10
        for(Podcast p: result) {
            adapter.add(p);
        }

        adapter.notifyDataSetInvalidated();
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
        adapter = new ItunesAdapter(getActivity(), new ArrayList<Podcast>());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_itunes_search, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.gridView);
        gridView.setAdapter(adapter);

        //Show information about the podcast when the list item is clicked
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(),
                        OnlineFeedViewActivity.class);

                //Tell the OnlineFeedViewActivity where to go
                String url = searchResults.get(position).feedUrl;
                intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, url);

                intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, "iTunes");
                startActivity(intent);
            }
        });

        //Configure search input view to be expanded by default with a visible submit button
        searchView = (SearchView) view.findViewById(R.id.itunes_search_view);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //This prevents onQueryTextSubmit() from being called twice when keyboard is used
                //to submit the query.
                searchView.clearFocus();
                search(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        SearchView.SearchAutoComplete textField = (SearchView.SearchAutoComplete) searchView.findViewById(de.danoeh.antennapod.R.id.search_src_text);
        if(UserPreferences.getTheme() == de.danoeh.antennapod.R.style.Theme_AntennaPod_Dark) {
            textField.setTextColor(Resources.getSystem().getColor(android.R.color.white));
        } else {
            textField.setTextColor(Resources.getSystem().getColor(android.R.color.black));
        }

        return view;
    }

    private void search(String query) {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        subscription = rx.Observable.create((Observable.OnSubscribe<List<Podcast>>) subscriber -> {
                    String encodedQuery = null;
                    try {
                        encodedQuery = URLEncoder.encode(query, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // this won't ever be thrown
                    }
                    if (encodedQuery == null) {
                        encodedQuery = query; // failsafe
                    }

                    //Spaces in the query need to be replaced with '+' character.
                    String formattedUrl = String.format(API_URL, query).replace(' ', '+');

                    OkHttpClient client = AntennapodHttpClient.getHttpClient();
                    Request.Builder httpReq = new Request.Builder()
                            .url(formattedUrl)
                            .header("User-Agent", ClientConfig.USER_AGENT);
                    List<Podcast> podcasts = new ArrayList<>();
                    try {
                        Response response = client.newCall(httpReq.build()).execute();

                        if(response.isSuccessful()) {
                            String resultString = response.body().string();
                            JSONObject result = new JSONObject(resultString);
                            JSONArray j = result.getJSONArray("results");

                            for (int i = 0; i < j.length(); i++) {
                                JSONObject podcastJson = j.getJSONObject(i);
                                Podcast podcast = new Podcast(podcastJson);
                                podcasts.add(podcast);
                            }
                        }
                        else {
                            subscriber.onError(new IOException("Unexpected error: " + response));
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    subscriber.onNext(podcasts);
                    subscriber.onCompleted();
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(podcasts -> {
                    updateData(podcasts);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                });
    }

}
