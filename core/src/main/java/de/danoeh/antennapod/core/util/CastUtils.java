package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;

import java.util.Calendar;

import de.danoeh.antennapod.core.cast.CastManager;
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

    public static final String CAST_APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

    public static final String KEY_MEDIA_ID = "CastUtils.Id";

    public static void initializeCastManager(Context context){
        CastManager.initialize(context, new CastConfiguration.Builder(CastUtils.CAST_APP_ID)
                .enableDebug()
                .enableWifiReconnection()
                .enableAutoReconnect()
                .build());
    }

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
            if (image != null && image.getDownload_url() != null &&
                    !image.getDownload_url().isEmpty()) {
                metadata.addImage(new WebImage(Uri.parse(image.getDownload_url())));
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(media.getItem().getPubDate());
            metadata.putDate(MediaMetadata.KEY_RELEASE_DATE, calendar);

        }
        //metadata.putString(MediaMetadata.KEY_ARTIST, null);
        metadata.putString(KEY_MEDIA_ID, media.getIdentifier().toString());

        return new MediaInfo.Builder(media.getStreamUrl())
                .setContentType(media.getMime_type())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata)
                .build();
    }

    //TODO Queue handling perhaps
}
