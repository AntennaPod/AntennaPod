package de.test.antennapod.service.playback;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.service.playback.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.core.util.playback.Playable;

public class DefaultPSMPCallback implements PlaybackServiceMediaPlayer.PSMPCallback {
        @Override
        public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {

        }

        @Override
        public void shouldStop() {

        }

        @Override
        public void playbackSpeedChanged(float s) {

        }

        @Override
        public void setSpeedAbilityChanged() {

        }

        @Override
        public void onBufferingUpdate(int percent) {

        }

        @Override
        public void onMediaChanged(boolean reloadUI) {

        }

        @Override
        public boolean onMediaPlayerInfo(int code, @StringRes int resourceId) {
            return false;
        }

        @Override
        public boolean onMediaPlayerError(Object inObj, int what, int extra) {
            return false;
        }

        @Override
        public void onPostPlayback(@NonNull Playable media, boolean ended, boolean skipped, boolean playingNext) {

        }

        @Override
        public void onPlaybackStart(@NonNull Playable playable, int position) {

        }

        @Override
        public void onPlaybackPause(Playable playable, int position) {

        }

        @Override
        public Playable getNextInQueue(Playable currentMedia) {
            return null;
        }

        @Override
        public void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {

        }
    }