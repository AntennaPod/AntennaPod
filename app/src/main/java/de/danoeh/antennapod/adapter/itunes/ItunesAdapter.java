package de.danoeh.antennapod.adapter.itunes;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

public class ItunesAdapter extends ArrayAdapter<ItunesAdapter.Podcast> {
    /**
     * Related Context
     */
    private final Context context;

    /**
     * List holding the podcasts found in the search
     */
    private final List<Podcast> data;

    /**
     * Constructor.
     *
     * @param context Related context
     * @param objects Search result
     */
    public ItunesAdapter(Context context, List<Podcast> objects) {
        super(context, 0, objects);
        this.data = objects;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Current podcast
        Podcast podcast = data.get(position);

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
        if(!podcast.feedUrl.contains("itunes.apple.com")) {
            viewHolder.urlView.setText(podcast.feedUrl);
            viewHolder.urlView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.urlView.setVisibility(View.GONE);
        }

        //Update the empty imageView with the image from the feed
        Glide.with(context)
                .load(podcast.imageUrl)
                .placeholder(R.color.light_gray)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .fitCenter()
                .dontAnimate()
                .into(viewHolder.coverView);

        //Feed the grid view
        return view;
    }

    /**
     * View holder object for the GridView
     */
    class PodcastViewHolder {

        /**
         * ImageView holding the Podcast image
         */
        public final ImageView coverView;

        /**
         * TextView holding the Podcast title
         */
        public final TextView titleView;

        public final TextView urlView;


        /**
         * Constructor
         * @param view GridView cell
         */
        PodcastViewHolder(View view){
            coverView = (ImageView) view.findViewById(R.id.imgvCover);
            titleView = (TextView) view.findViewById(R.id.txtvTitle);
            urlView = (TextView) view.findViewById(R.id.txtvUrl);
        }
    }

    /**
     * Represents an individual podcast on the iTunes Store.
     */
    public static class Podcast { //TODO: Move this out eventually. Possibly to core.itunes.model

        /**
         * The name of the podcast
         */
        public final String title;

        /**
         * URL of the podcast image
         */
        public final String imageUrl;
        /**
         * URL of the podcast feed
         */
        public final String feedUrl;


        private Podcast(String title, String imageUrl, String feedUrl) {
            this.title = title;
            this.imageUrl = imageUrl;
            this.feedUrl = feedUrl;
        }

        /**
         * Constructs a Podcast instance from a iTunes search result
         *
         * @param json object holding the podcast information
         * @throws JSONException
         */
        public static Podcast fromSearch(JSONObject json) throws JSONException {
            String title = json.getString("collectionName");
            String imageUrl = json.getString("artworkUrl100");
            String feedUrl = json.getString("feedUrl");
            return new Podcast(title, imageUrl, feedUrl);
        }

        /**
         * Constructs a Podcast instance from iTunes toplist entry
         *
         * @param json object holding the podcast information
         * @throws JSONException
         */
        public static Podcast fromToplist(JSONObject json) throws JSONException {
            String title = json.getJSONObject("title").getString("label");
            String imageUrl = null;
            JSONArray images =  json.getJSONArray("im:image");
            for(int i=0; imageUrl == null && i < images.length(); i++) {
                JSONObject image = images.getJSONObject(i);
                String height = image.getJSONObject("attributes").getString("height");
                if(Integer.parseInt(height) >= 100) {
                    imageUrl = image.getString("label");
                }
            }
            String feedUrl = "https://itunes.apple.com/lookup?id=" +
                    json.getJSONObject("id").getJSONObject("attributes").getString("im:id");
            return new Podcast(title, imageUrl, feedUrl);
        }

    }
}
