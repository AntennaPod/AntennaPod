package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.danoeh.antennapod.core.feed.EventDistributor;

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
    public static final String PREF_CURRENTLY_PLAYING_FEED_ID = "de.danoeh.antennapod.preferences.lastPlayedFeedId";

    /**
     * Contains the id of the currently playing FeedMedia object or
     * NO_MEDIA_PLAYING if the currently playing media is no FeedMedia object.
     */
    public static final String PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID = "de.danoeh.antennapod.preferences.lastPlayedFeedMediaId";

    /**
     * Type of the media object that is currently being played. This preference
     * is set to NO_MEDIA_PLAYING after playback has been completed and is set
     * as soon as the 'play' button is pressed.
     */
    public static final String PREF_CURRENTLY_PLAYING_MEDIA = "de.danoeh.antennapod.preferences.currentlyPlayingMedia";

    /**
     * True if last played media was streamed.
     */
    public static final String PREF_CURRENT_EPISODE_IS_STREAM = "de.danoeh.antennapod.preferences.lastIsStream";

    /**
     * True if last played media was a video.
     */
    public static final String PREF_CURRENT_EPISODE_IS_VIDEO = "de.danoeh.antennapod.preferences.lastIsVideo";

    /**
     * The current player status as int.
     */
    public static final String PREF_CURRENT_PLAYER_STATUS = "de.danoeh.antennapod.preferences.currentPlayerStatus";

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
        if (key.equals(PREF_CURRENT_PLAYER_STATUS)) {
            EventDistributor.getInstance().sendPlayerStatusUpdateBroadcast();
        }
    }

	public static long getLastPlayedFeedId() {
		return prefs.getLong(PREF_CURRENTLY_PLAYING_FEED_ID, -1);
	}

	public static long getCurrentlyPlayingMedia() {
		return prefs.getLong(PREF_CURRENTLY_PLAYING_MEDIA, NO_MEDIA_PLAYING);
	}

	public static long getCurrentlyPlayingFeedMediaId() {
		return prefs.getLong(PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, NO_MEDIA_PLAYING);
	}

	public static boolean getCurrentEpisodeIsStream() {
		return prefs.getBoolean(PREF_CURRENT_EPISODE_IS_STREAM, true);
	}

	public static boolean getCurrentEpisodeIsVideo() {
		return prefs.getBoolean(PREF_CURRENT_EPISODE_IS_VIDEO, false);
	}

    public static int getCurrentPlayerStatus() {
        return prefs.getInt(PREF_CURRENT_PLAYER_STATUS, PLAYER_STATUS_OTHER);
    }

}
