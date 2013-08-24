package de.danoeh.antennapod.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.FeedItemlistDescriptionAdapter;
import de.danoeh.antennapod.asynctask.ImageDiskCache;
import de.danoeh.antennapod.feed.Feed;

/**
 * Created by daniel on 24.08.13.
 */
public class DefaultOnlineFeedViewActivity extends OnlineFeedViewActivity {

    @Override
    protected void showFeedInformation(Feed feed) {
        super.showFeedInformation(feed);
        setContentView(R.layout.listview_activity);

        ListView listView = (ListView) findViewById(R.id.listview);
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.onlinefeedview_header, null);
        listView.addHeaderView(header);

        listView.setAdapter(new FeedItemlistDescriptionAdapter(this, 0, feed.getItems()));

        ImageView cover = (ImageView) header.findViewById(R.id.imgvCover);
        TextView title = (TextView) header.findViewById(R.id.txtvTitle);
        TextView description = (TextView) header.findViewById(R.id.txtvDescription);

        if (feed.getImage() != null) {
            ImageDiskCache.getDefaultInstance().loadThumbnailBitmap(feed.getImage().getDownload_url(), cover, (int) getResources().getDimension(
                    R.dimen.thumbnail_length));
        }
        title.setText(feed.getTitle());
        description.setText(feed.getDescription());


    }
}

