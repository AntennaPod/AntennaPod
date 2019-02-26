package de.danoeh.antennapod.core.util.playback;

import android.media.MediaPlayer;
import android.util.Log;

public class VideoPlayer extends MediaPlayer implements IPlayer {
	private static final String TAG = "VideoPlayer";

	@Override
	public boolean canSetSpeed() {
		return false;
	}

	@Override
	public boolean canDownmix() {
		return false;
	}

	@Override
	public float getCurrentSpeedMultiplier() {
		return 1;
	}

	@Override
	public void setPlaybackParams(float speed, boolean skipSilence) {
		//Ignore this for non ExoPlayer implementations
	}

	@Override
	public void setDownmix(boolean b) {
		Log.e(TAG, "Setting downmix unsupported in video player");
		throw new UnsupportedOperationException("Setting downmix unsupported in video player");
	}

    @Override
    public void setVideoScalingMode(int mode) {
        super.setVideoScalingMode(mode);
    }
}
