package de.danoeh.antennapod.ui.discovery;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;

import java.util.ArrayList;
import java.util.List;

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

        float radius = 8 * context.getResources().getDisplayMetrics().density;
        Glide.with(context)
                .load(podcast.imageUrl)
                .apply(new RequestOptions()
                        .placeholder(ImagePlaceholder.getDrawable(context, radius))
                        .transform(new FitCenter(), new RoundedCorners((int) radius))
                        .dontAnimate())
                .into(holder.imageView);

        return convertView;
    }

    static class Holder {
        ImageView imageView;
    }
}
