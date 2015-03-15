package de.danoeh.antennapod.adapter.itunes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

    /**
     * Updates the given ImageView with the image in the given Podcast's imageUrl
     */
    class FetchImageTask extends  AsyncTask<Void,Void,Bitmap>{
        /**
         * Current podcast
         */
        private final Podcast podcast;

        /**
         * ImageView to be updated
         */
        private final ImageView imageView;

        /**
         * Constructor
         *
         * @param podcast Podcast that has the image
         * @param imageView UI image to be updated
         */
        FetchImageTask(Podcast podcast, ImageView imageView){
            this.podcast = podcast;
            this.imageView = imageView;
        }

        //Get the image from the url
        @Override
        protected Bitmap doInBackground(Void... params) {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(podcast.imageUrl);
            try {
                HttpResponse response = client.execute(get);
                return BitmapFactory.decodeStream(response.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        //Set the background image for the podcast
        @Override
        protected void onPostExecute(Bitmap img) {
            super.onPostExecute(img);
            if(img!=null) {
                imageView.setImageBitmap(img);
            }
        }
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

        //Update the empty imageView with the image from the feed
        new FetchImageTask(podcast,viewHolder.coverView).execute();

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


        /**
         * Constructor
         * @param view GridView cell
         */
        PodcastViewHolder(View view){
            coverView = (ImageView) view.findViewById(R.id.imgvCover);
            titleView = (TextView) view.findViewById(R.id.txtvTitle);
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

        /**
         * Constructor.
         *
         * @param json object holding the podcast information
         * @throws JSONException
         */
        public Podcast(JSONObject json) throws JSONException {
            title = json.getString("collectionName");
            imageUrl = json.getString("artworkUrl100");
            feedUrl = json.getString("feedUrl");
        }
    }
}
