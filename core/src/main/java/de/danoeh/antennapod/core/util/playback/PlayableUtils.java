package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.storage.DBReader;

/**
 * Provides utility methods for Playable objects.
 */
public abstract class PlayableUtils {

    private static final String TAG = "PlayableUtils";

    /**
     * Restores a playable object from a sharedPreferences file. This method might load data from the database,
     * depending on the type of playable that was restored.
     *
     * @return The restored Playable object
     */
    @Nullable
    public static Playable createInstanceFromPreferences(@NonNull Context context) {
        long currentlyPlayingMedia = PlaybackPreferences.getCurrentlyPlayingMediaType();
        if (currentlyPlayingMedia != PlaybackPreferences.NO_MEDIA_PLAYING) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            return PlayableUtils.createInstanceFromPreferences((int) currentlyPlayingMedia, prefs);
        }
        return null;
    }

    /**
     * Restores a playable object from a sharedPreferences file. This method might load data from the database,
     * depending on the type of playable that was restored.
     *
     * @param type An integer that represents the type of the Playable object
     *             that is restored.
     * @param pref The SharedPreferences file from which the Playable object
     *             is restored
     * @return The restored Playable object
     */
    private static Playable createInstanceFromPreferences(int type, SharedPreferences pref) {
        Playable result;
        // ADD new Playable types here:
        switch (type) {
            case FeedMedia.PLAYABLE_TYPE_FEEDMEDIA:
                result = createFeedMediaInstance(pref);
                break;
            case ExternalMedia.PLAYABLE_TYPE_EXTERNAL_MEDIA:
                result = createExternalMediaInstance(pref);
                break;
            default:
                result = null;
                break;
        }
        if (result == null) {
            Log.e(TAG, "Could not restore Playable object from preferences");
        }
        return result;
    }

    private static Playable createFeedMediaInstance(SharedPreferences pref) {
        Playable result = null;
        long mediaId = pref.getLong(FeedMedia.PREF_MEDIA_ID, -1);
        if (mediaId != -1) {
            result =  DBReader.getFeedMedia(mediaId);
        }
        return result;
    }

    private static Playable createExternalMediaInstance(SharedPreferences pref) {
        Playable result = null;
        String source = pref.getString(ExternalMedia.PREF_SOURCE_URL, null);
        String mediaType = pref.getString(ExternalMedia.PREF_MEDIA_TYPE, null);
        if (source != null && mediaType != null) {
            int position = pref.getInt(ExternalMedia.PREF_POSITION, 0);
            long lastPlayedTime = pref.getLong(ExternalMedia.PREF_LAST_PLAYED_TIME, 0);
            result = new ExternalMedia(source, MediaType.valueOf(mediaType),
                    position, lastPlayedTime);
        }
        return result;
    }
}
