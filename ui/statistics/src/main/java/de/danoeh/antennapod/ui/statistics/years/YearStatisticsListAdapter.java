package de.danoeh.antennapod.ui.statistics.years;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.ui.statistics.R;
import de.danoeh.antennapod.ui.statistics.StatisticsColorScheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the yearly playback statistics list.
 */
public class YearStatisticsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FEED = 1;
    final Context context;
    private final List<DBReader.MonthlyStatisticsItem> statisticsData = new ArrayList<>();
    BarChartView.BarChartData barChartData;

    public YearStatisticsListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return statisticsData.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_FEED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.statistics_listitem_linechart, parent, false));
        }
        return new StatisticsHolder(inflater.inflate(R.layout.statistics_year_listitem, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderHolder holder = (HeaderHolder) h;
            holder.barChart.setData(barChartData);
        } else {
            StatisticsHolder holder = (StatisticsHolder) h;
            DBReader.MonthlyStatisticsItem statsItem = statisticsData.get(position - 1);
            holder.year.setText(String.format(Locale.getDefault(), "%d ", statsItem.year));
            holder.hours.setText(String.format(Locale.getDefault(), "%.1f ", statsItem.timePlayed / 3600000.0f)
                    + context.getString(R.string.time_hours));
            holder.chip.setTextColor(StatisticsColorScheme.COLOR_VALUES[
                    (getItemCount() - position - 1) % StatisticsColorScheme.COLOR_VALUES.length]);
        }
    }

    public void update(List<DBReader.MonthlyStatisticsItem> statistics) {
        int lastYear = statistics.size() > 0 ? statistics.get(0).year : 0;
        int lastDataPoint = statistics.size() > 0 ? (statistics.get(0).month - 1) + lastYear * 12 : 0;
        long yearSum = 0;
        statisticsData.clear();
        LongList barChartValues = new LongList();
        LongList barChartDivider = new LongList();
        for (DBReader.MonthlyStatisticsItem statistic : statistics) {
            if (statistic.year != lastYear) {
                DBReader.MonthlyStatisticsItem yearAggregate = new DBReader.MonthlyStatisticsItem();
                yearAggregate.year = lastYear;
                yearAggregate.timePlayed = yearSum;
                statisticsData.add(yearAggregate);
                yearSum = 0;
                lastYear = statistic.year;
                barChartDivider.add(barChartValues.size());
            }
            yearSum += statistic.timePlayed;
            while (lastDataPoint + 1 < (statistic.month - 1) + statistic.year * 12) {
                barChartValues.add(0); // Compensate for months without playback
                lastDataPoint++;
            }
            barChartValues.add(statistic.timePlayed);
            lastDataPoint = (statistic.month - 1) + statistic.year * 12;
        }
        DBReader.MonthlyStatisticsItem yearAggregate = new DBReader.MonthlyStatisticsItem();
        yearAggregate.year = lastYear;
        yearAggregate.timePlayed = yearSum;
        statisticsData.add(yearAggregate);
        Collections.reverse(statisticsData);
        barChartData = new BarChartView.BarChartData(barChartValues.toArray(), barChartDivider.toArray());
        notifyDataSetChanged();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        BarChartView barChart;

        HeaderHolder(View itemView) {
            super(itemView);
            barChart = itemView.findViewById(R.id.barChart);
        }
    }

    static class StatisticsHolder extends RecyclerView.ViewHolder {
        TextView year;
        TextView hours;
        TextView chip;

        StatisticsHolder(View itemView) {
            super(itemView);
            year = itemView.findViewById(R.id.yearLabel);
            hours = itemView.findViewById(R.id.hoursLabel);
            chip = itemView.findViewById(R.id.chip);
        }
    }
}
