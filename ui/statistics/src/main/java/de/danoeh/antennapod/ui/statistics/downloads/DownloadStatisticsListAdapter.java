package de.danoeh.antennapod.ui.statistics.downloads;

import android.content.Context;
import android.text.format.Formatter;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.storage.database.StatisticsItem;
import de.danoeh.antennapod.ui.statistics.PieChartView;
import de.danoeh.antennapod.ui.statistics.R;
import de.danoeh.antennapod.ui.statistics.StatisticsListAdapter;
import de.danoeh.antennapod.ui.statistics.feed.FeedStatisticsDialogFragment;

import java.util.List;

/**
 * Adapter for the download statistics list.
 */
public class DownloadStatisticsListAdapter extends StatisticsListAdapter {
    private final Fragment fragment;
    private int cacheEpisodes;

    public DownloadStatisticsListAdapter(Context context, Fragment fragment) {
        super(context);
        this.fragment = fragment;
    }

    @Override
    protected String getHeaderCaption() {
        return context.getResources().getQuantityString(
                R.plurals.total_size_downloaded_podcasts, cacheEpisodes, cacheEpisodes);
    }

    @Override
    protected String getHeaderValue() {
        return Formatter.formatShortFileSize(context, (long) pieChartData.getSum());
    }

    @Override
    protected PieChartView.PieChartData generateChartData(List<StatisticsItem> statisticsData) {
        float[] dataValues = new float[statisticsData.size()];
        cacheEpisodes = 0;
        for (int i = 0; i < statisticsData.size(); i++) {
            StatisticsItem item = statisticsData.get(i);
            dataValues[i] = item.totalDownloadSize;
            cacheEpisodes += item.episodesDownloadCount;
        }
        return new PieChartView.PieChartData(dataValues);
    }

    @Override
    protected void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem item) {
        int numEpisodes = (int) item.episodesDownloadCount;
        String text = Formatter.formatShortFileSize(context, item.totalDownloadSize);
        text += " • " + context.getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes);
        holder.value.setText(text);

        holder.itemView.setOnClickListener(v ->
                FeedStatisticsDialogFragment.newInstance(item.feed.getId(), item.feed.getTitle())
                        .show(fragment.getChildFragmentManager().beginTransaction(), "FeedStatistics"));
    }

}
