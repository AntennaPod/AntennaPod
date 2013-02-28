package de.danoeh.antennapod.util.playback;

import java.util.List;

import android.content.SharedPreferences;
import android.os.Parcelable;
import android.util.Log;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.MediaType;

/** Interface for objects that can be played by the PlaybackService. */
public interface Playable extends Parcelable {

	/**
	 * Save information about the playable in a preference so that it can be
	 * restored later via PlayableUtils.createInstanceFromPreferences.
	 * Implementations must NOT call commit() after they have written the values
	 * to the preferences file.
	 */
	public void writeToPreferences(SharedPreferences.Editor prefEditor);

	/**
	 * This method is called from a separate thread by the PlaybackService.
	 * Playable objects should load their metadata in this method (for example:
	 * chapter marks).
	 */
	public void loadMetadata() throws PlayableException;

	/** Returns the title of the episode that this playable represents */
	public String getEpisodeTitle();

	/**
	 * Loads shownotes. If the shownotes have to be loaded from a file or from a
	 * database, it should be done in a separate thread. After the shownotes
	 * have been loaded, callback.onShownotesLoaded should be called.
	 */
	public void loadShownotes(ShownoteLoaderCallback callback);

	/**
	 * Returns a list of chapter marks or null if this Playable has no chapters.
	 */
	public List<Chapter> getChapters();

	/** Returns a link to a website that is meant to be shown in a browser */
	public String getWebsiteLink();

	public String getPaymentLink();

	/** Returns the title of the feed this Playable belongs to. */
	public String getFeedTitle();

	/** Returns a file url to an image or null if no such image exists. */
	public String getImageFileUrl();

	/**
	 * Returns a unique identifier, for example a file url or an ID from a
	 * database.
	 */
	public Object getIdentifier();

	/** Return duration of object or 0 if duration is unknown. */
	public int getDuration();

	/** Return position of object or 0 if position is unknown. */
	public int getPosition();

	/**
	 * Returns the type of media. This method should return the correct value
	 * BEFORE loadMetadata() is called.
	 */
	public MediaType getMediaType();

	/**
	 * Returns an url to a local file that can be played or null if this file
	 * does not exist.
	 */
	public String getLocalMediaUrl();

	/**
	 * Returns an url to a file that can be streamed by the player or null if
	 * this url is not known.
	 */
	public String getStreamUrl();

	/**
	 * Returns true if a local file that can be played is available. getFileUrl
	 * MUST return a non-null string if this method returns true.
	 */
	public boolean localFileAvailable();

	/**
	 * Returns true if a streamable file is available. getStreamUrl MUST return
	 * a non-null string if this method returns true.
	 */
	public boolean streamAvailable();

	/**
	 * Saves the current position of this object. Implementations can use the
	 * provided SharedPreference to save this information and retrieve it later
	 * via PlayableUtils.createInstanceFromPreferences.
	 */
	public void saveCurrentPosition(SharedPreferences pref, int newPosition);

	public void setPosition(int newPosition);

	public void setDuration(int newDuration);

	/** Is called by the PlaybackService when playback starts. */
	public void onPlaybackStart();

	/** Is called by the PlaybackService when playback is completed. */
	public void onPlaybackCompleted();

	/**
	 * Returns an integer that must be unique among all Playable classes. The
	 * return value is later used by PlayableUtils to determine the type of the
	 * Playable object that is restored.
	 */
	public int getPlayableType();

	public void setChapters(List<Chapter> chapters);

	/** Provides utility methods for Playable objects. */
	public static class PlayableUtils {
		private static final String TAG = "PlayableUtils";

		/**
		 * Restores a playable object from a sharedPreferences file.
		 * 
		 * @param type
		 *            An integer that represents the type of the Playable object
		 *            that is restored.
		 * @param pref
		 *            The SharedPreferences file from which the Playable object
		 *            is restored
		 * @return The restored Playable object
		 */
		public static Playable createInstanceFromPreferences(int type,
				SharedPreferences pref) {
			// ADD new Playable types here:
			switch (type) {
			case FeedMedia.PLAYABLE_TYPE_FEEDMEDIA:
				long feedId = pref.getLong(FeedMedia.PREF_FEED_ID, -1);
				long mediaId = pref.getLong(FeedMedia.PREF_MEDIA_ID, -1);
				if (feedId != -1 && mediaId != -1) {
					Feed feed = FeedManager.getInstance().getFeed(feedId);
					if (feed != null) {
						return FeedManager.getInstance().getFeedMedia(mediaId,
								feed);
					}
				}
				break;
			case ExternalMedia.PLAYABLE_TYPE_EXTERNAL_MEDIA:
				String source = pref.getString(ExternalMedia.PREF_SOURCE_URL,
						null);
				String mediaType = pref.getString(
						ExternalMedia.PREF_MEDIA_TYPE, null);
				if (source != null && mediaType != null) {
					int position = pref.getInt(ExternalMedia.PREF_POSITION, 0);
					return new ExternalMedia(source,
							MediaType.valueOf(mediaType), position);
				}
				break;
			}
			Log.e(TAG, "Could not restore Playable object from preferences");
			return null;
		}
	}

	public static class PlayableException extends Exception {
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

	public static interface ShownoteLoaderCallback {
		void onShownotesLoaded(String shownotes);
	}
}
