package de.danoeh.antennapod.view.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.util.DateFormatter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.common.SquareImageView;

public class HorizontalItemViewHolder extends RecyclerView.ViewHolder {
    public ImageView secondaryActionButton;
    public View card;
    private final SquareImageView cover;
    private final TextView title;
    private final TextView date;
    private final ImageView secondaryActionIcon;
    private final ProgressBar progressBar;

    private final MainActivity activity;
    private FeedItem item;

    public HorizontalItemViewHolder(MainActivity activity, ViewGroup parent) {
        super(LayoutInflater.from(activity).inflate(R.layout.horizontal_itemlist_item, parent, false));
        this.activity = activity;

        card = itemView.findViewById(R.id.card);
        cover = itemView.findViewById(R.id.cover);
        title = itemView.findViewById(R.id.titleLabel);
        date = itemView.findViewById(R.id.dateLabel);
        secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
        progressBar = itemView.findViewById(R.id.progressBar);
        itemView.setTag(this);
    }

    public void bind(FeedItem item) {
        this.item = item;

        new CoverLoader(activity)
                .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                .withFallbackUri(item.getFeed().getImageUrl())
                .withCoverView(cover)
                .load();
        title.setText(item.getTitle());
        date.setText(DateFormatter.formatAbbrev(activity, item.getPubDate()));
        ItemActionButton actionButton = ItemActionButton.forItem(item);
        actionButton.configure(secondaryActionButton, secondaryActionIcon, activity);
        secondaryActionButton.setFocusable(false);

        FeedMedia media = item.getMedia();
        if (media != null) {
            if (DownloadService.isDownloadingFile(media.getDownload_url())) {
                final DownloadRequest downloadRequest = DownloadService.findRequest(media.getDownload_url());
                progressBar.setProgress(Math.max(downloadRequest.getProgressPercent(), 2));
            } else {
                int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                progressBar.setProgress(progress);
            }
        }
    }

    public boolean isCurrentlyPlayingItem() {
        return item.getMedia() != null && FeedItemUtil.isCurrentlyPlaying(item.getMedia());
    }

    public void notifyPlaybackPositionUpdated(PlaybackPositionEvent event) {
        progressBar.setProgress((int) (100.0 * event.getPosition() / event.getDuration()));
    }
}