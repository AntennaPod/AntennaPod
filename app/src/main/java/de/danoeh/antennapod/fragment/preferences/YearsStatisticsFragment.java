package de.danoeh.antennapod.fragment.preferences;

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
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.YearStatisticsListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the yearly statistics screen
 */
public class YearsStatisticsFragment extends Fragment {
    private static final String TAG = YearsStatisticsFragment.class.getSimpleName();

    private Disposable disposable;
    private RecyclerView yearStatisticsList;
    private ProgressBar progressBar;
    private YearStatisticsListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.statistics_activity, container, false);
        yearStatisticsList = root.findViewById(R.id.statistics_list);
        progressBar = root.findViewById(R.id.progressBar);
        listAdapter = new YearStatisticsListAdapter(getContext());
        yearStatisticsList.setLayoutManager(new LinearLayoutManager(getContext()));
        yearStatisticsList.setAdapter(listAdapter);
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
        menu.findItem(R.id.statistics_reset).setVisible(false);
        menu.findItem(R.id.statistics_filter).setVisible(false);
    }

    private void refreshStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        yearStatisticsList.setVisibility(View.GONE);
        loadStatistics();
    }

    private void loadStatistics() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(DBReader::getMonthlyTimeStatistics)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    listAdapter.update(result);
                    progressBar.setVisibility(View.GONE);
                    yearStatisticsList.setVisibility(View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
