package de.danoeh.antennapod.ui.statistics.downloads;

import android.content.Context;
import android.text.format.Formatter;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.ui.statistics.PieChartView;
import de.danoeh.antennapod.ui.statistics.R;
import de.danoeh.antennapod.ui.statistics.StatisticsListAdapter;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the download statistics list.
 */
public class DownloadStatisticsListAdapter extends StatisticsListAdapter {

    public DownloadStatisticsListAdapter(Context context) {
        super(context);
    }

    @Override
    protected String getHeaderCaption() {
        return context.getString(R.string.total_size_downloaded_podcasts);
    }

    @Override
    protected String getHeaderValue() {
        return Formatter.formatShortFileSize(context, (long) pieChartData.getSum());
    }

    @Override
    protected PieChartView.PieChartData generateChartData(List<StatisticsItem> statisticsData) {
        float[] dataValues = new float[statisticsData.size()];
        for (int i = 0; i < statisticsData.size(); i++) {
            StatisticsItem item = statisticsData.get(i);
            dataValues[i] = item.totalDownloadSize;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    protected void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem item) {
        holder.value.setText(Formatter.formatShortFileSize(context, item.totalDownloadSize)
                + " • "
                + String.format(Locale.getDefault(), "%d%s",
                item.episodesDownloadCount, context.getString(R.string.episodes_suffix)));
    }

}
