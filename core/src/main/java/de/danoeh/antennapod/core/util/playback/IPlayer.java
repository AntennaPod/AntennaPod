package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.view.SurfaceHolder;

import java.io.IOException;

public interface IPlayer {
	boolean canSetPitch();

	boolean canSetSpeed();

	float getCurrentPitchStepsAdjustment();

	int getCurrentPosition();

	float getCurrentSpeedMultiplier();

	int getDuration();

	float getMaxSpeedMultiplier();

	float getMinSpeedMultiplier();

	boolean isLooping();

	boolean isPlaying();

	void pause();

	void prepare() throws IllegalStateException, IOException;

	void prepareAsync();

	void release();

	void reset();

	void seekTo(int msec);

	void setAudioStreamType(int streamtype);

	void setScreenOnWhilePlaying(boolean screenOn);

	void setDataSource(String path) throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException;

	void setDisplay(SurfaceHolder sh);

	void setEnableSpeedAdjustment(boolean enableSpeedAdjustment);

	void setLooping(boolean looping);

	void setPitchStepsAdjustment(float pitchSteps);

	void setPlaybackPitch(float f);

	void setPlaybackSpeed(float f);

	void setVolume(float left, float right);

	void start();

	void stop();

    public void setVideoScalingMode(int mode);

    public void setWakeMode(Context context, int mode);
}
