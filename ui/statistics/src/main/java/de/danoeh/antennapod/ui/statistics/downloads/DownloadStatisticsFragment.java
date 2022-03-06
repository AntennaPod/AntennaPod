package de.danoeh.antennapod.ui.statistics.downloads;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.ui.statistics.R;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.Collections;

/**
 * Displays the 'download statistics' screen
 */
public class DownloadStatisticsFragment extends Fragment {
    private static final String TAG = DownloadStatisticsFragment.class.getSimpleName();

    private Disposable disposable;
    private RecyclerView downloadStatisticsList;
    private ProgressBar progressBar;
    private DownloadStatisticsListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.statistics_fragment, container, false);
        downloadStatisticsList = root.findViewById(R.id.statistics_list);
        progressBar = root.findViewById(R.id.progressBar);
        listAdapter = new DownloadStatisticsListAdapter(getContext());
        downloadStatisticsList.setLayoutManager(new LinearLayoutManager(getContext()));
        downloadStatisticsList.setAdapter(listAdapter);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshDownloadStatistics();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.statistics_reset).setVisible(false);
        menu.findItem(R.id.statistics_filter).setVisible(false);
    }

    private void refreshDownloadStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        downloadStatisticsList.setVisibility(View.GONE);
        loadStatistics();
    }

    private void loadStatistics() {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable =
                Observable.fromCallable(() -> {
                    // Filters do not matter here
                    DBReader.StatisticsResult statisticsData = DBReader.getStatistics(false, 0, Long.MAX_VALUE);
                    Collections.sort(statisticsData.feedTime, (item1, item2) ->
                            Long.compare(item2.totalDownloadSize, item1.totalDownloadSize));
                    return statisticsData;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    listAdapter.update(result.feedTime);
                    progressBar.setVisibility(View.GONE);
                    downloadStatisticsList.setVisibility(View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
