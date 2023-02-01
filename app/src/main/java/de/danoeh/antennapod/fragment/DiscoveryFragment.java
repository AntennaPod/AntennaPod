package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.adapter.itunes.ItunesAdapter;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.event.DiscoveryDefaultUpdateEvent;
import de.danoeh.antennapod.net.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Searches iTunes store for top podcasts and displays results in a list.
 */
public class DiscoveryFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    private static final String TAG = "ItunesSearchFragment";
    private static final int NUM_OF_TOP_PODCASTS = 25;
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
    private boolean hidden;
    private boolean needsConfirm;
    private MaterialToolbar toolbar;

    public DiscoveryFragment() {
        // Required empty public constructor
    }

    /**
     * Replace adapter data with provided search results from SearchTask.
     *
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getActivity().getSharedPreferences(ItunesTopListLoader.PREFS, Context.MODE_PRIVATE);
        countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, Locale.getDefault().getCountry());
        hidden = prefs.getBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, false);
        needsConfirm = prefs.getBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_itunes_search, container, false);
        gridView = root.findViewById(R.id.gridView);
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>());
        gridView.setAdapter(adapter);

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.inflateMenu(R.menu.countries_menu);
        MenuItem discoverHideItem = toolbar.getMenu().findItem(R.id.discover_hide_item);
        discoverHideItem.setChecked(hidden);
        toolbar.setOnMenuItemClickListener(this);

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

        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        loadToplist(countryCode);
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
        butRetry.setText(R.string.retry_label);
        txtvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        if (hidden) {
            gridView.setVisibility(View.GONE);
            txtvError.setVisibility(View.VISIBLE);
            txtvError.setText(getResources().getString(R.string.discover_is_hidden));
            butRetry.setVisibility(View.GONE);
            txtvEmpty.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            return;
        }
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("free") && needsConfirm) {
            txtvError.setVisibility(View.VISIBLE);
            txtvError.setText("");
            butRetry.setVisibility(View.VISIBLE);
            butRetry.setText(R.string.discover_confirm);
            butRetry.setOnClickListener(v -> {
                prefs.edit().putBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, false).apply();
                needsConfirm = false;
                loadToplist(country);
            });
            txtvEmpty.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        disposable = Observable.fromCallable(() ->
                        loader.loadToplist(country, NUM_OF_TOP_PODCASTS, DBReader.getFeedList()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    podcasts -> {
                        progressBar.setVisibility(View.GONE);
                        topList = podcasts;
                        updateData(topList);
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                        progressBar.setVisibility(View.GONE);
                        txtvError.setText(error.getMessage());
                        txtvError.setVisibility(View.VISIBLE);
                        butRetry.setOnClickListener(v -> loadToplist(country));
                        butRetry.setVisibility(View.VISIBLE);
                    });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        final int itemId = item.getItemId();
        if (itemId == R.id.discover_hide_item) {
            item.setChecked(!item.isChecked());
            hidden = item.isChecked();
            prefs.edit().putBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, hidden).apply();

            EventBus.getDefault().post(new DiscoveryDefaultUpdateEvent());
            loadToplist(countryCode);
            return true;
        } else if (itemId == R.id.discover_countries_item) {

            LayoutInflater inflater = getLayoutInflater();
            View selectCountryDialogView = inflater.inflate(R.layout.select_country_dialog, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
            builder.setView(selectCountryDialogView);

            List<String> countryCodeArray = new ArrayList<>(Arrays.asList(Locale.getISOCountries()));
            Map<String, String> countryCodeNames = new HashMap<>();
            Map<String, String> countryNameCodes = new HashMap<>();
            for (String code : countryCodeArray) {
                Locale locale = new Locale("", code);
                String countryName = locale.getDisplayCountry();
                countryCodeNames.put(code, countryName);
                countryNameCodes.put(countryName, code);
            }

            List<String> countryNamesSort = new ArrayList<>(countryCodeNames.values());
            Collections.sort(countryNamesSort);

            ArrayAdapter<String> dataAdapter =
                    new ArrayAdapter<>(this.getContext(), android.R.layout.simple_list_item_1, countryNamesSort);
            TextInputLayout textInput = selectCountryDialogView.findViewById(R.id.country_text_input);
            MaterialAutoCompleteTextView editText = (MaterialAutoCompleteTextView) textInput.getEditText();
            editText.setAdapter(dataAdapter);
            editText.setText(countryCodeNames.get(countryCode));
            editText.setOnClickListener(view -> {
                if (editText.getText().length() != 0) {
                    editText.setText("");
                    editText.postDelayed(editText::showDropDown, 100);
                }
            });
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    editText.setText("");
                    editText.postDelayed(editText::showDropDown, 100);
                }
            });

            builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                String countryName = editText.getText().toString();
                if (countryNameCodes.containsKey(countryName)) {
                    countryCode = countryNameCodes.get(countryName);
                    MenuItem discoverHideItem = toolbar.getMenu().findItem(R.id.discover_hide_item);
                    discoverHideItem.setChecked(false);
                    hidden = false;
                }

                prefs.edit().putBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, hidden).apply();
                prefs.edit().putString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE, countryCode).apply();

                EventBus.getDefault().post(new DiscoveryDefaultUpdateEvent());
                loadToplist(countryCode);
            });
            builder.setNegativeButton(R.string.cancel_label, null);
            builder.show();
            return true;
        }
        return false;
    }
}
