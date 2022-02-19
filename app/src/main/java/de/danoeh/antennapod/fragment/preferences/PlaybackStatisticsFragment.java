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
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

/**
 * Displays the 'playback statistics' screen
 */
public class PlaybackStatisticsFragment extends Fragment {
    private static final String TAG = PlaybackStatisticsFragment.class.getSimpleName();
    private static final String PREF_NAME = "StatisticsActivityPrefs";
    private static final String PREF_INCLUDE_MARKED_PLAYED = "countAll";

    private Disposable disposable;
    private RecyclerView feedStatisticsList;
    private ProgressBar progressBar;
    private PlaybackStatisticsListAdapter listAdapter;
    private boolean includeMarkedAsPlayed = false;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        includeMarkedAsPlayed = prefs.getBoolean(PREF_INCLUDE_MARKED_PLAYED, false);
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
        dialogBinding.statisticsIncludeMarked.setChecked(includeMarkedAsPlayed);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            includeMarkedAsPlayed = dialogBinding.statisticsIncludeMarked.isChecked();
            prefs.edit().putBoolean(PREF_INCLUDE_MARKED_PLAYED, includeMarkedAsPlayed).apply();
            refreshStatistics();
        });
        builder.show();
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
        disposable = Observable.fromCallable(() -> DBReader.getStatistics(includeMarkedAsPlayed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    listAdapter.update(result);
                    progressBar.setVisibility(View.GONE);
                    feedStatisticsList.setVisibility(View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
