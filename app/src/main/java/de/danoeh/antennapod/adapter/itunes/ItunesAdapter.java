package de.danoeh.antennapod.adapter.itunes;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
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
    private static final String FETCH_IMAGE_FILTER = "ItunesAdapter_receiver";

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
    class FetchImageReceiver extends BroadcastReceiver {
        /**
         * ImageView to be updated
         */
        private final ImageView imageView;

        /**
         * Constructor
         *
         * @param imageView UI image to be updated
         */
        FetchImageReceiver(ImageView imageView){
            this.imageView = imageView;
        }

        //Set the background image for the podcast
        @Override
        public void onReceive(Context receiverContext, Intent receiverIntent) {
            Bitmap img = (Bitmap) receiverIntent.getParcelableExtra("bitmap");
            if(img!=null) {
                imageView.setImageBitmap(img);
            }
        }
    }

    public static class FetchImageService extends IntentService {
        /**
         * Current podcast that has the image
         */
        Podcast podcast;

        public FetchImageService() {
            super("FetchImageService");
        }

        //Get the image from the url
        public void onHandleIntent(Intent intent) {
            podcast = (Podcast) intent.getSerializableExtra("podcast");
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(podcast.imageUrl);
            try {
                HttpResponse response = client.execute(get);
                Intent resultIntent = new Intent(FETCH_IMAGE_FILTER);
                resultIntent.putExtra("bitmap", BitmapFactory
                        .decodeStream(response.getEntity().getContent()));
                LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);
                return;
            } catch (IOException e) {
                e.printStackTrace();
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
        FetchImageReceiver receiver = new FetchImageReceiver(viewHolder.coverView);
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, new IntentFilter(FETCH_IMAGE_FILTER));
        Intent fetchImage = new Intent(context, FetchImageService.class);
        fetchImage.putExtra("podcast", podcast);
        context.startService(fetchImage);

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
    public static class Podcast implements java.io.Serializable { //TODO: Move this out eventually. Possibly to core.itunes.model

        private static final long serialVersionUID = 866610209692496362L;

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
