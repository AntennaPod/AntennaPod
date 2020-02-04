package de.danoeh.antennapod.view;

import android.graphics.Color;
import android.os.Build;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.view.LayoutInflaterCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.adapter.QueueRecyclerAdapter;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
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

/**
 * Holds the view which shows FeedItems.
 */
public class EpisodeItemViewHolder extends RecyclerView.ViewHolder
        implements QueueRecyclerAdapter.ItemTouchHelperViewHolder {
    private static final String TAG = "EpisodeItemViewHolder";

    private final View container;
    public final ImageView dragHandle;
    private final TextView placeholder;
    private final ImageView cover;
    private final TextView title;
    private final TextView pubDate;
    private final TextView position;
    private final TextView duration;
    private final TextView size;
    private final TextView isNew;
    private final ImageView isInQueue;
    private final ImageView isVideo;
    private final ImageView isFavorite;
    private final ProgressBar progressBar;
    private final ImageButton butSecondary;
    private final MainActivity activity;

    private FeedItem item;

    public EpisodeItemViewHolder(MainActivity activity, ViewGroup parent) {
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
        pubDate = itemView.findViewById(R.id.txtvPubDate);
        position = itemView.findViewById(R.id.txtvPosition);
        duration = itemView.findViewById(R.id.txtvDuration);
        butSecondary = itemView.findViewById(R.id.butSecondaryAction);
        progressBar = itemView.findViewById(R.id.progressBar);
        isInQueue = itemView.findViewById(R.id.ivInPlaylist);
        isVideo = itemView.findViewById(R.id.ivIsVideo);
        isNew = itemView.findViewById(R.id.statusUnread);
        isFavorite = itemView.findViewById(R.id.isFavorite);
        size = itemView.findViewById(R.id.size);
        itemView.setTag(this);
    }

    @Override
    public void onItemSelected() {
        itemView.setAlpha(0.5f);
    }

    @Override
    public void onItemClear() {
        itemView.setAlpha(1.0f);
    }

    public void bind(FeedItem item) {
        this.item = item;
        placeholder.setText(item.getFeed().getTitle());
        title.setText(item.getTitle());
        title.setText(item.getTitle());
        pubDate.setText(DateUtils.formatAbbrev(activity, item.getPubDate()));
        isNew.setVisibility(item.isNew() ? View.VISIBLE : View.GONE);
        isFavorite.setVisibility(item.isTagged(FeedItem.TAG_FAVORITE) ? View.VISIBLE : View.GONE);
        isInQueue.setVisibility(item.isTagged(FeedItem.TAG_QUEUE) ? View.VISIBLE : View.GONE);
        itemView.setAlpha(item.isPlayed() /*&& makePlayedItemsTransparent*/ ? 0.5f : 1.0f);

        if (item.getMedia() != null) {
            bind(item.getMedia());
        }

        ItemActionButton actionButton = ItemActionButton.forItem(item, true);
        actionButton.configure(butSecondary, activity);
        butSecondary.setFocusable(false);
        butSecondary.setTag(item);

        new CoverLoader(activity)
                .withUri(ImageResourceUtils.getImageLocation(item))
                .withFallbackUri(item.getFeed().getImageLocation())
                .withPlaceholderView(placeholder)
                .withCoverView(cover)
                .load();
    }

    private void bind(FeedMedia media) {
        isVideo.setVisibility(media.getMediaType() == MediaType.VIDEO ? View.VISIBLE : View.GONE);
        duration.setText(Converter.getDurationStringLong(media.getDuration()));

        if (media.isCurrentlyPlaying()) {
            container.setBackgroundColor(ThemeUtils.getColorFromAttr(activity, R.attr.currently_playing_background));
        } else {
            container.setBackgroundColor(Color.TRANSPARENT);
        }

        final DownloadRequest downloadRequest = DownloadRequester.getInstance().getRequestFor(media);
        progressBar.setVisibility(View.GONE);
        position.setVisibility(View.GONE);
        if (downloadRequest != null) {
            position.setText(Converter.byteToString(downloadRequest.getSoFar()));
            if (downloadRequest.getSize() > 0) {
                duration.setText(Converter.byteToString(downloadRequest.getSize()));
            } else {
                duration.setText(Converter.byteToString(media.getSize()));
            }
            progressBar.setProgress(downloadRequest.getProgressPercent());
            progressBar.setVisibility(View.VISIBLE);
            position.setVisibility(View.VISIBLE);
        } else if (item.getState() == FeedItem.State.PLAYING || item.getState() == FeedItem.State.IN_PROGRESS) {
            if (media.getDuration() > 0) {
                int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                progressBar.setProgress(progress);
                progressBar.setVisibility(View.VISIBLE);
                position.setVisibility(View.VISIBLE);
                position.setText(Converter.getDurationStringLong(media.getPosition()));
                duration.setText(Converter.getDurationStringLong(media.getDuration()));
            }
        }

        if (media.getSize() > 0) {
            size.setText(Converter.byteToString(media.getSize()));
        } else if (NetworkUtils.isEpisodeHeadDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
            size.setText("{fa-spinner}");
            Iconify.addIcons(size);
            NetworkUtils.getFeedMediaSizeObservable(media).subscribe(
                    sizeValue -> {
                        if (sizeValue > 0) {
                            size.setText(Converter.byteToString(sizeValue));
                        } else {
                            size.setText("");
                        }
                    }, error -> {
                        size.setText("");
                        Log.e(TAG, Log.getStackTraceString(error));
                    });
        } else {
            size.setText("");
        }
    }

    public boolean isCurrentlyPlayingItem() {
        return item.getMedia() != null && item.getMedia().isCurrentlyPlaying();
    }

    public void notifyPlaybackPositionUpdated(PlaybackPositionEvent event) {
        progressBar.setProgress((int) (100.0 * event.getPosition() / event.getDuration()));
        position.setText(Converter.getDurationStringLong(event.getPosition()));
        duration.setText(Converter.getDurationStringLong(event.getDuration()));
    }
}
