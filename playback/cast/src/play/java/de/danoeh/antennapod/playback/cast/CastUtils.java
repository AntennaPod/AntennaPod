package de.danoeh.antennapod.playback.cast;

import android.content.ContentResolver;
import android.util.Log;
import android.text.TextUtils;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.images.WebImage;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.model.playback.RemoteMedia;

import java.util.List;

/**
 * Helper functions for Cast support.
 */
public class CastUtils {
    private CastUtils() {
    }

    private static final String TAG = "CastUtils";

    public static final String KEY_MEDIA_ID = "de.danoeh.antennapod.core.cast.MediaId";

    public static final String KEY_EPISODE_IDENTIFIER = "de.danoeh.antennapod.core.cast.EpisodeId";
    public static final String KEY_EPISODE_LINK = "de.danoeh.antennapod.core.cast.EpisodeLink";
    public static final String KEY_STREAM_URL = "de.danoeh.antennapod.core.cast.StreamUrl";
    public static final String KEY_FEED_URL = "de.danoeh.antennapod.core.cast.FeedUrl";
    public static final String KEY_FEED_WEBSITE = "de.danoeh.antennapod.core.cast.FeedWebsite";
    public static final String KEY_EPISODE_NOTES = "de.danoeh.antennapod.core.cast.EpisodeNotes";

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

    public static boolean isCastable(Playable media, CastSession castSession) {
        if (media == null || castSession == null || castSession.getCastDevice() == null) {
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
                case AUDIO:
                    return castSession.getCastDevice().hasCapability(CastDevice.CAPABILITY_AUDIO_OUT);
                case VIDEO:
                    return castSession.getCastDevice().hasCapability(CastDevice.CAPABILITY_VIDEO_OUT);
                default:
                    return false;
            }
        }
        return false;
    }

    /**
     * Converts {@link MediaInfo} objects into the appropriate implementation of {@link Playable}.
     * @return {@link Playable} object in a format proper for casting.
     */
    public static Playable makeRemoteMedia(MediaInfo media) {
        MediaMetadata metadata = media.getMetadata();
        int version = metadata.getInt(KEY_FORMAT_VERSION);
        if (version <= 0 || version > MAX_VERSION_FORWARD_COMPATIBILITY) {
            Log.w(TAG, "MediaInfo object obtained from the cast device is not compatible with this"
                    + "version of AntennaPod CastUtils, curVer=" + FORMAT_VERSION_VALUE
                    + ", object version=" + version);
            return null;
        }
        List<WebImage> imageList = metadata.getImages();
        String imageUrl = null;
        if (!imageList.isEmpty()) {
            imageUrl = imageList.get(0).getUrl().toString();
        }
        String notes = metadata.getString(KEY_EPISODE_NOTES);
        RemoteMedia result = new RemoteMedia(media.getContentId(),
                metadata.getString(KEY_EPISODE_IDENTIFIER),
                metadata.getString(KEY_FEED_URL),
                metadata.getString(MediaMetadata.KEY_SUBTITLE),
                metadata.getString(MediaMetadata.KEY_TITLE),
                metadata.getString(KEY_EPISODE_LINK),
                metadata.getString(MediaMetadata.KEY_ARTIST),
                imageUrl,
                metadata.getString(KEY_FEED_WEBSITE),
                media.getContentType(),
                metadata.getDate(MediaMetadata.KEY_RELEASE_DATE).getTime(),
                notes);
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
        if (fi == null || metadata == null
                || !TextUtils.equals(metadata.getString(KEY_EPISODE_IDENTIFIER), fi.getItemIdentifier())) {
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
        return metadata != null
                && TextUtils.equals(metadata.getString(KEY_EPISODE_IDENTIFIER), media.getEpisodeIdentifier())
                && TextUtils.equals(metadata.getString(KEY_FEED_URL), media.getFeedUrl());
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
}
