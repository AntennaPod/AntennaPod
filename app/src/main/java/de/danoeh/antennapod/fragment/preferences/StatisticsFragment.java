package de.danoeh.antennapod.fragment.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.adapter.StatisticsListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the 'statistics' screen
 */
public class StatisticsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = StatisticsFragment.class.getSimpleName();
    private static final String PREF_NAME = "StatisticsActivityPrefs";
    private static final String PREF_COUNT_ALL = "countAll";

    private Disposable disposable;
    private TextView totalTimeTextView;
    private ListView feedStatisticsList;
    private ProgressBar progressBar;
    private StatisticsListAdapter listAdapter;
    private boolean countAll = false;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        countAll = prefs.getBoolean(PREF_COUNT_ALL, false);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.statistics_activity, container, false);
        totalTimeTextView = root.findViewById(R.id.total_time);
        feedStatisticsList = root.findViewById(R.id.statistics_list);
        progressBar = root.findViewById(R.id.progressBar);
        listAdapter = new StatisticsListAdapter(getContext());
        listAdapter.setCountAll(countAll);
        feedStatisticsList.setAdapter(listAdapter);
        feedStatisticsList.setOnItemClickListener(this);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.statistics_label);
        refreshStatistics();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.statistics, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.statistics_mode) {
            selectStatisticsMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectStatisticsMode() {
        View contentView = View.inflate(getContext(), R.layout.statistics_mode_select_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(contentView);
        builder.setTitle(R.string.statistics_mode);

        if (countAll) {
            ((RadioButton) contentView.findViewById(R.id.statistics_mode_count_all)).setChecked(true);
        } else {
            ((RadioButton) contentView.findViewById(R.id.statistics_mode_normal)).setChecked(true);
        }

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            countAll = ((RadioButton) contentView.findViewById(R.id.statistics_mode_count_all)).isChecked();
            listAdapter.setCountAll(countAll);
            prefs.edit().putBoolean(PREF_COUNT_ALL, countAll).apply();
            refreshStatistics();
        });

        builder.show();
    }

    private void refreshStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        totalTimeTextView.setVisibility(View.GONE);
        feedStatisticsList.setVisibility(View.GONE);
        loadStatistics();
    }

    private void loadStatistics() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getStatistics(countAll))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    totalTimeTextView.setText(Converter.shortLocalizedDuration(getContext(),
                            countAll ? result.totalTimeCountAll : result.totalTime));
                    listAdapter.update(result.feedTime);
                    progressBar.setVisibility(View.GONE);
                    totalTimeTextView.setVisibility(View.VISIBLE);
                    feedStatisticsList.setVisibility(View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DBReader.StatisticsItem stats = listAdapter.getItem(position);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(stats.feed.getTitle());
        dialog.setMessage(getString(R.string.statistics_details_dialog,
                countAll ? stats.episodesStartedIncludingMarked : stats.episodesStarted,
                stats.episodes, Converter.shortLocalizedDuration(getContext(),
                        countAll ? stats.timePlayedCountAll : stats.timePlayed),
                Converter.shortLocalizedDuration(getContext(), stats.time)));
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }
}
