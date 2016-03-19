package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;

import java.util.Calendar;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
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
        // TODO check for cast support enabled
        VideoCastManager.initialize(context, new CastConfiguration.Builder(CastUtils.CAST_APP_ID)
                .enableDebug()
                .enableLockScreen()
                .enableNotification()
                .enableWifiReconnection()
                .enableAutoReconnect()
                .setTargetActivity(ClientConfig.castCallbacks.getCastActivity())
                .build());
        VideoCastManager.getInstance().addVideoCastConsumer(castConsumer);
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(changeListener);
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
                    return audioCapable;
                case VIDEO:
                    return videoCapable;
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


    private static SharedPreferences.OnSharedPreferenceChangeListener changeListener =
            (preference, key) -> {
                if (UserPreferences.PREF_CAST_ENABLED.equals(key)){
                    if (UserPreferences.isCastEnabled()){
                        // TODO enable all cast-related features
                    } else {
                        // TODO disable all cast-related features
                    }
                }
            };

    // Ideally, all these fields and methods should be part of the CastManager implementation
    private static boolean videoCapable = true;
    private static boolean audioCapable = true;

    public static boolean isVideoCapable(CastDevice device, boolean defaultValue){
        if (device == null) {
            return defaultValue;
        }
        return device.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT);
    }

    public static boolean isAudioCapable(CastDevice device, boolean defaultValue){
        if (device == null) {
            return defaultValue;
        }
        return device.hasCapability(CastDevice.CAPABILITY_AUDIO_OUT);
    }

    private static VideoCastConsumer castConsumer = new VideoCastConsumerImpl() {
        @Override
        public void onDeviceSelected(CastDevice device, MediaRouter.RouteInfo routeInfo) {
            // If no device is selected, we assume both audio and video are castable
            videoCapable = isVideoCapable(device, true);
            audioCapable = isAudioCapable(device, true);
        }
    };

    //TODO Queue handling perhaps
}
