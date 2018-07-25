package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.net.Uri;
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

	void setPlaybackSpeed(float f);

	void setDownmix(boolean enable);

	void setVolume(float left, float right);

	void start();

	void stop();

    void setWakeMode(Context context, int mode);

	void setDataSource(Context context, Uri uri) throws IOException;
}
