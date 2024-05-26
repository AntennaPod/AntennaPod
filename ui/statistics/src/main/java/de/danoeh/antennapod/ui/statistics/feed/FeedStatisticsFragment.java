package de.danoeh.antennapod.ui.statistics.feed;

import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.StatisticsItem;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.DateFormatter;
import de.danoeh.antennapod.storage.database.ReleaseScheduleGuesser;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.ui.statistics.R;
import de.danoeh.antennapod.ui.statistics.databinding.FeedStatisticsBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
                    DBReader.StatisticsResult statisticsData = DBReader.getStatistics(true, 0, Long.MAX_VALUE);
                    Collections.sort(statisticsData.feedTime, (item1, item2) ->
                            Long.compare(item2.timePlayed, item1.timePlayed));

                    for (StatisticsItem statisticsItem : statisticsData.feedTime) {
                        if (statisticsItem.feed.getId() == feedId) {
                            List<FeedItem> items = DBReader.getFeedItemList(statisticsItem.feed,
                                    FeedItemFilter.unfiltered(), SortOrder.DATE_OLD_NEW, 0, Integer.MAX_VALUE);
                            List<Date> dates = new ArrayList<>();
                            for (FeedItem item : items) {
                                dates.add(item.getPubDate());
                            }
                            ReleaseScheduleGuesser.Guess guess = null;
                            if (dates.size() > 1) {
                                guess = ReleaseScheduleGuesser.performGuess(dates);
                            }
                            return new Pair<>(statisticsItem, guess);
                        }
                    }
                    return null;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::showStats, Throwable::printStackTrace);
    }

    private String getReadableDay(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return getString(R.string.release_schedule_monday);
            case Calendar.TUESDAY:
                return getString(R.string.release_schedule_tuesday);
            case Calendar.WEDNESDAY:
                return getString(R.string.release_schedule_wednesday);
            case Calendar.THURSDAY:
                return getString(R.string.release_schedule_thursday);
            case Calendar.FRIDAY:
                return getString(R.string.release_schedule_friday);
            case Calendar.SATURDAY:
                return getString(R.string.release_schedule_saturday);
            case Calendar.SUNDAY:
                return getString(R.string.release_schedule_sunday);
            default:
                return "error";
        }
    }

    private String getReadableSchedule(ReleaseScheduleGuesser.Guess guess) {
        switch (guess.schedule) {
            case DAILY:
                return getString(R.string.release_schedule_daily);
            case WEEKDAYS:
                return getString(R.string.release_schedule_weekdays);
            case WEEKLY:
                return getString(R.string.release_schedule_weekly) + ", " + getReadableDay(guess.days.get(0));
            case BIWEEKLY:
                return getString(R.string.release_schedule_biweekly) + ", " + getReadableDay(guess.days.get(0));
            case MONTHLY:
                return getString(R.string.release_schedule_monthly);
            case FOURWEEKLY:
                return getString(R.string.release_schedule_monthly) + ", " + getReadableDay(guess.days.get(0));
            case SPECIFIC_DAYS:
                StringBuilder days = new StringBuilder();
                for (int i = 0; i < guess.days.size(); i++) {
                    if (i != 0) {
                        days.append(", ");
                    }
                    days.append(getReadableDay(guess.days.get(i)));
                }
                return days.toString();
            default:
                return getString(R.string.statistics_expected_next_episode_unknown);
        }
    }

    private void showStats(Pair<StatisticsItem, ReleaseScheduleGuesser.Guess> p) {
        StatisticsItem s = p.first;
        viewBinding.startedTotalLabel.setText(String.format(Locale.getDefault(), "%d / %d",
                s.episodesStarted, s.episodes));
        viewBinding.timePlayedLabel.setText(Converter.shortLocalizedDuration(getContext(), s.timePlayed));
        viewBinding.totalDurationLabel.setText(Converter.shortLocalizedDuration(getContext(), s.time));
        viewBinding.onDeviceLabel.setText(String.format(Locale.getDefault(), "%d", s.episodesDownloadCount));
        viewBinding.spaceUsedLabel.setText(Formatter.formatShortFileSize(getContext(), s.totalDownloadSize));

        ReleaseScheduleGuesser.Guess guess = p.second;
        if (!s.feed.getPreferences().getKeepUpdated()) {
            viewBinding.expectedNextEpisodeLabel.setText(R.string.updates_disabled_label);
        } else if (guess == null || guess.nextExpectedDate.getTime() <= new Date().getTime() - 7 * 24 * 3600000L) {
            // More than 30 days delayed
            viewBinding.expectedNextEpisodeLabel.setText(R.string.statistics_expected_next_episode_unknown);
        } else {
            String text = DateFormatter.formatAbbrev(getContext(), guess.nextExpectedDate);
            if (guess.nextExpectedDate.getTime() <= new Date().getTime()) {
                text = getString(R.string.statistics_expected_next_episode_any_day);
            }
            if (guess.schedule != ReleaseScheduleGuesser.Schedule.UNKNOWN) {
                text += " (" + getReadableSchedule(guess) + ")";
            }
            viewBinding.expectedNextEpisodeLabel.setText(text);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
