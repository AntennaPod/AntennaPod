package de.danoeh.antennapod.view;

import android.graphics.Color;
import android.os.Build;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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

    private final FrameLayout container;
    public final ImageView dragHandle;
    private final TextView placeholder;
    private final ImageView cover;
    private final TextView title;
    private final TextView pubDate;
    private final TextView progressLeft;
    private final TextView progressRight;
    private final ProgressBar progressBar;
    private final ImageButton butSecondary;
    private final MainActivity activity;

    private FeedItem item;

    public EpisodeItemViewHolder(MainActivity activity) {
        super(View.inflate(activity, R.layout.queue_listitem, null));
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
        progressLeft = itemView.findViewById(R.id.txtvProgressLeft);
        progressRight = itemView.findViewById(R.id.txtvProgressRight);
        butSecondary = itemView.findViewById(R.id.butSecondaryAction);
        progressBar = itemView.findViewById(R.id.progressBar);
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
        pubDate.setText(formatPubDate());

        FeedMedia media = item.getMedia();
        if (media != null) {
            final DownloadRequest downloadRequest = DownloadRequester.getInstance().getRequestFor(media);
            FeedItem.State state = item.getState();
            if (downloadRequest != null) {
                progressLeft.setText(Converter.byteToString(downloadRequest.getSoFar()));
                if (downloadRequest.getSize() > 0) {
                    progressRight.setText(Converter.byteToString(downloadRequest.getSize()));
                } else {
                    progressRight.setText(Converter.byteToString(media.getSize()));
                }
                progressBar.setProgress(downloadRequest.getProgressPercent());
                progressBar.setVisibility(View.VISIBLE);
            } else if (state == FeedItem.State.PLAYING || state == FeedItem.State.IN_PROGRESS) {
                if (media.getDuration() > 0) {
                    int progress = (int) (100.0 * media.getPosition() / media.getDuration());
                    progressBar.setProgress(progress);
                    progressBar.setVisibility(View.VISIBLE);
                    progressLeft.setText(Converter.getDurationStringLong(media.getPosition()));
                    progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                }
            } else {
                if (media.getSize() > 0) {
                    progressLeft.setText(Converter.byteToString(media.getSize()));
                } else if (NetworkUtils.isEpisodeHeadDownloadAllowed() && !media.checkedOnSizeButUnknown()) {
                    progressLeft.setText("{fa-spinner}");
                    Iconify.addIcons(progressLeft);
                    NetworkUtils.getFeedMediaSizeObservable(media).subscribe(
                            size -> {
                                if (size > 0) {
                                    progressLeft.setText(Converter.byteToString(size));
                                } else {
                                    progressLeft.setText("");
                                }
                            }, error -> {
                                progressLeft.setText("");
                                Log.e(TAG, Log.getStackTraceString(error));
                            });
                } else {
                    progressLeft.setText("");
                }
                progressRight.setText(Converter.getDurationStringLong(media.getDuration()));
                progressBar.setVisibility(View.INVISIBLE);
            }

            if (media.isCurrentlyPlaying()) {
                container.setBackgroundColor(ThemeUtils.getColorFromAttr(activity,
                        R.attr.currently_playing_background));
            } else {
                container.setBackgroundColor(Color.TRANSPARENT);
            }
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

    private String formatPubDate() {
        String pubDateStr = DateUtils.formatAbbrev(activity, item.getPubDate());
        int index = 0;
        if (countMatches(pubDateStr, ' ') == 1 || countMatches(pubDateStr, ' ') == 2) {
            index = pubDateStr.lastIndexOf(' ');
        } else if (countMatches(pubDateStr, '.') == 2) {
            index = pubDateStr.lastIndexOf('.');
        } else if (countMatches(pubDateStr, '-') == 2) {
            index = pubDateStr.lastIndexOf('-');
        } else if (countMatches(pubDateStr, '/') == 2) {
            index = pubDateStr.lastIndexOf('/');
        }
        if (index > 0) {
            pubDateStr = pubDateStr.substring(0, index+1).trim() + "\n" + pubDateStr.substring(index+1);
        }
        return pubDateStr;
    }

    public boolean isCurrentlyPlayingItem() {
        return item.getMedia() != null && item.getMedia().isCurrentlyPlaying();
    }

    public void notifyPlaybackPositionUpdated(PlaybackPositionEvent event) {
        progressBar.setProgress((int) (100.0 * event.getPosition() / event.getDuration()));
        progressLeft.setText(Converter.getDurationStringLong(event.getPosition()));
        progressRight.setText(Converter.getDurationStringLong(event.getDuration()));
    }

    // Oh Xiaomi, I hate you so much. How did you manage to fuck this up?
    private static int countMatches(final CharSequence str, final char ch) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (ch == str.charAt(i)) {
                count++;
            }
        }
        return count;
    }
}
