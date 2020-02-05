package de.danoeh.antennapod.view.viewholder;

import android.os.Build;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.adapter.QueueRecyclerAdapter;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.view.CircularProgressBar;

/**
 * Holds the view which shows feeds.
 */
public class FeedViewHolder extends FeedComponentViewHolder {
    private static final String TAG = "FeedViewHolder";

    private final View container;
    public final ImageView dragHandle;
    private final TextView placeholder;
    private final ImageView cover;
    private final TextView title;
    public final View secondaryActionButton;
    public final ImageView secondaryActionIcon;
    private final CircularProgressBar secondaryActionProgress;
    public final CardView coverHolder;

    private final MainActivity activity;
    private Feed feed;

    public FeedViewHolder(MainActivity activity, ViewGroup parent) {
        super(LayoutInflater.from(activity).inflate(R.layout.feeditemlist_item, parent, false));
        this.activity = activity;
        container = itemView.findViewById(R.id.container);
        dragHandle = itemView.findViewById(R.id.drag_handle);
        placeholder = itemView.findViewById(R.id.txtvPlaceholder);
        cover = itemView.findViewById(R.id.imgvCover);
        title = itemView.findViewById(R.id.txtvTitle);
        if (Build.VERSION.SDK_INT >= 23) {
            title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        }

        itemView.findViewById(R.id.status).setVisibility(View.GONE);
        itemView.findViewById(R.id.progress).setVisibility(View.GONE);
        secondaryActionProgress = itemView.findViewById(R.id.secondaryActionProgress);
        secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
        coverHolder = itemView.findViewById(R.id.coverHolder);
        itemView.setTag(this);
    }

    public void bind(Feed feed) {
        this.feed = feed;
        placeholder.setText(feed.getTitle());
        title.setText(feed.getTitle());

        /*ItemActionButton actionButton = ItemActionButton.forItem(item, true, true);
        actionButton.configure(secondaryActionButton, secondaryActionIcon, activity);
        secondaryActionButton.setFocusable(false);
        secondaryActionProgress.setPercentage(0, null);*/

        if (coverHolder.getVisibility() == View.VISIBLE) {
            new CoverLoader(activity)
                    .withUri(feed.getImageLocation())
                    .withPlaceholderView(placeholder)
                    .withCoverView(cover)
                    .load();
        }
    }

}
