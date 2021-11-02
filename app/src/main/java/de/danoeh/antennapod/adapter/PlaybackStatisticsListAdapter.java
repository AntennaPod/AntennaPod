package de.danoeh.antennapod.adapter;

import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateFormatter;
import de.danoeh.antennapod.fragment.FeedStatisticsDialogFragment;
import de.danoeh.antennapod.view.PieChartView;

import java.util.Date;
import java.util.List;

/**
 * Adapter for the playback statistics list.
 */
public class PlaybackStatisticsListAdapter extends StatisticsListAdapter {

    private final Fragment fragment;
    boolean countAll = true;

    public PlaybackStatisticsListAdapter(Fragment fragment) {
        super(fragment.getContext());
        this.fragment = fragment;
    }

    public void setCountAll(boolean countAll) {
        this.countAll = countAll;
    }

    @Override
    String getHeaderCaption() {
        long usageCounting = UserPreferences.getUsageCountingDateMillis();
        if (usageCounting > 0) {
            String date = DateFormatter.formatAbbrev(context, new Date(usageCounting));
            return context.getString(R.string.statistics_counting_since, date);
        } else {
            return context.getString(R.string.total_time_listened_to_podcasts);
        }
    }

    @Override
    String getHeaderValue() {
        return Converter.shortLocalizedDuration(context, (long) pieChartData.getSum());
    }

    @Override
    PieChartView.PieChartData generateChartData(List<StatisticsItem> statisticsData) {
        float[] dataValues = new float[statisticsData.size()];
        for (int i = 0; i < statisticsData.size(); i++) {
            StatisticsItem item = statisticsData.get(i);
            dataValues[i] = countAll ? item.timePlayedCountAll : item.timePlayed;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem statsItem) {
        long time = countAll ? statsItem.timePlayedCountAll : statsItem.timePlayed;
        holder.value.setText(Converter.shortLocalizedDuration(context, time));

        holder.itemView.setOnClickListener(v -> {
            FeedStatisticsDialogFragment yourDialogFragment = FeedStatisticsDialogFragment.newInstance(
                    statsItem.feed.getId(), statsItem.feed.getTitle());
            yourDialogFragment.show(fragment.getChildFragmentManager().beginTransaction(), "DialogFragment");
        });
    }
}
