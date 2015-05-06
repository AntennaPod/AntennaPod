package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DefaultOnlineFeedViewActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter.Podcast;

import static de.danoeh.antennapod.adapter.itunes.ItunesAdapter.*;

//Searches iTunes store for given string and displays results in a list
public class ItunesSearchFragment extends Fragment {
    private static final String SEARCH_FILTER = "ItunesSearchFragment_searchReceiver";
    final String TAG = "ItunesSearchFragment";
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
    private SearchReceiver searchReceiver;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        searchReceiver = new SearchReceiver();
        LocalBroadcastManager.getInstance(activity)
            .registerReceiver(searchReceiver, new IntentFilter(SEARCH_FILTER));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (searchReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(searchReceiver);
        }
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
                        DefaultOnlineFeedViewActivity.class);

                //Tell the OnlineFeedViewActivity where to go
                String url = searchResults.get(position).feedUrl;
                intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, url);

                intent.putExtra(DefaultOnlineFeedViewActivity.ARG_TITLE, "iTunes");
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
                Intent search = new Intent(getActivity(), SearchService.class);
                search.putExtra("query", s);
                getActivity().startService(search);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return view;
    }

    /**
     * Search the iTunes store for podcasts using the given query
     */
    class SearchReceiver extends BroadcastReceiver {

        //Save the data and update the list
        @Override
        public void onReceive(Context receiverContext, Intent receiverIntent) {
            ArrayList<Podcast> taskData = (ArrayList<Podcast>) receiverIntent.getSerializableExtra("taskData");
            updateData(taskData);
        }
    }

    public static class SearchService extends IntentService {
        /**
         * Incomplete iTunes API search URL
         */
        final String apiUrl = "https://itunes.apple.com/search?media=podcast&term=%s";

        /**
         * Search terms
         */
        private String query;

        /**
         * Search result
         */
        final ArrayList<Podcast> taskData = new ArrayList<>();

        public SearchService() {
            super("SearchService");
        }

        public void onHandleIntent(Intent intent) {
            query = intent.getStringExtra("query");
            String formattedUrl = String.format(apiUrl, query).replace(' ', '+');
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(formattedUrl);
            try {
                HttpResponse response = client.execute(get);
                String resultString = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(resultString);
                JSONArray j = result.getJSONArray("results");

                for (int i = 0; i < j.length(); i++){
                    JSONObject podcastJson = j.getJSONObject(i);
                    Podcast podcast = new Podcast(podcastJson);
                    taskData.add(podcast);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            Intent resultIntent = new Intent(SEARCH_FILTER);
            resultIntent.putExtra("taskData", taskData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
        }
    }
}
