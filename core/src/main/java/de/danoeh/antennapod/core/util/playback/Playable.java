package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;
import androidx.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.core.asynctask.ImageResource;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.ShownotesProvider;

import java.util.List;

/**
 * Interface for objects that can be played by the PlaybackService.
 */
public interface Playable extends Parcelable,
        ShownotesProvider, ImageResource {

    /**
     * Save information about the playable in a preference so that it can be
     * restored later via PlayableUtils.createInstanceFromPreferences.
     * Implementations must NOT call commit() after they have written the values
     * to the preferences file.
     */
    void writeToPreferences(SharedPreferences.Editor prefEditor);

    /**
     * This method is called from a separate thread by the PlaybackService.
     * Playable objects should load their metadata in this method. This method
     * should execute as quickly as possible and NOT load chapter marks if no
     * local file is available.
     */
    void loadMetadata() throws PlayableException;

    /**
     * This method is called from a separate thread by the PlaybackService.
     * Playable objects should load their chapter marks in this method if no
     * local file was available when loadMetadata() was called.
     */
    void loadChapterMarks(Context context);

    /**
     * Returns the title of the episode that this playable represents
     */
    String getEpisodeTitle();

    /**
     * Returns a list of chapter marks or null if this Playable has no chapters.
     */
    List<Chapter> getChapters();

    /**
     * Returns a link to a website that is meant to be shown in a browser
     */
    String getWebsiteLink();

    String getPaymentLink();

    /**
     * Returns the title of the feed this Playable belongs to.
     */
    String getFeedTitle();

    /**
     * Returns a unique identifier, for example a file url or an ID from a
     * database.
     */
    Object getIdentifier();

    /**
     * Return duration of object or 0 if duration is unknown.
     */
    int getDuration();

    /**
     * Return position of object or 0 if position is unknown.
     */
    int getPosition();

    /**
     * Returns last time (in ms) when this playable was played or 0
     * if last played time is unknown.
     */
    long getLastPlayedTime();

    /**
     * Returns the type of media. This method should return the correct value
     * BEFORE loadMetadata() is called.
     */
    MediaType getMediaType();

    /**
     * Returns an url to a local file that can be played or null if this file
     * does not exist.
     */
    String getLocalMediaUrl();

    /**
     * Returns an url to a file that can be streamed by the player or null if
     * this url is not known.
     */
    String getStreamUrl();

    /**
     * Returns true if a local file that can be played is available. getFileUrl
     * MUST return a non-null string if this method returns true.
     */
    boolean localFileAvailable();

    /**
     * Returns true if a streamable file is available. getStreamUrl MUST return
     * a non-null string if this method returns true.
     */
    boolean streamAvailable();

    /**
     * Saves the current position of this object. Implementations can use the
     * provided SharedPreference to save this information and retrieve it later
     * via PlayableUtils.createInstanceFromPreferences.
     *
     * @param pref  shared prefs that might be used to store this object
     * @param newPosition  new playback position in ms
     * @param timestamp  current time in ms
     */
    void saveCurrentPosition(SharedPreferences pref, int newPosition, long timestamp);

    void setPosition(int newPosition);

    void setDuration(int newDuration);

    /**
     * @param lastPlayedTimestamp  timestamp in ms
     */
    void setLastPlayedTime(long lastPlayedTimestamp);

    /**
     * This method should be called every time playback starts on this object.
     * <p/>
     * Position held by this Playable should be set accurately before a call to this method is made.
     */
    void onPlaybackStart();

    /**
     * This method should be called every time playback pauses or stops on this object,
     * including just before a seeking operation is performed, after which a call to
     * {@link #onPlaybackStart()} should be made. If playback completes, calling this method is not
     * necessary, as long as a call to {@link #onPlaybackCompleted(Context)} is made.
     * <p/>
     * Position held by this Playable should be set accurately before a call to this method is made.
     */
    void onPlaybackPause(Context context);

    /**
     * This method should be called when playback completes for this object.
     * @param context
     */
    void onPlaybackCompleted(Context context);

    /**
     * Returns an integer that must be unique among all Playable classes. The
     * return value is later used by PlayableUtils to determine the type of the
     * Playable object that is restored.
     */
    int getPlayableType();

    void setChapters(List<Chapter> chapters);

    /**
     * Provides utility methods for Playable objects.
     */
    class PlayableUtils {
        private PlayableUtils(){}

        private static final String TAG = "PlayableUtils";

        /**
         * Restores a playable object from a sharedPreferences file. This method might load data from the database,
         * depending on the type of playable that was restored.
         *
         * @return The restored Playable object
         */
        @Nullable
        public static Playable createInstanceFromPreferences(Context context) {
            long currentlyPlayingMedia = PlaybackPreferences.getCurrentlyPlayingMediaType();
            if (currentlyPlayingMedia != PlaybackPreferences.NO_MEDIA_PLAYING) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                return PlayableUtils.createInstanceFromPreferences(context,
                        (int) currentlyPlayingMedia, prefs);
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
        public static Playable createInstanceFromPreferences(Context context, int type,
                                                             SharedPreferences pref) {
            Playable result = null;
            // ADD new Playable types here:
            switch (type) {
                case FeedMedia.PLAYABLE_TYPE_FEEDMEDIA:
                    result = createFeedMediaInstance(pref);
                    break;
                case ExternalMedia.PLAYABLE_TYPE_EXTERNAL_MEDIA:
                    result = createExternalMediaInstance(pref);
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

    class PlayableException extends Exception {
        private static final long serialVersionUID = 1L;

        public PlayableException() {
            super();
        }

        public PlayableException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public PlayableException(String detailMessage) {
            super(detailMessage);
        }

        public PlayableException(Throwable throwable) {
            super(throwable);
        }

    }
}
