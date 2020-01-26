package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.core.util.playback.Playable;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlaybackVolumeUpdaterTest {

    private static final long FEED_ID = 42;

    private PlaybackServiceMediaPlayer mediaPlayer;

    @Before
    public void setUp() {
        mediaPlayer = mock(PlaybackServiceMediaPlayer.class);
    }

    @Test
    public void noChangeIfNoFeedMediaPlaying() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PAUSED);

        Playable noFeedMedia = mock(Playable.class);
        when(mediaPlayer.getPlayable()).thenReturn(noFeedMedia);

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayerStatusIsError() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.ERROR);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayerStatusIsIndeterminate() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.INDETERMINATE);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayerStatusIsStopped() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.STOPPED);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayableIsNoItemOfAffectedFeed() {
        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PLAYING);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        when(feedMedia.getItem().getFeed().getId()).thenReturn(FEED_ID + 1);

        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();
        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void updatesPreferencesForLoadedFeedMediaIfPlayerStatusIsPaused() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PAUSED);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = feedMedia.getItem().getFeed().getPreferences();

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(feedPreferences, times(1)).setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void updatesPreferencesForLoadedFeedMediaIfPlayerStatusIsPrepared() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PREPARED);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = feedMedia.getItem().getFeed().getPreferences();

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(feedPreferences, times(1)).setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void updatesPreferencesForLoadedFeedMediaIfPlayerStatusIsInitializing() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.INITIALIZING);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = feedMedia.getItem().getFeed().getPreferences();

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(feedPreferences, times(1)).setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void updatesPreferencesForLoadedFeedMediaIfPlayerStatusIsPreparing() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PREPARING);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = feedMedia.getItem().getFeed().getPreferences();

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(feedPreferences, times(1)).setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void updatesPreferencesForLoadedFeedMediaIfPlayerStatusIsSeeking() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.SEEKING);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = feedMedia.getItem().getFeed().getPreferences();

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(feedPreferences, times(1)).setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_REDUCTION);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void updatesPreferencesAndForcesVolumeChangeForLoadedFeedMediaIfPlayerStatusIsPlaying() {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PLAYING);

        FeedMedia feedMedia = mockFeedMedia();
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = feedMedia.getItem().getFeed().getPreferences();

        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeAdaptionSetting.HEAVY_REDUCTION);

        verify(feedPreferences, times(1)).setVolumeAdaptionSetting(VolumeAdaptionSetting.HEAVY_REDUCTION);

        verify(mediaPlayer, times(1)).pause(false, false);
        verify(mediaPlayer, times(1)).resume();
    }
    
    private FeedMedia mockFeedMedia() {
        FeedMedia feedMedia = mock(FeedMedia.class);
        FeedItem feedItem = mock(FeedItem.class);
        Feed feed = mock(Feed.class);
        FeedPreferences feedPreferences = mock(FeedPreferences.class);

        when(feedMedia.getItem()).thenReturn(feedItem);
        when(feedItem.getFeed()).thenReturn(feed);
        when(feed.getId()).thenReturn(FEED_ID);
        when(feed.getPreferences()).thenReturn(feedPreferences);
        return feedMedia;
    }
}
