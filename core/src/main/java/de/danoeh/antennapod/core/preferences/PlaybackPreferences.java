package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.core.BuildConfig;

/**
 * Provides access to preferences set by the playback service. A private
 * instance of this class must first be instantiated via createInstance() or
 * otherwise every public method will throw an Exception when called.
 */
public class PlaybackPreferences implements
		SharedPreferences.OnSharedPreferenceChangeListener {
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

	/** True if last played media was streamed. */
	public static final String PREF_CURRENT_EPISODE_IS_STREAM = "de.danoeh.antennapod.preferences.lastIsStream";

	/** True if last played media was a video. */
	public static final String PREF_CURRENT_EPISODE_IS_VIDEO = "de.danoeh.antennapod.preferences.lastIsVideo";

	/** Value of PREF_CURRENTLY_PLAYING_MEDIA if no media is playing. */
	public static final long NO_MEDIA_PLAYING = -1;

	private long currentlyPlayingFeedId;
	private long currentlyPlayingFeedMediaId;
	private long currentlyPlayingMedia;
	private boolean currentEpisodeIsStream;
	private boolean currentEpisodeIsVideo;

	private static PlaybackPreferences instance;
	private Context context;

	private PlaybackPreferences(Context context) {
		this.context = context;
		loadPreferences();
	}

	/**
	 * Sets up the UserPreferences class.
	 * 
	 * @throws IllegalArgumentException
	 *             if context is null
	 * */
	public static void createInstance(Context context) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Creating new instance of UserPreferences");
		Validate.notNull(context);

		instance = new PlaybackPreferences(context);

		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(instance);
	}

	private void loadPreferences() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		currentlyPlayingFeedId = sp.getLong(PREF_CURRENTLY_PLAYING_FEED_ID, -1);
		currentlyPlayingFeedMediaId = sp.getLong(
				PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, NO_MEDIA_PLAYING);
		currentlyPlayingMedia = sp.getLong(PREF_CURRENTLY_PLAYING_MEDIA,
				NO_MEDIA_PLAYING);
		currentEpisodeIsStream = sp.getBoolean(PREF_CURRENT_EPISODE_IS_STREAM, true);
		currentEpisodeIsVideo = sp.getBoolean(PREF_CURRENT_EPISODE_IS_VIDEO, false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if (key.equals(PREF_CURRENTLY_PLAYING_FEED_ID)) {
			currentlyPlayingFeedId = sp.getLong(PREF_CURRENTLY_PLAYING_FEED_ID,
					-1);

		} else if (key.equals(PREF_CURRENTLY_PLAYING_MEDIA)) {
			currentlyPlayingMedia = sp
					.getLong(PREF_CURRENTLY_PLAYING_MEDIA, -1);

		} else if (key.equals(PREF_CURRENT_EPISODE_IS_STREAM)) {
			currentEpisodeIsStream = sp.getBoolean(PREF_CURRENT_EPISODE_IS_STREAM, true);

		} else if (key.equals(PREF_CURRENT_EPISODE_IS_VIDEO)) {
			currentEpisodeIsVideo = sp.getBoolean(PREF_CURRENT_EPISODE_IS_VIDEO, false);

		} else if (key.equals(PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID)) {
			currentlyPlayingFeedMediaId = sp.getLong(
					PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID, NO_MEDIA_PLAYING);
		}
	}

	private static void instanceAvailable() {
		if (instance == null) {
			throw new IllegalStateException(
					"UserPreferences was used before being set up");
		}
	}


	public static long getLastPlayedFeedId() {
		instanceAvailable();
		return instance.currentlyPlayingFeedId;
	}

	public static long getCurrentlyPlayingMedia() {
		instanceAvailable();
		return instance.currentlyPlayingMedia;
	}

	public static long getCurrentlyPlayingFeedMediaId() {
		return instance.currentlyPlayingFeedMediaId;
	}

	public static boolean getCurrentEpisodeIsStream() {
		instanceAvailable();
		return instance.currentEpisodeIsStream;
	}

	public static boolean getCurrentEpisodeIsVideo() {
		instanceAvailable();
		return instance.currentEpisodeIsVideo;
	}

}
