package de.danoeh.antennapod.core.service.playback;

import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceHolder;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Interface that allows for different implementations of the PlaybackServiceMediaPlayer for local
 * and remote (cast devices) playback.
 */
public interface IPlaybackServiceMediaPlayer {
    void playMediaObject(@NonNull Playable playable, boolean stream, boolean startWhenPrepared, boolean prepareImmediately);

    void resume();

    void pause(boolean abandonFocus, boolean reinit);

    void prepare();

    void reinit();

    void seekTo(int t);

    void seekDelta(int d);

    void seekToChapter(@NonNull Chapter c);

    int getDuration();

    int getPosition();

    boolean isStartWhenPrepared();

    void setStartWhenPrepared(boolean startWhenPrepared);

    boolean canSetSpeed();

    void setSpeed(float speed);

    float getPlaybackSpeed();

    void setVolume(float volumeLeft, float volumeRight);

    boolean canDownmix();

    void setDownmix(boolean enable);

    MediaType getCurrentMediaType();

    boolean isStreaming();

    void shutdown();

    void setVideoSurface(SurfaceHolder surface);

    void resetVideoSurface();

    Pair<Integer, Integer> getVideoSize();

    PSMPInfo getPSMPInfo();

    PlayerStatus getPlayerStatus();

    Playable getPlayable();

    void endPlayback(boolean wasSkipped);

    void stop();

    interface PSMPCallback {
        void statusChanged(PSMPInfo newInfo);

        void shouldStop();

        void playbackSpeedChanged(float s);

        void setSpeedAbilityChanged();

        void onBufferingUpdate(int percent);

        void updateMediaSessionMetadata(Playable p);

        boolean onMediaPlayerInfo(int code);

        boolean onMediaPlayerError(Object inObj, int what, int extra);

        boolean endPlayback(boolean playNextEpisode, boolean wasSkipped);
    }

    /**
     * Holds information about a PSMP object.
     */
    class PSMPInfo {
        public PlayerStatus playerStatus;
        public Playable playable;

        public PSMPInfo(PlayerStatus playerStatus, Playable playable) {
            this.playerStatus = playerStatus;
            this.playable = playable;
        }
    }
}
