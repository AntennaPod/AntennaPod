package de.danoeh.antennapod.ui.statistics.subscriptions;

import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.ui.statistics.PieChartView;
import de.danoeh.antennapod.ui.statistics.R;
import de.danoeh.antennapod.ui.statistics.StatisticsListAdapter;
import de.danoeh.antennapod.ui.statistics.feed.FeedStatisticsDialogFragment;

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
    protected String getHeaderCaption() {
        if (includeMarkedAsPlayed) {
            return context.getString(R.string.statistics_counting_total);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        String dateFrom = dateFormat.format(new Date(timeFilterFrom));
        // FilterTo is first day of next month => Subtract one day
        String dateTo = dateFormat.format(new Date(timeFilterTo - 24L * 3600000L));
        return context.getString(R.string.statistics_counting_range, dateFrom, dateTo);
    }

    @Override
    protected String getHeaderValue() {
        return Converter.shortLocalizedDuration(context, (long) pieChartData.getSum());
    }

    @Override
    protected PieChartView.PieChartData generateChartData(List<StatisticsItem> statisticsData) {
        float[] dataValues = new float[statisticsData.size()];
        for (int i = 0; i < statisticsData.size(); i++) {
            StatisticsItem item = statisticsData.get(i);
            dataValues[i] = item.timePlayed;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    protected void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem statsItem) {
        long time = statsItem.timePlayed;
        holder.value.setText(Converter.shortLocalizedDuration(context, time));

        holder.itemView.setOnClickListener(v -> {
            FeedStatisticsDialogFragment yourDialogFragment = FeedStatisticsDialogFragment.newInstance(
                    statsItem.feed.getId(), statsItem.feed.getTitle());
            yourDialogFragment.show(fragment.getChildFragmentManager().beginTransaction(), "DialogFragment");
        });
    }
}
