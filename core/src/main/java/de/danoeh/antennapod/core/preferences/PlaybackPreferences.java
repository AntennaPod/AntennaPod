package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import android.util.Log;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import org.greenrobot.eventbus.EventBus;

import static de.danoeh.antennapod.model.feed.FeedPreferences.SPEED_USE_GLOBAL;

/**
 * Provides access to preferences set by the playback service. A private
 * instance of this class must first be instantiated via createInstance() or
 * otherwise every public method will throw an Exception when called.
 */
public class PlaybackPreferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PlaybackPreferences";

    /**
     * Contains the feed id of the currently playing item if it is a FeedMedia
     * object.
     */
    private static final String PREF_CURRENTLY_PLAYING_FEED_ID = "de.danoeh.antennapod.preferences.lastPlayedFeedId";

    /**
     * Contains the id of the currently playing FeedMedia object or
     * NO_MEDIA_PLAYING if the currently playing media is no FeedMedia object.
     */
    private static final String PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID
            = "de.danoeh.antennapod.preferences.lastPlayedFeedMediaId";

    /**
     * Type of the media object that is currently being played. This preference
     * is set to NO_MEDIA_PLAYING after playback has been completed and is set
     * as soon as the 'play' button is pressed.
     */
    private static final String PREF_CURRENTLY_PLAYING_MEDIA_TYPE
            = "de.danoeh.antennapod.preferences.currentlyPlayingMedia";

    /**
     * True if last played media was a video.
     */
    private static final String PREF_CURRENT_EPISODE_IS_VIDEO = "de.danoeh.antennapod.preferences.lastIsVideo";

    /**
     * The current player status as int.
     */
    private static final String PREF_CURRENT_PLAYER_STATUS = "de.danoeh.antennapod.preferences.currentPlayerStatus";

    /**
     * A temporary playback speed which overrides the per-feed playback speed for the currently playing
     * media. Considered unset if set to SPEED_USE_GLOBAL;
     */
    private static final String PREF_CURRENTLY_PLAYING_TEMPORARY_PLAYBACK_SPEED
            = "de.danoeh.antennapod.preferences.temporaryPlaybackSpeed";


    /**
     * Value of PREF_CURRENTLY_PLAYING_MEDIA if no media is playing.
     */
    public static final long NO_MEDIA_PLAYING = -1;

    /**
     * Value of PREF_CURRENT_PLAYER_STATUS if media player status is playing.
     */
    public static final int PLAYER_STATUS_PLAYING = 1;

    /**
     * Value of PREF_CURRENT_PLAYER_STATUS if media player status is paused.
     */
    public static final int PLAYER_STATUS_PAUSED = 2;

    /**
     * Value of PREF_CURRENT_PLAYER_STATUS if media player status is neither playing nor paused.
     */
    public static final int PLAYER_STATUS_OTHER = 3;

    private static PlaybackPreferences instance;
    private static SharedPreferences prefs;

    private PlaybackPreferences() {
    }

    public static void init(Context context) {
        instance = new PlaybackPreferences();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(instance);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_CURRENT_PLAYER_STATUS.equals(key)) {
            EventBus.getDefault().post(new PlayerStatusEvent());
        }
    }

    public static long getCurrentlyPlayingMediaType() {
        return prefs.getLong(PREF_CURRENTLY_PLAYING_MEDIA_TYPE, NO_MEDIA_PLAYING);
    }

    public static long getCurrentlyPlayingFeedMediaId() {
        return prefs.getLong(PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, NO_MEDIA_PLAYING);
    }

    public static boolean getCurrentEpisodeIsVideo() {
        return prefs.getBoolean(PREF_CURRENT_EPISODE_IS_VIDEO, false);
    }

    public static int getCurrentPlayerStatus() {
        return prefs.getInt(PREF_CURRENT_PLAYER_STATUS, PLAYER_STATUS_OTHER);
    }

    public static float getCurrentlyPlayingTemporaryPlaybackSpeed() {
        return prefs.getFloat(PREF_CURRENTLY_PLAYING_TEMPORARY_PLAYBACK_SPEED, SPEED_USE_GLOBAL);
    }

    public static void writeNoMediaPlaying() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_CURRENTLY_PLAYING_MEDIA_TYPE, NO_MEDIA_PLAYING);
        editor.putLong(PREF_CURRENTLY_PLAYING_FEED_ID, NO_MEDIA_PLAYING);
        editor.putLong(PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, NO_MEDIA_PLAYING);
        editor.putInt(PREF_CURRENT_PLAYER_STATUS, PLAYER_STATUS_OTHER);
        editor.apply();
    }

    public static void writeMediaPlaying(Playable playable, PlayerStatus playerStatus) {
        Log.d(TAG, "Writing playback preferences");
        SharedPreferences.Editor editor = prefs.edit();

        if (playable == null) {
            writeNoMediaPlaying();
        } else {
            editor.putLong(PREF_CURRENTLY_PLAYING_MEDIA_TYPE, playable.getPlayableType());
            editor.putBoolean(PREF_CURRENT_EPISODE_IS_VIDEO, playable.getMediaType() == MediaType.VIDEO);
            if (playable instanceof FeedMedia) {
                FeedMedia feedMedia = (FeedMedia) playable;
                editor.putLong(PREF_CURRENTLY_PLAYING_FEED_ID, feedMedia.getItem().getFeed().getId());
                editor.putLong(PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, feedMedia.getId());
            } else {
                editor.putLong(PREF_CURRENTLY_PLAYING_FEED_ID, NO_MEDIA_PLAYING);
                editor.putLong(PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, NO_MEDIA_PLAYING);
            }
            playable.writeToPreferences(editor);
        }
        editor.putInt(PREF_CURRENT_PLAYER_STATUS, getCurrentPlayerStatusAsInt(playerStatus));

        editor.apply();
    }

    public static void writePlayerStatus(PlayerStatus playerStatus) {
        Log.d(TAG, "Writing player status playback preferences");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_CURRENT_PLAYER_STATUS, getCurrentPlayerStatusAsInt(playerStatus));
        editor.apply();
    }

    public static void setCurrentlyPlayingTemporaryPlaybackSpeed(float speed) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(PREF_CURRENTLY_PLAYING_TEMPORARY_PLAYBACK_SPEED, speed);
        editor.apply();
    }

    public static void clearCurrentlyPlayingTemporaryPlaybackSpeed() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_CURRENTLY_PLAYING_TEMPORARY_PLAYBACK_SPEED);
        editor.apply();
    }

    private static int getCurrentPlayerStatusAsInt(PlayerStatus playerStatus) {
        int playerStatusAsInt;
        switch (playerStatus) {
            case PLAYING:
                playerStatusAsInt = PLAYER_STATUS_PLAYING;
                break;
            case PAUSED:
                playerStatusAsInt = PLAYER_STATUS_PAUSED;
                break;
            default:
                playerStatusAsInt = PLAYER_STATUS_OTHER;
        }
        return playerStatusAsInt;
    }

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
            return createInstanceFromPreferences((int) currentlyPlayingMedia, prefs);
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
        if (type == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA) {
            return createFeedMediaInstance(pref);
        } else {
            Log.e(TAG, "Could not restore Playable object from preferences");
            return null;
        }
    }

    private static Playable createFeedMediaInstance(SharedPreferences pref) {
        Playable result = null;
        long mediaId = pref.getLong(FeedMedia.PREF_MEDIA_ID, -1);
        if (mediaId != -1) {
            result =  DBReader.getFeedMedia(mediaId);
        }
        return result;
    }
}
