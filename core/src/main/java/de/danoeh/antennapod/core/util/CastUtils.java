package de.danoeh.antennapod.core.util;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import java.util.Calendar;

import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Helper functions for Cast support.
 */
public class CastUtils {
    private static final String TAG = "CastUtils";

    public static final String KEY_MEDIA_ID = "AntennaPod.MediaId";

    public static final String KEY_EPISODE_IDENTIFIER = "AntennaPod.EpisodeId";
    public static final String KEY_EPISODE_LINK = "AntennaPod.EpisodeLink";
    public static final String KEY_FEED_URL = "AntennaPod.FeedUrl";
    public static final String KEY_FEED_WEBSITE = "AntennaPod.FeedWebsite";
    public static final String KEY_EPISODE_NOTES = "AntennaPod.EpisodeNotes";
    public static final int EPISODE_NOTES_MAX_LENGTH = Integer.MAX_VALUE;

    /**
     * The field <code>AntennaPod.FormatVersion</code> specifies which version of MediaMetaData
     * fields we're using. Future implementations should try to be backwards compatible with earlier
     * versions, and earlier versions should be forward compatible until the version indicated by
     * <code>MAX_VERSION_FORWARD_COMPATIBILITY</code>. If an update makes the format unreadable for
     * an earlier version, then its version number should be greater than the
     * <code>MAX_VERSION_FORWARD_COMPATIBILITY</code> value set on the earlier one, so that it
     * doesn't try to parse the object.
     */
    public static final String KEY_FORMAT_VERSION = "AntennaPod.FormatVersion";
    public static final int FORMAT_VERSION_VALUE = 1;
    public static final int MAX_VERSION_FORWARD_COMPATIBILITY = 9999;

    public static boolean isCastable(Playable media){
        if (media == null || media instanceof ExternalMedia) {
            return false;
        }
        if (media instanceof FeedMedia){
            String url = media.getStreamUrl();
            if(url == null || url.isEmpty()){
                return false;
            }
            switch (media.getMediaType()) {
                case UNKNOWN:
                    return false;
                case AUDIO:
                    return CastManager.getInstance().hasCapability(CastDevice.CAPABILITY_AUDIO_OUT, true);
                case VIDEO:
                    return CastManager.getInstance().hasCapability(CastDevice.CAPABILITY_VIDEO_OUT, true);
            }
        }
        return false;
    }

    /**
     * Converts {@link FeedMedia} objects into a format suitable for sending to a Cast Device.
     * Before using this method, one should make sure {@link #isCastable(Playable)} returns
     * {@code true}.
     *
     * Unless media.{@link FeedMedia#loadMetadata() loadMetadata()} has already been called,
     * this method should not run on the main thread.
     *
     * @param media The {@link FeedMedia} object to be converted.
     * @return {@link MediaInfo} object in a format proper for casting.
     */
    public static MediaInfo convertFromFeedMedia(FeedMedia media){
        if(media == null) {
            return null;
        }
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);
        try{
            media.loadMetadata();
        } catch (Playable.PlayableException e) {
            Log.e(TAG, "Unable to load FeedMedia metadata", e);
        }
        FeedItem feedItem = media.getItem();
        if (feedItem != null) {
            metadata.putString(MediaMetadata.KEY_TITLE, media.getEpisodeTitle());
            String subtitle = media.getFeedTitle();
            if (subtitle != null) {
                metadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);
            }
            FeedImage image = feedItem.getImage();
            if (image != null && !TextUtils.isEmpty(image.getDownload_url())) {
                metadata.addImage(new WebImage(Uri.parse(image.getDownload_url())));
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(media.getItem().getPubDate());
            metadata.putDate(MediaMetadata.KEY_RELEASE_DATE, calendar);
            Feed feed = feedItem.getFeed();
            if (feed != null) {
                if (!TextUtils.isEmpty(feed.getAuthor())) {
                    metadata.putString(MediaMetadata.KEY_ARTIST, feed.getAuthor());
                }
                if (!TextUtils.isEmpty(feed.getDownload_url())) {
                    metadata.putString(KEY_FEED_URL, feed.getDownload_url());
                }
                if (!TextUtils.isEmpty(feed.getLink())) {
                    metadata.putString(KEY_FEED_WEBSITE, feed.getLink());
                }
            }
            if (!TextUtils.isEmpty(feedItem.getItemIdentifier())) {
                metadata.putString(KEY_EPISODE_IDENTIFIER, feedItem.getItemIdentifier());
            } else {
                metadata.putString(KEY_EPISODE_IDENTIFIER, media.getStreamUrl());
            }
            if (!TextUtils.isEmpty(feedItem.getLink())) {
                metadata.putString(KEY_EPISODE_LINK, feedItem.getLink());
            }
        }
        String notes = null;
        try {
            notes = media.loadShownotes().call();
        } catch (Exception e) {
            Log.e(TAG, "Unable to load FeedMedia notes", e);
        }
        if (notes != null) {
            if (notes.length() > EPISODE_NOTES_MAX_LENGTH) {
                notes = notes.substring(0, EPISODE_NOTES_MAX_LENGTH);
            }
            metadata.putString(KEY_EPISODE_NOTES, notes);
        }
        // This field only identifies the id on the device that has the original version.
        // Idea is to perhaps, on a first approach, check if the version on the local DB with the
        // same id matches the remote object, and if not then search for episode and feed identifiers.
        // This at least should make media recognition for a single device much quicker.
        metadata.putInt(KEY_MEDIA_ID, ((Long) media.getIdentifier()).intValue());
        // A way to identify different casting media formats in case we change it in the future and
        // senders with different versions share a casting device.
        metadata.putInt(KEY_FORMAT_VERSION, FORMAT_VERSION_VALUE);

        return new MediaInfo.Builder(media.getStreamUrl())
                .setContentType(media.getMime_type())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata)
                .build();
    }



    //TODO Queue handling perhaps
}
