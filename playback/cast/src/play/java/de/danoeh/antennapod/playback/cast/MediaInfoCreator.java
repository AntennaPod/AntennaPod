package de.danoeh.antennapod.playback.cast;

import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.RemoteMedia;
import java.util.Calendar;

public class MediaInfoCreator {
    public static MediaInfo from(RemoteMedia media) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);

        metadata.putString(MediaMetadata.KEY_TITLE, media.getEpisodeTitle());
        metadata.putString(MediaMetadata.KEY_SUBTITLE, media.getFeedTitle());
        if (!TextUtils.isEmpty(media.getImageLocation())) {
            metadata.addImage(new WebImage(Uri.parse(media.getImageLocation())));
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(media.getPubDate());
        metadata.putDate(MediaMetadata.KEY_RELEASE_DATE, calendar);
        if (!TextUtils.isEmpty(media.getFeedAuthor())) {
            metadata.putString(MediaMetadata.KEY_ARTIST, media.getFeedAuthor());
        }
        if (!TextUtils.isEmpty(media.getFeedUrl())) {
            metadata.putString(CastUtils.KEY_FEED_URL, media.getFeedUrl());
        }
        if (!TextUtils.isEmpty(media.getFeedLink())) {
            metadata.putString(CastUtils.KEY_FEED_WEBSITE, media.getFeedLink());
        }
        if (!TextUtils.isEmpty(media.getEpisodeIdentifier())) {
            metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, media.getEpisodeIdentifier());
        } else {
            metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, media.getDownloadUrl());
        }
        if (!TextUtils.isEmpty(media.getEpisodeLink())) {
            metadata.putString(CastUtils.KEY_EPISODE_LINK, media.getEpisodeLink());
        }
        String notes = media.getNotes();
        if (notes != null) {
            metadata.putString(CastUtils.KEY_EPISODE_NOTES, notes);
        }
        // Default id value
        metadata.putInt(CastUtils.KEY_MEDIA_ID, 0);
        metadata.putInt(CastUtils.KEY_FORMAT_VERSION, CastUtils.FORMAT_VERSION_VALUE);
        metadata.putString(CastUtils.KEY_STREAM_URL, media.getStreamUrl());

        MediaInfo.Builder builder = new MediaInfo.Builder(media.getDownloadUrl())
                .setContentType(media.getMimeType())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata);
        if (media.getDuration() > 0) {
            builder.setStreamDuration(media.getDuration());
        }
        return builder.build();
    }

    /**
     * Converts {@link FeedMedia} objects into a format suitable for sending to a Cast Device.
     * Before using this method, one should make sure isCastable(Playable) returns
     * {@code true}. This method should not run on the main thread.
     *
     * @param media The {@link FeedMedia} object to be converted.
     * @return {@link MediaInfo} object in a format proper for casting.
     */
    public static MediaInfo from(FeedMedia media) {
        if (media == null) {
            return null;
        }
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
        if (media.getItem() == null) {
            throw new IllegalStateException("item is null");
        }
        FeedItem feedItem = media.getItem();
        if (feedItem != null) {
            metadata.putString(MediaMetadata.KEY_TITLE, media.getEpisodeTitle());
            String subtitle = media.getFeedTitle();
            if (subtitle != null) {
                metadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
            }

            final @Nullable Feed feed = feedItem.getFeed();
            // Manual because cast does not support embedded images
            String url = (feedItem.getImageUrl() == null && feed != null) ? feed.getImageUrl() : feedItem.getImageUrl();
            if (!TextUtils.isEmpty(url)) {
                metadata.addImage(new WebImage(Uri.parse(url)));
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(media.getItem().getPubDate());
            metadata.putDate(MediaMetadata.KEY_RELEASE_DATE, calendar);
            if (feed != null) {
                if (!TextUtils.isEmpty(feed.getAuthor())) {
                    metadata.putString(MediaMetadata.KEY_ARTIST, feed.getAuthor());
                }
                if (!TextUtils.isEmpty(feed.getDownload_url())) {
                    metadata.putString(CastUtils.KEY_FEED_URL, feed.getDownload_url());
                }
                if (!TextUtils.isEmpty(feed.getLink())) {
                    metadata.putString(CastUtils.KEY_FEED_WEBSITE, feed.getLink());
                }
            }
            if (!TextUtils.isEmpty(feedItem.getItemIdentifier())) {
                metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, feedItem.getItemIdentifier());
            } else {
                metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, media.getStreamUrl());
            }
            if (!TextUtils.isEmpty(feedItem.getLink())) {
                metadata.putString(CastUtils.KEY_EPISODE_LINK, feedItem.getLink());
            }
        }
        // This field only identifies the id on the device that has the original version.
        // Idea is to perhaps, on a first approach, check if the version on the local DB with the
        // same id matches the remote object, and if not then search for episode and feed identifiers.
        // This at least should make media recognition for a single device much quicker.
        metadata.putInt(CastUtils.KEY_MEDIA_ID, ((Long) media.getIdentifier()).intValue());
        // A way to identify different casting media formats in case we change it in the future and
        // senders with different versions share a casting device.
        metadata.putInt(CastUtils.KEY_FORMAT_VERSION, CastUtils.FORMAT_VERSION_VALUE);
        metadata.putString(CastUtils.KEY_STREAM_URL, media.getStreamUrl());

        MediaInfo.Builder builder = new MediaInfo.Builder(media.getStreamUrl())
                .setContentType(media.getMime_type())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata);
        if (media.getDuration() > 0) {
            builder.setStreamDuration(media.getDuration());
        }
        return builder.build();
    }
}
