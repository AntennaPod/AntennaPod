package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;

import org.antennapod.audio.MediaPlayer;

import de.danoeh.antennapod.core.preferences.UserPreferences;

public class AudioPlayer extends MediaPlayer implements IPlayer {
	private static final String TAG = "AudioPlayer";

	public AudioPlayer(Context context) {
		super(context);
		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(sonicListener);
	}

	private final SharedPreferences.OnSharedPreferenceChangeListener sonicListener =
			(sharedPreferences, key) -> {
				if (key.equals(UserPreferences.PREF_MEDIA_PLAYER)) {
					checkMpi();
				}
			};

	@Override
	public void setDisplay(@Nullable SurfaceHolder sh) {
		if (sh != null) {
			Log.e(TAG, "Setting display not supported in Audio Player");
			throw new UnsupportedOperationException("Setting display not supported in Audio Player");
		}
	}

	@Override
	protected boolean useSonic() {
		return UserPreferences.useSonic();
	}

	@Override
	protected boolean downmix() {
		return UserPreferences.stereoToMono();
	}
}
