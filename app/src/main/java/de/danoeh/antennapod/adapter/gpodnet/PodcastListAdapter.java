package de.danoeh.antennapod.adapter.gpodnet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import com.bumptech.glide.request.RequestOptions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetPodcast;

/**
 * Adapter for displaying a list of GPodnetPodcast-Objects.
 */
public class PodcastListAdapter extends ArrayAdapter<GpodnetPodcast> {

    public PodcastListAdapter(Context context, int resource, List<GpodnetPodcast> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        GpodnetPodcast podcast = getItem(position);

        // Inflate Layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.gpodnet_podcast_listitem, parent, false);
            holder.image = convertView.findViewById(R.id.imgvCover);
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.subscribers = convertView.findViewById(R.id.txtvSubscribers);
            holder.description = convertView.findViewById(R.id.txtvDescription);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        if (StringUtils.isNotBlank(podcast.getLogoUrl())) {
            Glide.with(convertView.getContext())
                    .load(podcast.getLogoUrl())
                    .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                    .into(holder.image);
        }

        holder.title.setText(podcast.getTitle());
        holder.subscribers.setText(String.valueOf(podcast.getSubscribers()));
        holder.description.setText(podcast.getDescription());

        return convertView;
    }

    static class Holder {
        ImageView image;
        TextView title;
        TextView subscribers;
        TextView description;
    }
}
