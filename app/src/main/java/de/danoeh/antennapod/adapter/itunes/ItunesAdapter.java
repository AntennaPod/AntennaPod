package de.danoeh.antennapod.adapter.itunes;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.discovery.PodcastSearchResult;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

public class ItunesAdapter extends ArrayAdapter<PodcastSearchResult> {
    /**
     * Related Context
     */
    private final Context context;

    /**
     * List holding the podcasts found in the search
     */
    private final List<PodcastSearchResult> data;

    /**
     * Constructor.
     *
     * @param context Related context
     * @param objects Search result
     */
    public ItunesAdapter(Context context, List<PodcastSearchResult> objects) {
        super(context, 0, objects);
        this.data = objects;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        //Current podcast
        PodcastSearchResult podcast = data.get(position);

        //ViewHolder
        PodcastViewHolder viewHolder;

        //Resulting view
        View view;

        //Handle view holder stuff
        if(convertView == null) {
            view = ((MainActivity) context).getLayoutInflater()
                    .inflate(R.layout.itunes_podcast_listitem, parent, false);
            viewHolder = new PodcastViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (PodcastViewHolder) view.getTag();
        }

        //Set the title
        viewHolder.titleView.setText(podcast.title);
        if(podcast.summary != null && ! podcast.summary.trim().isEmpty()) {
            viewHolder.descriptionView.setText(podcast.summary);
            viewHolder.descriptionView.setVisibility(View.VISIBLE);
        } else if(podcast.feedUrl != null && !podcast.feedUrl.contains("itunes.apple.com")) {
            viewHolder.descriptionView.setText(podcast.feedUrl);
            viewHolder.descriptionView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.descriptionView.setVisibility(View.GONE);
        }

        //Update the empty imageView with the image from the feed
        Glide.with(context)
                .load(podcast.imageUrl)
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .fitCenter()
                    .dontAnimate())
                .into(viewHolder.coverView);

        //Feed the grid view
        return view;
    }

    /**
     * View holder object for the GridView
     */
    static class PodcastViewHolder {

        /**
         * ImageView holding the Podcast image
         */
        final ImageView coverView;

        /**
         * TextView holding the Podcast title
         */
        final TextView titleView;

        final TextView descriptionView;


        /**
         * Constructor
         * @param view GridView cell
         */
        PodcastViewHolder(View view){
            coverView = view.findViewById(R.id.imgvCover);
            titleView = view.findViewById(R.id.txtvTitle);
            descriptionView = view.findViewById(R.id.txtvDescription);
        }
    }
}
