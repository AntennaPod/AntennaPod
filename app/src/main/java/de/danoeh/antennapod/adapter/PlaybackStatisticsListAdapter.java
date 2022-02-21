package de.danoeh.antennapod.adapter;

import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.fragment.FeedStatisticsDialogFragment;
import de.danoeh.antennapod.view.PieChartView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the playback statistics list.
 */
public class PlaybackStatisticsListAdapter extends StatisticsListAdapter {

    private final Fragment fragment;
    private long timeFilterFrom = 0;
    private long timeFilterTo = Long.MAX_VALUE;
    private boolean includeMarkedAsPlayed = false;

    public PlaybackStatisticsListAdapter(Fragment fragment) {
        super(fragment.getContext());
        this.fragment = fragment;
    }

    public void setTimeFilter(boolean includeMarkedAsPlayed, long timeFilterFrom, long timeFilterTo) {
        this.includeMarkedAsPlayed = includeMarkedAsPlayed;
        this.timeFilterFrom = timeFilterFrom;
        this.timeFilterTo = timeFilterTo;
    }

    @Override
    String getHeaderCaption() {
        if (includeMarkedAsPlayed) {
            return context.getString(R.string.statistics_counting_total);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        String dateFrom = dateFormat.format(new Date(timeFilterFrom));
        String dateTo = dateFormat.format(new Date(timeFilterTo));
        return context.getString(R.string.statistics_counting_range, dateFrom, dateTo);
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
            dataValues[i] = item.timePlayed;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem statsItem) {
        long time = statsItem.timePlayed;
        holder.value.setText(Converter.shortLocalizedDuration(context, time));

        holder.itemView.setOnClickListener(v -> {
            FeedStatisticsDialogFragment yourDialogFragment = FeedStatisticsDialogFragment.newInstance(
                    statsItem.feed.getId(), statsItem.feed.getTitle());
            yourDialogFragment.show(fragment.getChildFragmentManager().beginTransaction(), "DialogFragment");
        });
    }
}
