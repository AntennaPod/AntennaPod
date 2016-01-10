package de.danoeh.antennapod.core.util.playback;

import android.media.MediaPlayer;
import android.util.Log;

public class VideoPlayer extends MediaPlayer implements IPlayer {
	private static final String TAG = "VideoPlayer";

	@Override
	public boolean canSetPitch() {
		return false;
	}

	@Override
	public boolean canSetSpeed() {
		return false;
	}

	@Override
	public boolean canDownmix() {
		return false;
	}

	@Override
	public float getCurrentPitchStepsAdjustment() {
		return 1;
	}

	@Override
	public float getCurrentSpeedMultiplier() {
		return 1;
	}

	@Override
	public float getMaxSpeedMultiplier() {
		return 1;
	}

	@Override
	public float getMinSpeedMultiplier() {
		return 1;
	}

	@Override
	public void setEnableSpeedAdjustment(boolean enableSpeedAdjustment) throws UnsupportedOperationException {
		Log.e(TAG, "Setting enable speed adjustment unsupported in video player");
		throw new UnsupportedOperationException("Setting enable speed adjustment unsupported in video player");
	}

	@Override
	public void setPitchStepsAdjustment(float pitchSteps) {
		Log.e(TAG, "Setting pitch steps adjustment unsupported in video player");
		throw new UnsupportedOperationException("Setting pitch steps adjustment unsupported in video player");
	}

	@Override
	public void setPlaybackPitch(float f) {
		Log.e(TAG, "Setting playback pitch unsupported in video player");
		throw new UnsupportedOperationException("Setting playback pitch unsupported in video player");
	}

	@Override
	public void setPlaybackSpeed(float f) {
		Log.e(TAG, "Setting playback speed unsupported in video player");
		throw new UnsupportedOperationException("Setting playback speed unsupported in video player");
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
