package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

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

    void setDataSource(String streamUrl, String username, String password) throws IOException;

    void setDisplay(SurfaceHolder sh);

    void setPlaybackParams(float speed, boolean skipSilence);

    void setDownmix(boolean enable);

    void setVolume(float left, float right);

    void start();

    void stop();

    void setWakeMode(Context context, int mode);

    List<String> getAudioTracks();

    void setAudioTrack(int track);

    int getSelectedAudioTrack();
}
