package de.danoeh.antennapod.adapter;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.view.PieChartView;

import java.util.List;

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
