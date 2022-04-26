package de.danoeh.antennapod.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FeedDiscoverAdapter extends BaseAdapter {

    private final WeakReference<MainActivity> mainActivityRef;
    private final List<PodcastSearchResult> data = new ArrayList<>();

    public FeedDiscoverAdapter(MainActivity mainActivity) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public void updateData(List<PodcastSearchResult> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public PodcastSearchResult getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        if (convertView == null) {
            convertView = View.inflate(mainActivityRef.get(), R.layout.quick_feed_discovery_item, null);
            holder = new Holder();
            holder.imageView = convertView.findViewById(R.id.discovery_cover);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }


        final PodcastSearchResult podcast = getItem(position);
        holder.imageView.setContentDescription(podcast.title);

        Glide.with(mainActivityRef.get())
                .load(podcast.imageUrl)
                .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .fitCenter()
                        .dontAnimate())
                .into(holder.imageView);

        return convertView;
    }

    static class Holder {
        ImageView imageView;
    }
}
