package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DefaultOnlineFeedViewActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import static de.danoeh.antennapod.adapter.itunes.ItunesAdapter.Podcast;

//Searches iTunes store for given string and displays results in a list
public class ItunesSearchFragment extends Fragment {
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
                new SearchTask(s).execute();
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

    /**
     * Search the iTunes store for podcasts using the given query
     */
    class SearchTask extends AsyncTask<Void,Void,Void> {
        /**
         * Incomplete iTunes API search URL
         */
        final String apiUrl = "https://itunes.apple.com/search?media=podcast&term=%s";

        /**
         * Search terms
         */
        final String query;

        /**
         * Search result
         */
        final List<Podcast> taskData = new ArrayList<>();

        /**
         * Constructor
         *
         * @param query Search string
         */
        public SearchTask(String query) {
            String encodedQuery = null;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch(UnsupportedEncodingException e) {
                // this won't ever be thrown
            }
            if(encodedQuery != null) {
                this.query = encodedQuery;
            } else {
                this.query = query; // failsafe
            }
        }

        //Get the podcast data
        @Override
        protected Void doInBackground(Void... params) {

            //Spaces in the query need to be replaced with '+' character.
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
            return null;
        }

        //Save the data and update the list
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateData(taskData);
        }
    }
}
