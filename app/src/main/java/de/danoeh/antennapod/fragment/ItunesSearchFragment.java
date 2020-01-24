package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.ArrayRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.danoeh.antennapod.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.discovery.PodcastSearchResult;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import io.reactivex.disposables.Disposable;

//Searches iTunes store for given string and displays results in a list
public class ItunesSearchFragment extends Fragment implements AdapterView.OnItemSelectedListener {

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

        AppCompatSpinner genre_spinner = root.findViewById(R.id.spinner_genre);
        genre_spinner.setOnItemSelectedListener(this);


        //Show information about the podcast when the list item is clicked
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            PodcastSearchResult podcast = searchResults.get(position);
            if (podcast.feedUrl == null) {
                return;
            }
            gridView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
            disposable = loader.getFeedUrl(podcast)
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
                        new AlertDialog.Builder(getActivity())
                                .setMessage(prefix + " " + error.getMessage())
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    });
        });
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        String genre = "1406";
        loadToplist(genre);

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
        sv.setQueryHint(getString(R.string.search_itunes_label));
        sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
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

    private void loadToplist(String genre) {
        if (disposable != null) {
            disposable.dispose();
        }
        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = loader.loadToplist(100, genre)
                .subscribe(podcasts -> {
                    progressBar.setVisibility(View.GONE);
                    topList = podcasts;
                    updateData(topList);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    progressBar.setVisibility(View.GONE);
                    txtvError.setText(error.toString());
                    txtvError.setVisibility(View.VISIBLE);
                    butRetry.setOnClickListener(v -> loadToplist(genre));
                    butRetry.setVisibility(View.VISIBLE);
                });
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

        ItunesPodcastSearcher searcher = new ItunesPodcastSearcher(getContext());
        disposable = searcher.search(query).subscribe(podcasts -> {
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {

        String genre = (String) adapterView.getItemAtPosition(pos);
        String[] genre_code_array = getResources().getStringArray(R.array.itunes_genres_code);
        String genre_code = (pos < genre_code_array.length) ? genre_code_array[pos] : "";

        Log.d(TAG, "Genre spinner selected position " +
                pos + " " +
                genre +  " " +
                genre_code);
        loadToplist(genre_code);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Log.d(TAG, "Nothing Selected in Genre spinner");
    }
}

