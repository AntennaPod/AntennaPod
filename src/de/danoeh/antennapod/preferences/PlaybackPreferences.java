package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;

/**
 * Provides access to preferences set by the playback service. A private
 * instance of this class must first be instantiated via createInstance() or
 * otherwise every public method will throw an Exception when called.
 */
public class PlaybackPreferences implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "PlaybackPreferences";

	/** Contains the id of the media that was played last. */
	public static final String PREF_LAST_PLAYED_ID = "de.danoeh.antennapod.preferences.lastPlayedId";

	/** Contains the feed id of the last played item. */
	public static final String PREF_LAST_PLAYED_FEED_ID = "de.danoeh.antennapod.preferences.lastPlayedFeedId";

	/**
	 * ID of the media object that is currently being played. This preference is
	 * set to NO_MEDIA_PLAYING after playback has been completed and is set as
	 * soon as the 'play' button is pressed.
	 */
	public static final String PREF_CURRENTLY_PLAYING_MEDIA = "de.danoeh.antennapod.preferences.currentlyPlayingMedia";

	/** True if last played media was streamed. */
	public static final String PREF_LAST_IS_STREAM = "de.danoeh.antennapod.preferences.lastIsStream";

	/** True if last played media was a video. */
	public static final String PREF_LAST_IS_VIDEO = "de.danoeh.antennapod.preferences.lastIsVideo";

	/** True if playback of last played media has been completed. */
	public static final String PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED = "de.danoeh.antennapod.preferences.lastPlaybackCompleted";

	/**
	 * ID of the last played media which should be auto-deleted as soon as
	 * PREF_LAST_PLAYED_ID changes.
	 */
	public static final String PREF_AUTODELETE_MEDIA_ID = "de.danoeh.antennapod.preferences.autoDeleteMediaId";

	/** Value of PREF_CURRENTLY_PLAYING_MEDIA if no media is playing. */
	public static final long NO_MEDIA_PLAYING = -1;

	private long lastPlayedId;
	private long lastPlayedFeedId;
	private long currentlyPlayingMedia;
	private boolean lastIsStream;
	private boolean lastIsVideo;
	private boolean autoDeleteMediaPlaybackCompleted;
	private long autoDeleteMediaId;

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
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating new instance of UserPreferences");
		if (context == null)
			throw new IllegalArgumentException("Context must not be null");
		instance = new PlaybackPreferences(context);

		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(instance);
	}

	private void loadPreferences() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		lastPlayedId = sp.getLong(PREF_LAST_PLAYED_ID, -1);
		lastPlayedFeedId = sp.getLong(PREF_LAST_PLAYED_FEED_ID, -1);
		currentlyPlayingMedia = sp.getLong(PREF_CURRENTLY_PLAYING_MEDIA,
				NO_MEDIA_PLAYING);
		lastIsStream = sp.getBoolean(PREF_LAST_IS_STREAM, true);
		lastIsVideo = sp.getBoolean(PREF_LAST_IS_VIDEO, false);
		autoDeleteMediaPlaybackCompleted = sp.getBoolean(
				PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED, false);
		autoDeleteMediaId = sp.getLong(PREF_AUTODELETE_MEDIA_ID, -1);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if (key.equals(PREF_LAST_PLAYED_ID)) {
			lastPlayedId = sp.getLong(PREF_LAST_PLAYED_ID, -1);
			long mediaId = sp.getLong(
					PlaybackPreferences.PREF_AUTODELETE_MEDIA_ID, -1);
			if (mediaId != -1) {
				FeedManager manager = FeedManager.getInstance();
				FeedMedia media = manager.getFeedMedia(mediaId);
				if (media != null) {
					manager.autoDeleteIfPossible(context, media);
				}
			}
		} else if (key.equals(PREF_LAST_PLAYED_FEED_ID)) {
			lastPlayedFeedId = sp.getLong(PREF_LAST_PLAYED_FEED_ID, -1);

		} else if (key.equals(PREF_CURRENTLY_PLAYING_MEDIA)) {
			currentlyPlayingMedia = sp
					.getLong(PREF_CURRENTLY_PLAYING_MEDIA, -1);

		} else if (key.equals(PREF_LAST_IS_STREAM)) {
			lastIsStream = sp.getBoolean(PREF_LAST_IS_STREAM, true);

		} else if (key.equals(PREF_LAST_IS_VIDEO)) {
			lastIsVideo = sp.getBoolean(PREF_LAST_IS_VIDEO, false);

		} else if (key.equals(PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED)) {
			autoDeleteMediaPlaybackCompleted = sp.getBoolean(
					PREF_AUTODELETE_MEDIA_ID, false);
		} else if (key.equals(PREF_AUTODELETE_MEDIA_ID)) {
			autoDeleteMediaId = sp.getLong(PREF_AUTODELETE_MEDIA_ID, -1);
		}
	}

	private static void instanceAvailable() {
		if (instance == null) {
			throw new IllegalStateException(
					"UserPreferences was used before being set up");
		}
	}

	public static long getLastPlayedId() {
		instanceAvailable();
		return instance.lastPlayedId;
	}

	public static long getAutoDeleteMediaId() {
		return instance.autoDeleteMediaId;
	}

	public static long getLastPlayedFeedId() {
		instanceAvailable();
		return instance.lastPlayedFeedId;
	}

	public static long getCurrentlyPlayingMedia() {
		instanceAvailable();
		return instance.currentlyPlayingMedia;
	}

	public static boolean isLastIsStream() {
		instanceAvailable();
		return instance.lastIsStream;
	}

	public static boolean isLastIsVideo() {
		instanceAvailable();
		return instance.lastIsVideo;
	}

	public static boolean isAutoDeleteMediaPlaybackCompleted() {
		instanceAvailable();
		return instance.autoDeleteMediaPlaybackCompleted;
	}

}
