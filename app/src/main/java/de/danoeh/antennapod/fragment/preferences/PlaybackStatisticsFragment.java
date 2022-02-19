package de.danoeh.antennapod.fragment.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.PlaybackStatisticsListAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.databinding.StatisticsFilterDialogBinding;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Displays the 'playback statistics' screen
 */
public class PlaybackStatisticsFragment extends Fragment {
    private static final String TAG = PlaybackStatisticsFragment.class.getSimpleName();
    private static final String PREF_NAME = "StatisticsActivityPrefs";
    private static final String PREF_INCLUDE_MARKED_PLAYED = "countAll";
    private static final String PREF_FILTER_FROM = "filterFrom";
    private static final String PREF_FILTER_TO = "filterTo";

    private Disposable disposable;
    private RecyclerView feedStatisticsList;
    private ProgressBar progressBar;
    private PlaybackStatisticsListAdapter listAdapter;
    private boolean includeMarkedAsPlayed = false;
    private long timeFilterFrom = 0;
    private long timeFilterTo = Long.MAX_VALUE;
    private SharedPreferences prefs;
    private DBReader.StatisticsResult statisticsResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        includeMarkedAsPlayed = prefs.getBoolean(PREF_INCLUDE_MARKED_PLAYED, false);
        timeFilterFrom = prefs.getLong(PREF_FILTER_FROM, 0);
        timeFilterTo = prefs.getLong(PREF_FILTER_TO, Long.MAX_VALUE);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.statistics_activity, container, false);
        feedStatisticsList = root.findViewById(R.id.statistics_list);
        progressBar = root.findViewById(R.id.progressBar);
        listAdapter = new PlaybackStatisticsListAdapter(this);
        feedStatisticsList.setLayoutManager(new LinearLayoutManager(getContext()));
        feedStatisticsList.setAdapter(listAdapter);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshStatistics();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.statistics_reset).setVisible(true);
        menu.findItem(R.id.statistics_filter).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.statistics_filter) {
            selectStatisticsFilter();
            return true;
        } else if (item.getItemId() == R.id.statistics_reset) {
            confirmResetStatistics();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectStatisticsFilter() {
        StatisticsFilterDialogBinding dialogBinding = StatisticsFilterDialogBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogBinding.getRoot());
        builder.setTitle(R.string.filter);
        dialogBinding.includeMarkedCheckbox.setChecked(includeMarkedAsPlayed);

        Pair<String[], Long[]> filterDates = makeMonthlyList(statisticsResult.oldestDate);

        ArrayAdapter<String> adapterFrom = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, filterDates.first);
        adapterFrom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.timeFromSpinner.setAdapter(adapterFrom);
        for (int i = 0; i < filterDates.second.length; i++) {
            if (filterDates.second[i] >= timeFilterFrom) {
                dialogBinding.timeFromSpinner.setSelection(i);
                break;
            }
        }

        ArrayAdapter<String> adapterTo = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, filterDates.first);
        adapterTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.timeToSpinner.setAdapter(adapterTo);
        for (int i = 0; i < filterDates.second.length; i++) {
            if (filterDates.second[i] >= timeFilterTo) {
                dialogBinding.timeToSpinner.setSelection(i);
                break;
            }
        }

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            includeMarkedAsPlayed = dialogBinding.includeMarkedCheckbox.isChecked();
            timeFilterFrom = filterDates.second[dialogBinding.timeFromSpinner.getSelectedItemPosition()];
            timeFilterTo = filterDates.second[dialogBinding.timeToSpinner.getSelectedItemPosition()];
            prefs.edit()
                    .putBoolean(PREF_INCLUDE_MARKED_PLAYED, includeMarkedAsPlayed)
                    .putLong(PREF_FILTER_FROM, timeFilterFrom)
                    .putLong(PREF_FILTER_TO, timeFilterTo)
                    .apply();
            refreshStatistics();
        });
        builder.show();
    }

    private Pair<String[], Long[]> makeMonthlyList(long oldestDate) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(oldestDate);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Long> timestamps = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        while (date.getTimeInMillis() < System.currentTimeMillis()) {
            names.add(dateFormat.format(new Date(date.getTimeInMillis())));
            timestamps.add(date.getTimeInMillis());
            if (date.get(Calendar.MONTH) == Calendar.DECEMBER) {
                date.set(Calendar.MONTH, Calendar.JANUARY);
                date.set(Calendar.YEAR, date.get(Calendar.YEAR) + 1);
            } else {
                date.set(Calendar.MONTH, date.get(Calendar.MONTH) + 1);
            }
        }
        names.add(getString(R.string.statistics_today));
        timestamps.add(Long.MAX_VALUE);
        return new Pair<>(names.toArray(new String[0]), timestamps.toArray(new Long[0]));
    }

    private void confirmResetStatistics() {
        if (!includeMarkedAsPlayed) {
            ConfirmationDialog conDialog = new ConfirmationDialog(
                    getActivity(),
                    R.string.statistics_reset_data,
                    R.string.statistics_reset_data_msg) {

                @Override
                public void onConfirmButtonPressed(DialogInterface dialog) {
                    dialog.dismiss();
                    doResetStatistics();
                }
            };
            conDialog.createNewDialog().show();
        }
    }

    private void doResetStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        feedStatisticsList.setVisibility(View.GONE);
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = Completable.fromFuture(DBWriter.resetStatistics())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    refreshStatistics();
                    UserPreferences.resetUsageCountingDate();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void refreshStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        feedStatisticsList.setVisibility(View.GONE);
        loadStatistics();
    }

    private void loadStatistics() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
                () -> {
                    DBReader.StatisticsResult statisticsData = DBReader.getStatistics(
                            includeMarkedAsPlayed, timeFilterFrom, timeFilterTo);
                    Collections.sort(statisticsData.feedTime, (item1, item2) ->
                            Long.compare(item2.timePlayed, item1.timePlayed));
                    return statisticsData;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    statisticsResult = result;
                    // When "from" is "today", set it to today
                    listAdapter.setTimeFilter(Math.max(
                                Math.min(timeFilterFrom, System.currentTimeMillis()), result.oldestDate),
                            Math.min(timeFilterTo, System.currentTimeMillis()));
                    listAdapter.update(result.feedTime);
                    progressBar.setVisibility(View.GONE);
                    feedStatisticsList.setVisibility(View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
