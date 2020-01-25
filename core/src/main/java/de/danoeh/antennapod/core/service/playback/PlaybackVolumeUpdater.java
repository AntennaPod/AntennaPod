package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.core.util.playback.Playable;

class PlaybackVolumeUpdater {

    public void updateVolumeIfNecessary(PlaybackServiceMediaPlayer mediaPlayer, long feedId,
                                        VolumeAdaptionSetting volumeAdaptionSetting) {
        Playable playable = mediaPlayer.getPlayable();
        boolean isFeedMedia = playable instanceof FeedMedia;
        boolean isPlayableLoaded = isPlayableLoaded(mediaPlayer.getPlayerStatus());

        if (isFeedMedia && isPlayableLoaded) {
            updateFeedMediaVolumeIfNecessary(mediaPlayer, feedId, volumeAdaptionSetting, (FeedMedia) playable);
        }
    }

    private void updateFeedMediaVolumeIfNecessary(PlaybackServiceMediaPlayer mediaPlayer, long feedId,
                                                  VolumeAdaptionSetting volumeAdaptionSetting, FeedMedia feedMedia) {
        if (feedMedia.getItem().getFeed().getId() == feedId) {
            FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
            preferences.setVolumeAdaptionSetting(volumeAdaptionSetting);

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

    private void forceUpdateVolume(PlaybackServiceMediaPlayer mediaPlayer) {
        mediaPlayer.pause(false, false);
        mediaPlayer.resume();
    }

}
