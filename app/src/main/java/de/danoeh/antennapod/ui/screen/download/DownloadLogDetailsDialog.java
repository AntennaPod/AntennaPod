package de.danoeh.antennapod.ui.screen.download;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.DownloadLogDetailsDialogBinding;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import org.greenrobot.eventbus.EventBus;

public class DownloadLogDetailsDialog extends MaterialAlertDialogBuilder {

    /**
     * Called when the dialog is dismissed.
     */
    final Runnable onDismiss;

    /**
     * Creates a dialog with Feed title (and FeedItem title if possible).
     */
    public DownloadLogDetailsDialog(@NonNull Context context, DownloadResult status) {
        this(context, status, false, null);
    }

    /**
     * Creates a dialog with Feed title (and FeedItem title if possible).
     * Can show a button to jump to the feed details view.
     * Will take a callback function to call when being dismissed.
     */
    public DownloadLogDetailsDialog(@NonNull Context context, DownloadResult downloadResult, boolean isJumpToFeed,
                                    @Nullable Runnable onDismiss) {
        super(context);
        this.onDismiss = onDismiss;

        Feed feed = null;
        String podcastTitle = null;
        String episodeTitle = null;

        String url = "unknown";
        if (downloadResult.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            FeedMedia media = DBReader.getFeedMedia(downloadResult.getFeedfileId());
            if (media != null && media.getItem() != null) {
                feed = DBReader.getFeed(media.getItem().getFeedId(), false, 0, 0);
                if (feed != null) {
                    podcastTitle = feed.getFeedTitle();
                }
                episodeTitle = media.getEpisodeTitle();
                url = media.getDownloadUrl();
            }
        } else if (downloadResult.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            feed = DBReader.getFeed(downloadResult.getFeedfileId(), false, 0, 0);
            if (feed != null) {
                podcastTitle = feed.getFeedTitle();
                url = feed.getDownloadUrl();
            }
        }

        String message = context.getString(R.string.download_successful);
        if (!downloadResult.isSuccessful()) {
            message = downloadResult.getReasonDetailed();
        }

        final Boolean isSubscribed = feed != null ? feed.getState() == Feed.STATE_SUBSCRIBED : null;
        final DownloadLogDetailsDialogBinding binding =
                DownloadLogDetailsDialogBinding.inflate(LayoutInflater.from(context));
        if (!isJumpToFeed) {
            binding.btnGoToPodcast.setVisibility(View.GONE);
        } else {
            final Feed f = feed;
            binding.btnGoToPodcast.setOnClickListener(v -> {
                goToFeedOrFeedItem(context, f, isSubscribed);
                dismissThisDialog.run();
            });
        }

        if (podcastTitle != null) {
            binding.txtvPodcastLabel.setText(getContext().getString(R.string.feed_title) + ":");
            binding.txtvPodcastName.setText(podcastTitle);
        }
        if (episodeTitle != null) {
            binding.txtvEpisodeLabel.setText(getContext().getString(R.string.episode_title) + ":");
            binding.txtvEpisodeName.setText(episodeTitle);
            // Needed for the the ellipsize = marquee to work
            binding.txtvEpisodeName.setSelected(true);
        } else {
            binding.llEpisode.setVisibility(View.GONE);
        }

        final String humanReadableReason = context.getString(DownloadErrorLabel.from(downloadResult.getReason()));
        final String dialogContent = context.getString(R.string.download_log_details_message,
                humanReadableReason, message, url);

        binding.txtvLogMessage.setText(dialogContent);

        setTitle(R.string.download_error_details);
        setView(binding.getRoot());
        setPositiveButton(android.R.string.ok, null);
        setNeutralButton(R.string.copy_to_clipboard, (copyDialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(context.getString(R.string.download_error_details), dialogContent);
            clipboard.setPrimaryClip(clip);
            if (Build.VERSION.SDK_INT < 32) {
                EventBus.getDefault().post(new MessageEvent(context.getString(R.string.copied_to_clipboard)));
            }
        });
    }

    private Runnable dismissThisDialog;

    void goToFeedOrFeedItem(Context context, Feed feed, Boolean isSubscribed) {
        if (isSubscribed != null) {
            Intent intent;
            if (isSubscribed) {
                intent = new MainActivityStarter(context).withOpenFeed(feed.getId()).getIntent();
            } else {
                intent = new OnlineFeedviewActivityStarter(getContext(), feed.getDownloadUrl())
                        .getIntent();
            }
            if (onDismiss != null) {
                onDismiss.run();
            }
            context.startActivity(intent);
        }
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setTextIsSelectable(true);
        dismissThisDialog = dialog::dismiss;
        return dialog;
    }
}