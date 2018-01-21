package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.view.SurfaceHolder;

import java.io.IOException;

public interface IPlayer {
	boolean canSetPitch();

	boolean canSetSpeed();

	boolean canDownmix();

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

	void setDownmix(boolean enable);

	void setVolume(float left, float right);

	void start();

	void stop();

    void setVideoScalingMode(int mode);

    void setWakeMode(Context context, int mode);
}
