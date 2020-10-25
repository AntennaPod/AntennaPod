package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.format.Formatter;

import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.view.PieChartView;

/**
 * Adapter for the download statistics list.
 */
public class DownloadStatisticsListAdapter extends StatisticsListAdapter {

    public DownloadStatisticsListAdapter(Context context) {
        super(context);
    }

    @Override
    String getHeaderCaption() {
        return context.getString(R.string.total_size_downloaded_podcasts);
    }

    @Override
    String getHeaderValue() {
        return Formatter.formatShortFileSize(context, (long) pieChartData.getSum());
    }

    @Override
    PieChartView.PieChartData generateChartData(List<StatisticsItem> statisticsData) {
        float[] dataValues = new float[statisticsData.size()];
        for (int i = 0; i < statisticsData.size(); i++) {
            StatisticsItem item = statisticsData.get(i);
            dataValues[i] = item.totalDownloadSize;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem item) {
        holder.value.setText(Formatter.formatShortFileSize(context, item.totalDownloadSize)
                + " • "
                + String.format(Locale.getDefault(), "%d%s",
                item.episodesDownloadCount, context.getString(R.string.episodes_suffix)));
    }

}
