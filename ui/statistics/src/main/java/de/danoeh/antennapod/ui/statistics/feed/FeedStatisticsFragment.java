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
import de.danoeh.antennapod.ui.common.ThemeUtils;
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
        loadStatistics();

        if (getArguments().getBoolean(EXTRA_DETAILED)) {
            viewBinding.secondRowContainer.setVisibility(View.VISIBLE);
            int color = ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSurface);
            viewBinding.playbackTime.getRoot().setBackgroundColor(color);
            viewBinding.episodesStarted.getRoot().setBackgroundColor(color);
            viewBinding.spaceDownloaded.getRoot().setBackgroundColor(color);
            viewBinding.episodesTotal.getRoot().setBackgroundColor(color);
            viewBinding.durationTotal.getRoot().setBackgroundColor(color);
            viewBinding.episodesDownloaded.getRoot().setBackgroundColor(color);
            viewBinding.expectedNextEpisode.getRoot().setBackgroundColor(color);
            viewBinding.episodeSchedule.getRoot().setBackgroundColor(color);
        }
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
        viewBinding.episodesStarted.mainLabel.setText(getResources()
                .getQuantityString(R.plurals.num_episodes, (int) s.episodesStarted, s.episodesStarted));
        viewBinding.episodesStarted.subtitleLabel.setText(getResources()
                .getQuantityString(R.plurals.statistics_episodes_started, (int) s.episodesStarted));

        viewBinding.episodesTotal.mainLabel.setText(getResources()
                .getQuantityString(R.plurals.num_episodes, (int) s.episodes, s.episodes));
        viewBinding.episodesTotal.subtitleLabel.setText(getResources()
                .getQuantityString(R.plurals.statistics_episodes_total, (int) s.episodes));

        viewBinding.playbackTime.mainLabel.setText(Converter.shortLocalizedDuration(getContext(), s.timePlayed));
        viewBinding.playbackTime.subtitleLabel.setText(R.string.statistics_time_played);

        viewBinding.durationTotal.mainLabel.setText(Converter.shortLocalizedDuration(getContext(), s.time));
        viewBinding.durationTotal.subtitleLabel.setText(R.string.statistics_time_total);

        viewBinding.episodesDownloaded.mainLabel.setText(getResources()
                .getQuantityString(R.plurals.num_episodes, (int) s.episodesDownloadCount, s.episodesDownloadCount));
        viewBinding.episodesDownloaded.subtitleLabel.setText(getResources()
                .getQuantityString(R.plurals.statistics_episodes_downloaded, (int) s.episodesDownloadCount));

        viewBinding.spaceDownloaded.mainLabel.setText(Formatter.formatShortFileSize(getContext(), s.totalDownloadSize));
        viewBinding.spaceDownloaded.subtitleLabel.setText(R.string.statistics_episodes_space);

        viewBinding.expectedNextEpisode.subtitleLabel.setText(R.string.statistics_release_next);
        viewBinding.episodeSchedule.subtitleLabel.setText(R.string.statistics_release_schedule);
        ReleaseScheduleGuesser.Guess guess = p.second;
        if (s.feed.isLocalFeed()) {
            viewBinding.expectedNextEpisode.mainLabel.setText(R.string.local_folder);
            viewBinding.episodeSchedule.mainLabel.setText(R.string.local_folder);
        } else if (!s.feed.getPreferences().getKeepUpdated()) {
            viewBinding.expectedNextEpisode.mainLabel.setText(R.string.updates_disabled_label);
            viewBinding.episodeSchedule.mainLabel.setText(R.string.updates_disabled_label);
        } else if (guess == null || guess.nextExpectedDate.getTime() <= new Date().getTime() - 7 * 24 * 3600000L) {
            // More than 30 days delayed
            viewBinding.expectedNextEpisode.mainLabel.setText(R.string.statistics_expected_next_episode_unknown);
            viewBinding.episodeSchedule.mainLabel.setText(R.string.statistics_expected_next_episode_unknown);
        } else {
            if (guess.nextExpectedDate.getTime() <= new Date().getTime()) {
                viewBinding.expectedNextEpisode.mainLabel.setText(R.string.statistics_expected_next_episode_any_day);
            } else {
                viewBinding.expectedNextEpisode.mainLabel.setText(
                        DateFormatter.formatAbbrev(getContext(), guess.nextExpectedDate));
            }
            if (guess.schedule == ReleaseScheduleGuesser.Schedule.UNKNOWN) {
                viewBinding.episodeSchedule.mainLabel.setText(R.string.statistics_expected_next_episode_unknown);
            } else {
                viewBinding.episodeSchedule.mainLabel.setText(getReadableSchedule(guess));
            }
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
