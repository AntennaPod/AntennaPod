package de.danoeh.antennapod.view.viewholder;

import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.core.feed.Feed;

/**
 * Holds the view which shows feeds.
 */
public class FeedViewHolder extends FeedComponentViewHolder {
    private static final String TAG = "FeedViewHolder";

    private final TextView placeholder;
    private final ImageView cover;
    private final TextView title;
    public final CardView coverHolder;

    private final MainActivity activity;
    private Feed feed;

    public FeedViewHolder(MainActivity activity, ViewGroup parent) {
        super(LayoutInflater.from(activity).inflate(R.layout.feeditemlist_item, parent, false));
        this.activity = activity;
        placeholder = itemView.findViewById(R.id.txtvPlaceholder);
        cover = itemView.findViewById(R.id.imgvCover);
        coverHolder = itemView.findViewById(R.id.coverHolder);
        title = itemView.findViewById(R.id.txtvTitle);
        if (Build.VERSION.SDK_INT >= 23) {
            title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        }

        itemView.findViewById(R.id.secondaryActionButton).setVisibility(View.GONE);
        itemView.findViewById(R.id.status).setVisibility(View.GONE);
        itemView.findViewById(R.id.progress).setVisibility(View.GONE);
        itemView.findViewById(R.id.drag_handle).setVisibility(View.GONE);
        itemView.setTag(this);
    }

    public void bind(Feed feed) {
        this.feed = feed;
        placeholder.setText(feed.getTitle());
        title.setText(feed.getTitle());

        if (coverHolder.getVisibility() == View.VISIBLE) {
            new CoverLoader(activity)
                    .withUri(feed.getImageLocation())
                    .withPlaceholderView(placeholder)
                    .withCoverView(cover)
                    .load();
        }
    }

}
