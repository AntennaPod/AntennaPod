package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.databinding.FeedStatisticsBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.Locale;

public class FeedStatisticsFragment extends Fragment {
    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String EXTRA_DETAILED = "de.danoeh.antennapod.extra.detailed";

    private long feedId;
    private Disposable disposable;
    private FeedStatisticsBinding viewBinding;

    public static FeedStatisticsFragment newInstance(long feedId, boolean detailed) {
        FeedStatisticsFragment fragment = new FeedStatisticsFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_FEED_ID, feedId);
        arguments.putBoolean(EXTRA_DETAILED, detailed);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        feedId = getArguments().getLong(EXTRA_FEED_ID);
        viewBinding = FeedStatisticsBinding.inflate(inflater);

        if (!getArguments().getBoolean(EXTRA_DETAILED)) {
            for (int i = 0; i < viewBinding.getRoot().getChildCount(); i++) {
                View child = viewBinding.getRoot().getChildAt(i);
                if ("detailed".equals(child.getTag())) {
                    child.setVisibility(View.GONE);
                }
            }
        }

        loadStatistics();
        return viewBinding.getRoot();
    }

    private void loadStatistics() {
        disposable =
                Observable.fromCallable(() -> {
                    List<StatisticsItem> statisticsData = DBReader.getStatistics();
                    for (StatisticsItem statisticsItem : statisticsData) {
                        if (statisticsItem.feed.getId() == feedId) {
                            return statisticsItem;
                        }
                    }
                    return null;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::showStats, Throwable::printStackTrace);
    }

    private void showStats(StatisticsItem s) {
        viewBinding.startedTotalLabel.setText(String.format(Locale.getDefault(), "%d / %d",
                s.episodesStarted, s.episodes));
        viewBinding.timePlayedLabel.setText(Converter.shortLocalizedDuration(getContext(), s.timePlayed));
        viewBinding.durationPlayedLabel.setText(Converter.shortLocalizedDuration(getContext(), s.timePlayedCountAll));
        viewBinding.totalDurationLabel.setText(Converter.shortLocalizedDuration(getContext(), s.time));
        viewBinding.onDeviceLabel.setText(String.format(Locale.getDefault(), "%d", s.episodesDownloadCount));
        viewBinding.spaceUsedLabel.setText(Formatter.formatShortFileSize(getContext(), s.totalDownloadSize));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
