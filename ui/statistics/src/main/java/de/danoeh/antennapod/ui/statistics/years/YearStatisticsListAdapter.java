package de.danoeh.antennapod.ui.statistics.years;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.ui.statistics.R;

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
    private final List<DBReader.MonthlyStatisticsItem> yearlyAggregate = new ArrayList<>();

    public YearStatisticsListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return yearlyAggregate.size() + 1;
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
            return new HeaderHolder(inflater.inflate(R.layout.statistics_listitem_barchart, parent, false));
        }
        return new StatisticsHolder(inflater.inflate(R.layout.statistics_year_listitem, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderHolder holder = (HeaderHolder) h;
            holder.barChart.setData(statisticsData);
        } else {
            StatisticsHolder holder = (StatisticsHolder) h;
            DBReader.MonthlyStatisticsItem statsItem = yearlyAggregate.get(position - 1);
            holder.year.setText(String.format(Locale.getDefault(), "%d ", statsItem.year));
            holder.hours.setText(String.format(Locale.getDefault(), "%.1f ", statsItem.timePlayed / 3600000.0f)
                    + context.getString(R.string.time_hours));
        }
    }

    public void update(List<DBReader.MonthlyStatisticsItem> statistics) {
        int lastYear = statistics.size() > 0 ? statistics.get(0).year : 0;
        int lastDataPoint = statistics.size() > 0 ? (statistics.get(0).month - 1) + lastYear * 12 : 0;
        long yearSum = 0;
        yearlyAggregate.clear();
        statisticsData.clear();
        for (DBReader.MonthlyStatisticsItem statistic : statistics) {
            if (statistic.year != lastYear) {
                DBReader.MonthlyStatisticsItem yearAggregate = new DBReader.MonthlyStatisticsItem();
                yearAggregate.year = lastYear;
                yearAggregate.timePlayed = yearSum;
                yearlyAggregate.add(yearAggregate);
                yearSum = 0;
                lastYear = statistic.year;
            }
            yearSum += statistic.timePlayed;
            while (lastDataPoint + 1 < (statistic.month - 1) + statistic.year * 12) {
                lastDataPoint++;
                DBReader.MonthlyStatisticsItem item = new DBReader.MonthlyStatisticsItem();
                item.year = lastDataPoint / 12;
                item.month = lastDataPoint % 12 + 1;
                statisticsData.add(item); // Compensate for months without playback
            }
            statisticsData.add(statistic);
            lastDataPoint = (statistic.month - 1) + statistic.year * 12;
        }
        DBReader.MonthlyStatisticsItem yearAggregate = new DBReader.MonthlyStatisticsItem();
        yearAggregate.year = lastYear;
        yearAggregate.timePlayed = yearSum;
        yearlyAggregate.add(yearAggregate);
        Collections.reverse(yearlyAggregate);
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

        StatisticsHolder(View itemView) {
            super(itemView);
            year = itemView.findViewById(R.id.yearLabel);
            hours = itemView.findViewById(R.id.hoursLabel);
        }
    }
}
