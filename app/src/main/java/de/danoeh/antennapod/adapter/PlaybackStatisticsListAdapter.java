package de.danoeh.antennapod.adapter;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.view.PieChartView;

/**
 * Adapter for the playback statistics list.
 */
public class PlaybackStatisticsListAdapter extends StatisticsListAdapter {

    boolean countAll = true;

    public PlaybackStatisticsListAdapter(Context context) {
        super(context);
    }

    public void setCountAll(boolean countAll) {
        this.countAll = countAll;
    }

    @Override
    int getHeaderCaptionResourceId() {
        return R.string.total_time_listened_to_podcasts;
    }

    @Override
    String getHeaderValue() {
        return Converter.shortLocalizedDuration(context, (long) pieChartData.getSum());
    }

    @Override
    PieChartView.PieChartData generateChartData(DBReader.StatisticsData statisticsData) {
        float[] dataValues = new float[statisticsData.feeds.size()];
        for (int i = 0; i < statisticsData.feeds.size(); i++) {
            DBReader.StatisticsItem item = statisticsData.feeds.get(i);
            dataValues[i] = countAll ? item.timePlayedCountAll : item.timePlayed;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    void onBindFeedViewHolder(StatisticsHolder holder, DBReader.StatisticsItem statsItem) {
        long time = countAll ? statsItem.timePlayedCountAll : statsItem.timePlayed;
        holder.value.setText(Converter.shortLocalizedDuration(context, time));

        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(statsItem.feed.getTitle());
            dialog.setMessage(context.getString(R.string.statistics_details_dialog,
                    countAll ? statsItem.episodesStartedIncludingMarked : statsItem.episodesStarted,
                    statsItem.episodes, Converter.shortLocalizedDuration(context,
                            countAll ? statsItem.timePlayedCountAll : statsItem.timePlayed),
                    Converter.shortLocalizedDuration(context, statsItem.time)));
            dialog.setPositiveButton(android.R.string.ok, null);
            dialog.show();
        });
    }

}
