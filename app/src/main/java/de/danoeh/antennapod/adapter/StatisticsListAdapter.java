package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;

/**
 * Adapter for the statistics list
 */
public class StatisticsListAdapter extends BaseAdapter {
    private final Context context;
    private List<DBReader.StatisticsItem> feedTime = new ArrayList<>();
    private boolean countAll = true;

    public StatisticsListAdapter(Context context) {
        this.context = context;
    }

    public void setCountAll(boolean countAll) {
        this.countAll = countAll;
    }

    @Override
    public int getCount() {
        return feedTime.size();
    }

    @Override
    public DBReader.StatisticsItem getItem(int position) {
        return feedTime.get(position);
    }

    @Override
    public long getItemId(int position) {
        return feedTime.get(position).feed.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StatisticsHolder holder;
        Feed feed = feedTime.get(position).feed;

        if (convertView == null) {
            holder = new StatisticsHolder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.statistics_listitem, parent, false);

            holder.image = (ImageView) convertView.findViewById(R.id.imgvCover);
            holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
            holder.time = (TextView) convertView.findViewById(R.id.txtvTime);
            convertView.setTag(holder);
        } else {
            holder = (StatisticsHolder) convertView.getTag();
        }

        Glide.with(context)
                .load(feed.getImageLocation())
                .placeholder(R.color.light_gray)
                .error(R.color.light_gray)
                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                .fitCenter()
                .dontAnimate()
                .into(holder.image);

        holder.title.setText(feed.getTitle());
        holder.time.setText(Converter.shortLocalizedDuration(context,
                countAll ? feedTime.get(position).timePlayedCountAll
                        : feedTime.get(position).timePlayed));
        return convertView;
    }

    public void update(List<DBReader.StatisticsItem> feedTime) {
        this.feedTime = feedTime;
        notifyDataSetChanged();
    }

    static class StatisticsHolder {
        ImageView image;
        TextView title;
        TextView time;
    }

}
