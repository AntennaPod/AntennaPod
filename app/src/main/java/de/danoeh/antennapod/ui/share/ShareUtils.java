package de.danoeh.antennapod.ui.share;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import de.danoeh.antennapod.ui.common.Converter;
import java.io.File;
import java.net.URLEncoder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

/** Utility methods for sharing data */
public class ShareUtils {
    private static final String TAG = "ShareUtils";
    private static final int ABBREVIATE_MAX_LENGTH = 50;

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
        String feedurl = URLEncoder.encode(feed.getDownloadUrl());
        feedurl = feedurl.replace("htt", "%68%74%74"); // To not confuse users by having a url inside a url
        String text = feed.getTitle() + "\n\n"
                + "https://antennapod.org/deeplink/subscribe/?url=" + feedurl
                + "&title=" + URLEncoder.encode(feed.getTitle());
        shareLink(context, text);
    }

    public static boolean hasLinkToShare(FeedItem item) {
        return item.getLinkWithFallback() != null;
    }

    public static String getSocialFeedItemShareText(Context context, FeedItem item,
                                                    boolean withPosition, boolean abbreviate) {
        String text = item.getFeed().getTitle() + ": ";

        if (abbreviate && item.getTitle().length() > ABBREVIATE_MAX_LENGTH) {
            text += item.getTitle().substring(0, ABBREVIATE_MAX_LENGTH) + "…";
        } else {
            text += item.getTitle();
        }

        if (item.getMedia() != null && withPosition) {
            text += "\n" + context.getResources().getString(R.string.share_starting_position_label) + ": ";
            text +=  Converter.getDurationStringLong(item.getMedia().getPosition());
        }

        if (hasLinkToShare(item)) {
            if (!abbreviate) {
                text += "\n";
            }
            text +=  "\n" + context.getResources().getString(R.string.share_dialog_episode_website_label) + ": ";
            if (abbreviate && item.getLinkWithFallback().length() > ABBREVIATE_MAX_LENGTH) {
                text += item.getLinkWithFallback().substring(0, ABBREVIATE_MAX_LENGTH) + "…";
            } else {
                text += item.getLinkWithFallback();
            }
        }

        if (item.getMedia() != null && item.getMedia().getDownloadUrl() != null) {
            if (!abbreviate) {
                text += "\n";
            }
            text += "\n" + context.getResources().getString(R.string.share_dialog_media_file_label) + ": ";
            if (abbreviate && item.getMedia().getDownloadUrl().length() > ABBREVIATE_MAX_LENGTH) {
                text += item.getMedia().getDownloadUrl().substring(0, ABBREVIATE_MAX_LENGTH) + "…";
            } else {
                text += item.getMedia().getDownloadUrl();
            }
            if (withPosition) {
                text += "#t=" + item.getMedia().getPosition() / 1000;
            }
        }
        return text;
    }

    public static void shareFeedItemFile(Context context, FeedMedia media) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority),
                new File(media.getLocalFileUrl()));

        new ShareCompat.IntentBuilder(context)
                .setType(media.getMimeType())
                .addStream(fileUri)
                .setChooserTitle(R.string.share_file_label)
                .startChooser();

        Log.e(TAG, "shareFeedItemFile called");
    }
}
