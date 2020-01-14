package de.danoeh.antennapod.adapter;

import android.content.Context;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.view.PieChartView;

/**
 * Adapter for the download statistics list.
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
    String getHeaderValue() {
        return Converter.byteToString((long) pieChartData.getSum());
    }

    @Override
    PieChartView.PieChartData generateChartData(DBReader.StatisticsData statisticsData) {
        float[] dataValues = new float[statisticsData.feeds.size()];
        for (int i = 0; i < statisticsData.feeds.size(); i++) {
            DBReader.StatisticsItem item = statisticsData.feeds.get(i);
            dataValues[i] = item.totalDownloadSize;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    void onBindFeedViewHolder(StatisticsHolder holder, DBReader.StatisticsItem item) {
        holder.value.setText(Converter.byteToString(item.totalDownloadSize));
    }

}
