package de.danoeh.antennapod.view.viewholder;

import android.os.Build;
import android.text.Layout;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.elevation.SurfaceColors;
import com.joanzapata.iconify.Iconify;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.util.PlaybackStatus;
import de.danoeh.antennapod.core.util.download.MediaSizeLoader;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.core.util.DateFormatter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.CircularProgressBar;
import de.danoeh.antennapod.ui.common.ThemeUtils;

/**
 * Holds the view which shows FeedItems.
 */
public class EpisodeItemViewHolder extends RecyclerView.ViewHolder {
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
    public final ImageView isInbox;
    public final ImageView isInQueue;
    private final ImageView isVideo;
    public final ImageView isFavorite;
    private final ProgressBar progressBar;
    public final View secondaryActionButton;
    public final ImageView secondaryActionIcon;
    private final CircularProgressBar secondaryActionProgress;
    private final TextView separatorIcons;
    private final View leftPadding;
    public final CardView coverHolder;

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
        progressBar = itemView.findViewById(R.id.progressBar);
        isInQueue = itemView.findViewById(R.id.ivInPlaylist);
        isVideo = itemView.findViewById(R.id.ivIsVideo);
        isInbox = itemView.findViewById(R.id.statusInbox);
        isFavorite = itemView.findViewById(R.id.isFavorite);
        size = itemView.findViewById(R.id.size);
        separatorIcons = itemView.findViewById(R.id.separatorIcons);
        secondaryActionProgress = itemView.findViewById(R.id.secondaryActionProgress);
        secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
        coverHolder = itemView.findViewById(R.id.coverHolder);
        leftPadding = itemView.findViewById(R.id.left_padding);
        itemView.setTag(this);
    }

    public void bind(FeedItem item) {
        this.item = item;
        placeholder.setText(item.getFeed().getTitle());
        title.setText(item.getTitle());
        leftPadding.setContentDescription(item.getTitle());
        pubDate.setText(DateFormatter.formatAbbrev(activity, item.getPubDate()));
        pubDate.setContentDescription(DateFormatter.formatForAccessibility(item.getPubDate()));
        if (item.isPlayed()) {
            cover.setContentDescription(activity.getString(R.string.is_played));
            cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        } else {
            cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        isInbox.setVisibility(item.isNew() ? View.VISIBLE : View.GONE);
        isFavorite.setVisibility(item.isTagged(FeedItem.TAG_FAVORITE) ? View.VISIBLE : View.GONE);
        isInQueue.setVisibility(item.isTagged(FeedItem.TAG_QUEUE) ? View.VISIBLE : View.GONE);
        container.setAlpha(item.isPlayed() ? 0.5f : 1.0f);

        ItemActionButton actionButton = ItemActionButton.forItem(item);
        actionButton.configure(secondaryActionButton, secondaryActionIcon, activity);
        secondaryActionButton.setFocusable(false);

        if (item.getMedia() != null) {
            bind(item.getMedia());
        } else {
            secondaryActionProgress.setPercentage(0, item);
            secondaryActionProgress.setIndeterminate(false);
            isVideo.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            duration.setVisibility(View.GONE);
            position.setVisibility(View.GONE);
            itemView.setBackgroundResource(ThemeUtils.getDrawableFromAttr(activity, R.attr.selectableItemBackground));
        }

        if (coverHolder.getVisibility() == View.VISIBLE) {
            new CoverLoader(activity)
                    .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                    .withFallbackUri(item.getFeed().getImageUrl())
                    .withPlaceholderView(placeholder)
                    .withCoverView(cover)
                    .load();
        }
    }

    private void bind(FeedMedia media) {
        isVideo.setVisibility(media.getMediaType() == MediaType.VIDEO ? View.VISIBLE : View.GONE);
        duration.setVisibility(media.getDuration() > 0 ? View.VISIBLE : View.GONE);

        if (PlaybackStatus.isCurrentlyPlaying(media)) {
            float density = activity.getResources().getDisplayMetrics().density;
            itemView.setBackgroundColor(SurfaceColors.getColorForElevation(activity, 8 * density));
        } else {
            itemView.setBackgroundResource(ThemeUtils.getDrawableFromAttr(activity, R.attr.selectableItemBackground));
        }

        if (DownloadServiceInterface.get().isDownloadingEpisode(media.getDownload_url())) {
            float percent = 0.01f * DownloadServiceInterface.get().getProgress(media.getDownload_url());
            secondaryActionProgress.setPercentage(Math.max(percent, 0.01f), item);
            secondaryActionProgress.setIndeterminate(
                    DownloadServiceInterface.get().isEpisodeQueued(media.getDownload_url()));
        } else if (media.isDownloaded()) {
            secondaryActionProgress.setPercentage(1, item); // Do not animate 100% -> 0%
            secondaryActionProgress.setIndeterminate(false);
        } else {
            secondaryActionProgress.setPercentage(0, item); // Animate X% -> 0%
            secondaryActionProgress.setIndeterminate(false);
        }

        duration.setText(Converter.getDurationStringLong(media.getDuration()));
        duration.setContentDescription(activity.getString(R.string.chapter_duration,
                Converter.getDurationStringLocalized(activity, media.getDuration())));
        if (PlaybackStatus.isPlaying(item.getMedia()) || item.isInProgress()) {
            int progress = (int) (100.0 * media.getPosition() / media.getDuration());
            int remainingTime = Math.max(media.getDuration() - media.getPosition(), 0);
            progressBar.setProgress(progress);
            position.setText(Converter.getDurationStringLong(media.getPosition()));
            position.setContentDescription(activity.getString(R.string.position,
                    Converter.getDurationStringLocalized(activity, media.getPosition())));
            progressBar.setVisibility(View.VISIBLE);
            position.setVisibility(View.VISIBLE);
            if (UserPreferences.shouldShowRemainingTime()) {
                duration.setText(((remainingTime > 0) ? "-" : "") + Converter.getDurationStringLong(remainingTime));
                duration.setContentDescription(activity.getString(R.string.chapter_duration,
                        Converter.getDurationStringLocalized(activity, (media.getDuration() - media.getPosition()))));
            }
        } else {
            progressBar.setVisibility(View.GONE);
            position.setVisibility(View.GONE);
        }

        if (media.getSize() > 0) {
            size.setText(Formatter.formatShortFileSize(activity, media.getSize()));
        } else if (NetworkUtils.isEpisodeHeadDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
            size.setText("{fa-spinner}");
            Iconify.addIcons(size);
            MediaSizeLoader.getFeedMediaSizeObservable(media).subscribe(
                    sizeValue -> {
                        if (sizeValue > 0) {
                            size.setText(Formatter.formatShortFileSize(activity, sizeValue));
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

    public void bindDummy() {
        item = new FeedItem();
        container.setAlpha(0.1f);
        secondaryActionIcon.setImageDrawable(null);
        isInbox.setVisibility(View.VISIBLE);
        isVideo.setVisibility(View.GONE);
        isFavorite.setVisibility(View.GONE);
        isInQueue.setVisibility(View.GONE);
        title.setText("███████");
        pubDate.setText("████");
        duration.setText("████");
        secondaryActionProgress.setPercentage(0, null);
        secondaryActionProgress.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);
        position.setVisibility(View.GONE);
        dragHandle.setVisibility(View.GONE);
        size.setText("");
        itemView.setBackgroundResource(ThemeUtils.getDrawableFromAttr(activity, R.attr.selectableItemBackground));
        placeholder.setText("");
        if (coverHolder.getVisibility() == View.VISIBLE) {
            new CoverLoader(activity)
                    .withResource(R.color.medium_gray)
                    .withPlaceholderView(placeholder)
                    .withCoverView(cover)
                    .load();
        }
    }

    private void updateDuration(PlaybackPositionEvent event) {
        if (getFeedItem().getMedia() != null) {
            getFeedItem().getMedia().setPosition(event.getPosition());
            getFeedItem().getMedia().setDuration(event.getDuration());
        }
        int currentPosition = event.getPosition();
        int timeDuration = event.getDuration();
        int remainingTime = Math.max(timeDuration - currentPosition, 0);
        Log.d(TAG, "currentPosition " + Converter.getDurationStringLong(currentPosition));
        if (currentPosition == Playable.INVALID_TIME || timeDuration == Playable.INVALID_TIME) {
            Log.w(TAG, "Could not react to position observer update because of invalid time");
            return;
        }
        if (UserPreferences.shouldShowRemainingTime()) {
            duration.setText(((remainingTime > 0) ? "-" : "") + Converter.getDurationStringLong(remainingTime));
        } else {
            duration.setText(Converter.getDurationStringLong(timeDuration));
        }
    }

    public FeedItem getFeedItem() {
        return item;
    }

    public boolean isCurrentlyPlayingItem() {
        return item.getMedia() != null && PlaybackStatus.isCurrentlyPlaying(item.getMedia());
    }

    public void notifyPlaybackPositionUpdated(PlaybackPositionEvent event) {
        progressBar.setProgress((int) (100.0 * event.getPosition() / event.getDuration()));
        position.setText(Converter.getDurationStringLong(event.getPosition()));
        updateDuration(event);
        duration.setVisibility(View.VISIBLE); // Even if the duration was previously unknown, it is now known
    }

    /**
     * Hides the separator dot between icons and text if there are no icons.
     */
    public void hideSeparatorIfNecessary() {
        boolean hasIcons = isInbox.getVisibility() == View.VISIBLE
                || isInQueue.getVisibility() == View.VISIBLE
                || isVideo.getVisibility() == View.VISIBLE
                || isFavorite.getVisibility() == View.VISIBLE
                || isInbox.getVisibility() == View.VISIBLE;
        separatorIcons.setVisibility(hasIcons ? View.VISIBLE : View.GONE);
    }
}
