package de.danoeh.antennapod.adapter;

import android.content.Context;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;

/**
 * Adapter for the download statistics list
 */
public class DownloadStatisticsListAdapter extends StatisticsListAdapter {

    public DownloadStatisticsListAdapter(Context context) {
        super(context);
    }

    @Override
    int getHeaderCaptionResourceId() {
        return R.string.total_size_downloaded_podcasts;
    }

    @Override
    void onBindHeaderViewHolder(HeaderHolder holder) {
        long totalDownloadSize = 0;

        for (DBReader.StatisticsItem item: statisticsData.feeds) {
            totalDownloadSize = totalDownloadSize + item.totalDownloadSize;
        }
        holder.totalTime.setText(Converter.byteToString(totalDownloadSize));
        float[] dataValues = new float[statisticsData.feeds.size()];
        for (int i = 0; i < statisticsData.feeds.size(); i++) {
            DBReader.StatisticsItem item = statisticsData.feeds.get(i);
            dataValues[i] = item.totalDownloadSize;
        }
        holder.pieChart.setData(dataValues);
    }

    @Override
    void onBindFeedViewHolder(StatisticsHolder holder, int position) {
        DBReader.StatisticsItem statsItem = statisticsData.feeds.get(position - 1);
        holder.value.setText(Converter.byteToString(statsItem.totalDownloadSize));
    }

}
