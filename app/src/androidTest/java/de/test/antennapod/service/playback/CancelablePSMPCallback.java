package de.test.antennapod.service.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;

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
    public void onMediaChanged(boolean reloadUI) {
        if (isCancelled) {
            return;
        }
        originalCallback.onMediaChanged(reloadUI);
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

    @Nullable
    @Override
    public Playable findMedia(@NonNull String url) {
        if (isCancelled) {
            return null;
        }
        return originalCallback.findMedia(url);
    }

    @Override
    public void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {
        if (isCancelled) {
            return;
        }
        originalCallback.onPlaybackEnded(mediaType, stopPlaying);
    }

    @Override
    public void ensureMediaInfoLoaded(@NonNull Playable media) {
        if (isCancelled) {
            return;
        }
        originalCallback.ensureMediaInfoLoaded(media);
    }
}