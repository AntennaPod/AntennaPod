package de.danoeh.antennapod.core.cast;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import de.danoeh.antennapod.core.util.playback.RemoteMedia;
import java.util.Calendar;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Helper functions for Cast support.
 */
public class CastUtils {
    private CastUtils(){}

    private static final String TAG = "CastUtils";

    public static final String KEY_MEDIA_ID = "de.danoeh.antennapod.core.cast.MediaId";

    public static final String KEY_EPISODE_IDENTIFIER = "de.danoeh.antennapod.core.cast.EpisodeId";
    public static final String KEY_EPISODE_LINK = "de.danoeh.antennapod.core.cast.EpisodeLink";
    public static final String KEY_FEED_URL = "de.danoeh.antennapod.core.cast.FeedUrl";
    public static final String KEY_FEED_WEBSITE = "de.danoeh.antennapod.core.cast.FeedWebsite";
    public static final String KEY_EPISODE_NOTES = "de.danoeh.antennapod.core.cast.EpisodeNotes";
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
    public static final String KEY_FORMAT_VERSION = "de.danoeh.antennapod.core.cast.FormatVersion";
    public static final int FORMAT_VERSION_VALUE = 1;
    public static final int MAX_VERSION_FORWARD_COMPATIBILITY = 9999;

    public static boolean isCastable(Playable media) {
        if (media == null || media instanceof ExternalMedia) {
            return false;
        }
        if (media instanceof FeedMedia || media instanceof RemoteMedia) {
            String url = media.getStreamUrl();
            if (url == null || url.isEmpty()) {
                return false;
            }
            if (url.startsWith(ContentResolver.SCHEME_CONTENT)) {
                return false; // Local feed
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

            if (!TextUtils.isEmpty(feedItem.getImageLocation())) {
                metadata.addImage(new WebImage(Uri.parse(feedItem.getImageLocation())));
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

        MediaInfo.Builder builder = new MediaInfo.Builder(media.getStreamUrl())
                .setContentType(media.getMime_type())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata);
        if (media.getDuration() > 0) {
            builder.setStreamDuration(media.getDuration());
        }
        return builder.build();
    }

    //TODO make unit tests for all the conversion methods
    /**
     * Converts {@link MediaInfo} objects into the appropriate implementation of {@link Playable}.
     *
     * Unless <code>searchFeedMedia</code> is set to <code>false</code>, this method should not run
     * on the GUI thread.
     *
     * @param media The {@link MediaInfo} object to be converted.
     * @param searchFeedMedia If set to <code>true</code>, the database will be queried to find a
     *              {@link FeedMedia} instance that matches {@param media}.
     * @return {@link Playable} object in a format proper for casting.
     */
    public static Playable getPlayable(MediaInfo media, boolean searchFeedMedia) {
        Log.d(TAG, "getPlayable called with searchFeedMedia=" + searchFeedMedia);
        if (media == null) {
            Log.d(TAG, "MediaInfo object provided is null, not converting to any Playable instance");
            return null;
        }
        MediaMetadata metadata = media.getMetadata();
        int version = metadata.getInt(KEY_FORMAT_VERSION);
        if (version <= 0 || version > MAX_VERSION_FORWARD_COMPATIBILITY) {
            Log.w(TAG, "MediaInfo object obtained from the cast device is not compatible with this" +
                    "version of AntennaPod CastUtils, curVer=" + FORMAT_VERSION_VALUE +
                    ", object version=" + version);
            return null;
        }
        Playable result = null;
        if (searchFeedMedia) {
            long mediaId = metadata.getInt(KEY_MEDIA_ID);
            if (mediaId > 0) {
                FeedMedia fMedia = DBReader.getFeedMedia(mediaId);
                if (fMedia != null) {
                    try {
                        fMedia.loadMetadata();
                        if (matches(media, fMedia)) {
                            result = fMedia;
                            Log.d(TAG, "FeedMedia object obtained matches the MediaInfo provided. id=" + mediaId);
                        } else {
                            Log.d(TAG, "FeedMedia object obtained does NOT match the MediaInfo provided. id=" + mediaId);
                        }
                    } catch (Playable.PlayableException e) {
                        Log.e(TAG, "Unable to load FeedMedia metadata to compare with MediaInfo", e);
                    }
                } else {
                    Log.d(TAG, "Unable to find in database a FeedMedia with id=" + mediaId);
                }
            }
            if (result == null) {
                FeedItem feedItem = DBReader.getFeedItemByUrl(metadata.getString(KEY_FEED_URL),
                        metadata.getString(KEY_EPISODE_IDENTIFIER));
                if (feedItem != null) {
                    result = feedItem.getMedia();
                    Log.d(TAG, "Found episode that matches the MediaInfo provided. Using its media, if existing.");
                }
            }
        }
        if (result == null) {
            List<WebImage> imageList = metadata.getImages();
            String imageUrl = null;
            if (!imageList.isEmpty()) {
                imageUrl = imageList.get(0).getUrl().toString();
            }
            result = new RemoteMedia(media.getContentId(),
                    metadata.getString(KEY_EPISODE_IDENTIFIER),
                    metadata.getString(KEY_FEED_URL),
                    metadata.getString(MediaMetadata.KEY_SUBTITLE),
                    metadata.getString(MediaMetadata.KEY_TITLE),
                    metadata.getString(KEY_EPISODE_LINK),
                    metadata.getString(MediaMetadata.KEY_ARTIST),
                    imageUrl,
                    metadata.getString(KEY_FEED_WEBSITE),
                    media.getContentType(),
                    metadata.getDate(MediaMetadata.KEY_RELEASE_DATE).getTime());
            String notes = metadata.getString(KEY_EPISODE_NOTES);
            if (!TextUtils.isEmpty(notes)) {
                ((RemoteMedia) result).setNotes(notes);
            }
            Log.d(TAG, "Converted MediaInfo into RemoteMedia");
        }
        if (result.getDuration() == 0 && media.getStreamDuration() > 0) {
            result.setDuration((int) media.getStreamDuration());
        }
        return result;
    }

    /**
     * Compares a {@link MediaInfo} instance with a {@link FeedMedia} one and evaluates whether they
     * represent the same podcast episode.
     *
     * @param info      the {@link MediaInfo} object to be compared.
     * @param media     the {@link FeedMedia} object to be compared.
     * @return <true>true</true> if there's a match, <code>false</code> otherwise.
     *
     * @see RemoteMedia#equals(Object)
     */
    public static boolean matches(MediaInfo info, FeedMedia media) {
        if (info == null || media == null) {
            return false;
        }
        if (!TextUtils.equals(info.getContentId(), media.getStreamUrl())) {
            return false;
        }
        MediaMetadata metadata = info.getMetadata();
        FeedItem fi = media.getItem();
        if (fi == null || metadata == null ||
                !TextUtils.equals(metadata.getString(KEY_EPISODE_IDENTIFIER), fi.getItemIdentifier())) {
            return false;
        }
        Feed feed = fi.getFeed();
        return feed != null && TextUtils.equals(metadata.getString(KEY_FEED_URL), feed.getDownload_url());
    }

    /**
     * Compares a {@link MediaInfo} instance with a {@link RemoteMedia} one and evaluates whether they
     * represent the same podcast episode.
     *
     * @param info      the {@link MediaInfo} object to be compared.
     * @param media     the {@link RemoteMedia} object to be compared.
     * @return <true>true</true> if there's a match, <code>false</code> otherwise.
     *
     * @see RemoteMedia#equals(Object)
     */
    public static boolean matches(MediaInfo info, RemoteMedia media) {
        if (info == null || media == null) {
            return false;
        }
        if (!TextUtils.equals(info.getContentId(), media.getStreamUrl())) {
            return false;
        }
        MediaMetadata metadata = info.getMetadata();
        return metadata != null &&
                TextUtils.equals(metadata.getString(KEY_EPISODE_IDENTIFIER), media.getEpisodeIdentifier()) &&
                TextUtils.equals(metadata.getString(KEY_FEED_URL), media.getFeedUrl());
    }

    /**
     * Compares a {@link MediaInfo} instance with a {@link Playable} and evaluates whether they
     * represent the same podcast episode. Useful every time we get a MediaInfo from the Cast Device
     * and want to avoid unnecessary conversions.
     *
     * @param info      the {@link MediaInfo} object to be compared.
     * @param media     the {@link Playable} object to be compared.
     * @return <true>true</true> if there's a match, <code>false</code> otherwise.
     *
     * @see RemoteMedia#equals(Object)
     */
    public static boolean matches(MediaInfo info, Playable media) {
        if (info == null || media == null) {
            return false;
        }
        if (media instanceof RemoteMedia) {
            return matches(info, (RemoteMedia) media);
        }
        return media instanceof FeedMedia && matches(info, (FeedMedia) media);
    }


    //TODO Queue handling perhaps
}
