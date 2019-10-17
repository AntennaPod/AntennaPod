package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeReductionSetting;
import de.danoeh.antennapod.core.util.playback.Playable;

class PlaybackVolumeUpdater {

    public void updateVolumeIfNecessary(PlaybackServiceMediaPlayer mediaPlayer, String affectedFeedIdentifier, VolumeReductionSetting volumeReductionSetting) {
        Playable playable = mediaPlayer.getPlayable();
        boolean isFeedMedia = playable instanceof FeedMedia;
        boolean isPlayableLoaded = isPlayableLoaded(mediaPlayer.getPlayerStatus());

        if (isFeedMedia && isPlayableLoaded) {
            updateFeedMediaVolumeIfNecessary(mediaPlayer, affectedFeedIdentifier, volumeReductionSetting, (FeedMedia) playable);
        }
    }

    private void updateFeedMediaVolumeIfNecessary(PlaybackServiceMediaPlayer mediaPlayer, String affectedFeedIdentifier, VolumeReductionSetting volumeReductionSetting, FeedMedia feedMedia) {
        if (mediaBelongsToAffectedFeed(feedMedia, affectedFeedIdentifier)) {
            FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
            preferences.setVolumeReductionSetting(volumeReductionSetting);

            if (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING) {
                forceUpdateVolume(mediaPlayer);
            }
        }
    }

    private static boolean isPlayableLoaded(PlayerStatus playerStatus) {
        return playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.SEEKING
                || playerStatus == PlayerStatus.PREPARING
                || playerStatus == PlayerStatus.PREPARED
                || playerStatus == PlayerStatus.INITIALIZING;
    }

    private static boolean mediaBelongsToAffectedFeed(FeedMedia feedMedia, String affectedFeedIdentifier) {
        return affectedFeedIdentifier != null
                && affectedFeedIdentifier.equals(feedMedia.getItem().getFeed().getIdentifyingValue());
    }

    private void forceUpdateVolume(PlaybackServiceMediaPlayer mediaPlayer) {
        mediaPlayer.pause(false, false);
        mediaPlayer.resume();
    }

}
