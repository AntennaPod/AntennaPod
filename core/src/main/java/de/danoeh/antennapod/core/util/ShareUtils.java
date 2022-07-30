package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

/** Utility methods for sharing data */
public class ShareUtils {
    private static final String TAG = "ShareUtils";

    private ShareUtils() {
    }

    public static void shareLink(@NonNull Context context, @NonNull String text) {
        Intent intent = new ShareCompat.IntentBuilder(context)
                .setType("text/plain")
                .setText(text)
                .setChooserTitle(R.string.share_url_label)
                .createChooserIntent();
        context.startActivity(intent);
    }

    public static void shareFeedLink(Context context, Feed feed) {
        String text = feed.getTitle();
        if (feed.getLink() != null) {
            text += "\n" + feed.getLink();
        }
        text += "\n\n" + context.getResources().getString(R.string.share_rss_address_label)
                + " " + feed.getDownload_url();
        shareLink(context, text);
    }

    public static boolean hasLinkToShare(FeedItem item) {
        return FeedItemUtil.getLinkWithFallback(item) != null;
    }

    public static void shareMediaDownloadLink(Context context, FeedMedia media) {
        shareLink(context, media.getDownload_url());
    }

    public static void shareFeedItemLinkWithDownloadLink(Context context, FeedItem item, boolean withPosition) {
        String text = item.getFeed().getTitle() + ": " + item.getTitle();
        int pos = 0;
        if (item.getMedia() != null && withPosition) {
            text += "\n" + context.getResources().getString(R.string.share_starting_position_label) + ": ";
            pos = item.getMedia().getPosition();
            text +=  Converter.getDurationStringLong(pos);
        }

        if (hasLinkToShare(item)) {
            text +=  "\n\n" + context.getResources().getString(R.string.share_dialog_episode_website_label) + ": ";
            text += FeedItemUtil.getLinkWithFallback(item);
        }

        if (item.getMedia() != null && item.getMedia().getDownload_url() != null) {
            text += "\n\n" + context.getResources().getString(R.string.share_dialog_media_file_label) + ": ";
            text +=  item.getMedia().getDownload_url();
            if (withPosition) {
                text += "#t=" + pos / 1000;
            }
        }
        shareLink(context, text);
    }

    public static void shareFeedItemFile(Context context, FeedMedia media) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority),
                new File(media.getLocalMediaUrl()));

        new ShareCompat.IntentBuilder(context)
                .setType(media.getMime_type())
                .addStream(fileUri)
                .setChooserTitle(R.string.share_file_label)
                .startChooser();

        Log.e(TAG, "shareFeedItemFile called");
    }
}
