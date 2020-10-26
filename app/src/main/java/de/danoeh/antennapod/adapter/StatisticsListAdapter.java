package de.danoeh.antennapod.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.view.PieChartView;

import java.util.List;

/**
 * Parent Adapter for the playback and download statistics list.
 */
public abstract class StatisticsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FEED = 1;
    final Context context;
    private List<StatisticsItem> statisticsData;
    PieChartView.PieChartData pieChartData;

    StatisticsListAdapter(Context context) {
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
            View view = inflater.inflate(R.layout.statistics_listitem_total, parent, false);
            TextView totalText = view.findViewById(R.id.total_description);
            totalText.setText(getHeaderCaption());
            return new HeaderHolder(view);
        }
        return new StatisticsHolder(inflater.inflate(R.layout.statistics_listitem, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderHolder holder = (HeaderHolder) h;
            holder.pieChart.setData(pieChartData);
            holder.totalTime.setText(getHeaderValue());
        } else {
            StatisticsHolder holder = (StatisticsHolder) h;
            StatisticsItem statsItem = statisticsData.get(position - 1);
            Glide.with(context)
                    .load(statsItem.feed.getImageLocation())
                    .apply(new RequestOptions()
                            .placeholder(R.color.light_gray)
                            .error(R.color.light_gray)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .fitCenter()
                            .dontAnimate())
                    .into(holder.image);

            holder.title.setText(statsItem.feed.getTitle());
            holder.chip.setTextColor(pieChartData.getColorOfItem(position - 1));
            onBindFeedViewHolder(holder, statsItem);
        }
    }

    public void update(List<StatisticsItem> statistics) {
        statisticsData = statistics;
        pieChartData = generateChartData(statistics);
        notifyDataSetChanged();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView totalTime;
        PieChartView pieChart;

        HeaderHolder(View itemView) {
            super(itemView);
            totalTime = itemView.findViewById(R.id.total_time);
            pieChart = itemView.findViewById(R.id.pie_chart);
        }
    }

    static class StatisticsHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView value;
        TextView chip;

        StatisticsHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgvCover);
            title = itemView.findViewById(R.id.txtvTitle);
            value = itemView.findViewById(R.id.txtvValue);
            chip = itemView.findViewById(R.id.chip);
        }
    }

    abstract String getHeaderCaption();

    abstract String getHeaderValue();

    abstract PieChartView.PieChartData generateChartData(List<StatisticsItem> statisticsData);

    abstract void onBindFeedViewHolder(StatisticsHolder holder, StatisticsItem item);
}
