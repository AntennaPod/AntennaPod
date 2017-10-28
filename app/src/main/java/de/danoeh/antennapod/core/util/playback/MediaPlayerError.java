package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.media.MediaPlayer;

import de.danoeh.antennapod.core.R;

/** Utility class for MediaPlayer errors. */
public class MediaPlayerError {

	/** Get a human-readable string for a specific error code. */
	public static String getErrorString(Context context, int code) {
		int resId;
		switch(code) {
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			resId = R.string.playback_error_server_died;
			break;
		default:
			resId = R.string.playback_error_unknown;
			break;
		}
		return context.getString(resId);
	}
}
