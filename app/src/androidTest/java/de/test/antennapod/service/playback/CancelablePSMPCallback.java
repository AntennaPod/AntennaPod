package de.test.antennapod.service.playback;

import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.core.service.playback.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.model.playback.Playable;

public class CancelablePSMPCallback implements PlaybackServiceMediaPlayer.PSMPCallback {

    private final PlaybackServiceMediaPlayer.PSMPCallback originalCallback;
    private boolean isCancelled = false;

    public CancelablePSMPCallback(PlaybackServiceMediaPlayer.PSMPCallback originalCallback) {
        this.originalCallback = originalCallback;
    }

    public void cancel() {
        isCancelled = true;
    }

    @Override
    public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {
        if (isCancelled) {
            return;
        }
        originalCallback.statusChanged(newInfo);
    }

    @Override
    public void shouldStop() {
        if (isCancelled) {
            return;
        }
        originalCallback.shouldStop();
    }

    @Override
    public void playbackSpeedChanged(float s) {
        if (isCancelled) {
            return;
        }
        originalCallback.playbackSpeedChanged(s);
    }

    @Override
    public void onBufferingUpdate(int percent) {
        if (isCancelled) {
            return;
        }
        originalCallback.onBufferingUpdate(percent);
    }

    @Override
    public void onMediaChanged(boolean reloadUI) {
        if (isCancelled) {
            return;
        }
        originalCallback.onMediaChanged(reloadUI);
    }

    @Override
    public boolean onMediaPlayerInfo(int code, int resourceId) {
        if (isCancelled) {
            return true;
        }
        return originalCallback.onMediaPlayerInfo(code, resourceId);
    }

    @Override
    public boolean onMediaPlayerError(Object inObj, int what, int extra) {
        if (isCancelled) {
            return true;
        }
        return originalCallback.onMediaPlayerError(inObj, what, extra);
    }

    @Override
    public void onPostPlayback(@NonNull Playable media, boolean ended, boolean skipped, boolean playingNext) {
        if (isCancelled) {
            return;
        }
        originalCallback.onPostPlayback(media, ended, skipped, playingNext);
    }

    @Override
    public void onPlaybackStart(@NonNull Playable playable, int position) {
        if (isCancelled) {
            return;
        }
        originalCallback.onPlaybackStart(playable, position);
    }

    @Override
    public void onPlaybackPause(Playable playable, int position) {
        if (isCancelled) {
            return;
        }
        originalCallback.onPlaybackPause(playable, position);
    }

    @Override
    public Playable getNextInQueue(Playable currentMedia) {
        if (isCancelled) {
            return null;
        }
        return originalCallback.getNextInQueue(currentMedia);
    }

    @Override
    public void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {
        if (isCancelled) {
            return;
        }
        originalCallback.onPlaybackEnded(mediaType, stopPlaying);
    }
}