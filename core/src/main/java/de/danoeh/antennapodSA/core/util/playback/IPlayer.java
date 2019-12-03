package de.danoeh.antennapodSA.core.util.playback;

import android.content.Context;
import android.view.SurfaceHolder;

import java.io.IOException;

public interface IPlayer {

	boolean canSetSpeed();

	boolean canDownmix();


	int getCurrentPosition();

	float getCurrentSpeedMultiplier();

	int getDuration();

	boolean isPlaying();

	void pause();

	void prepare() throws IllegalStateException, IOException;

	void release();

	void reset();

	void seekTo(int msec);

	void setAudioStreamType(int streamtype);

	void setDataSource(String path) throws IllegalStateException, IOException,
            IllegalArgumentException, SecurityException;

	void setDisplay(SurfaceHolder sh);

	void setPlaybackParams(float speed, boolean skipSilence);

	void setDownmix(boolean enable);

	void setVolume(float left, float right);

	void start();

	void stop();

    void setWakeMode(Context context, int mode);
}
