package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.engine.Resource;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.core.event.DiscoveryDefaultUpdateEvent;
import de.danoeh.antennapod.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.discovery.PodcastSearchResult;
import io.reactivex.disposables.Disposable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.POWER_SERVICE;

/**
 * Searches iTunes store for top podcasts and displays results in a list.
 */
public class DiscoveryFragment extends Fragment {

    private static final String TAG = "ItunesSearchFragment";
    private SharedPreferences prefs;

    /**
     * Adapter responsible with the search results.
     */
    private ItunesAdapter adapter;
    private GridView gridView;
    private ProgressBar progressBar;
    private TextView txtvError;
    private Button butRetry;
    private TextView txtvEmpty;

    /**
     * List of podcasts retreived from the search.
     */
    private List<PodcastSearchResult> searchResults;
    private List<PodcastSearchResult> topList;
    private Disposable disposable;
    private String countryCode = "US";

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

    public DiscoveryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SharedPreferences prefs = getActivity().getSharedPreferences(ItunesTopListLoader.PREFS, MODE_PRIVATE);
        countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, Locale.getDefault().getCountry());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_itunes_search, container, false);
        ((AppCompatActivity) getActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));
        gridView = root.findViewById(R.id.gridView);
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>());
        gridView.setAdapter(adapter);

        //Show information about the podcast when the list item is clicked
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            PodcastSearchResult podcast = searchResults.get(position);
            if (podcast.feedUrl == null) {
                return;
            }
            Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
            intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, podcast.feedUrl);
            startActivity(intent);
        });

        List<String> countryCodeArray = new ArrayList<String>(Arrays.asList(Locale.getISOCountries()));
        HashMap<String, String> countryCodeNames = new HashMap<String, String>();
        for (String code: countryCodeArray) {
            Locale locale = new Locale("", code);
            String countryName = locale.getDisplayCountry();
            if (countryName != null) {
                countryCodeNames.put(code, countryName);
            }
        }
        //countryCodeArray.add(0, getResources().getString(R.string.discover_hide_fake_code));
        // countryCodeNames.put(getResources().getString(R.string.discover_hide_fake_code),
        //                 getResources().getString(R.string.discover_hide));

        List<String> countryNamesSort = new ArrayList<String>(countryCodeNames.values());
        Collections.sort(countryNamesSort);
        countryNamesSort.add(0, getResources().getString(R.string.discover_hide));

        Spinner countrySpinner = root.findViewById(R.id.spinner_country);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getContext(), 
                android.R.layout.simple_spinner_item, 
                countryNamesSort);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(dataAdapter);
        int pos = countryNamesSort.indexOf(countryCodeNames.get(countryCode));
        countrySpinner.setSelection(pos);

        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> countrySpinner, View view, int position, long id) {
                String countryName = (String) countrySpinner.getItemAtPosition(position);

                if (countryName.equals(getResources().getString(R.string.discover_hide))) {
                    countryCode = getResources().getString(R.string.discover_hide_fake_code);
                    gridView.setVisibility(View.GONE);
                    txtvError.setVisibility(View.VISIBLE);
                    txtvError.setText(String.format(getResources().getString(R.string.discover_will_be_hidden),
                            getResources().getString(R.string.discover_hide)));
                    butRetry.setVisibility(View.GONE);
                    txtvEmpty.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                } else {
                    for (Object o : countryCodeNames.keySet()) {
                        if (countryCodeNames.get(o).equals(countryName)) {
                            countryCode = o.toString();
                            break;
                        }
                    }
                }

                prefs = getActivity().getSharedPreferences(ItunesTopListLoader.PREFS, MODE_PRIVATE);
                prefs.edit()
                        .putString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, countryCode)
                        .apply();

                EventBus.getDefault().post(
                        new DiscoveryDefaultUpdateEvent()
                );
                if (countryCode.equals(getResources().getString(R.string.discover_hide_fake_code))) {
                     return;
                }
                loadToplist(countryCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        String fake_code = getResources().getString(R.string.discover_hide_fake_code);
        if (! countryCode.equals(fake_code)) {
            loadToplist(countryCode);
        }

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

    private void loadToplist(String country) {
        if (disposable != null) {
            disposable.dispose();
        }
        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = loader.loadToplist(country, 25).subscribe(podcasts -> {
            progressBar.setVisibility(View.GONE);
            topList = podcasts;
            updateData(topList);
        }, error -> {
                Log.e(TAG, Log.getStackTraceString(error));
                progressBar.setVisibility(View.GONE);
                txtvError.setText(error.toString());
                txtvError.setVisibility(View.VISIBLE);
                butRetry.setOnClickListener(v -> loadToplist(country));
                butRetry.setVisibility(View.VISIBLE);
            });
    }
}
