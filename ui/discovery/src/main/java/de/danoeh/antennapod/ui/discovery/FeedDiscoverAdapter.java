package de.danoeh.antennapod.ui.discovery;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.ui.common.GenerativeUrlBuilder;
import de.danoeh.antennapod.ui.glide.CoverLoader;

public class FeedDiscoverAdapter extends BaseAdapter {

    private final List<PodcastSearchResult> data = new ArrayList<>();
    private final Context context;

    public FeedDiscoverAdapter(Context context) {
        this.context = context;
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
            convertView = View.inflate(context, R.layout.quick_feed_discovery_item, null);
            holder = new Holder();
            holder.imageView = convertView.findViewById(R.id.discovery_cover);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }


        final PodcastSearchResult podcast = getItem(position);
        holder.imageView.setContentDescription(podcast.title);

        CoverLoader.with(context,
                        8f,
                        new RequestOptions()
                                .dontAnimate(),
                        new GenerativeUrlBuilder(
                                podcast.imageUrl,
                                podcast.title,
                                podcast.feedUrl,
                                false))
                .into(holder.imageView);

        return convertView;
    }

    static class Holder {
        ImageView imageView;
    }
}
