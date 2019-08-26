package de.danoeh.antennapod.core.service.playback;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeReductionSetting;
import de.danoeh.antennapod.core.util.playback.Playable;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlaybackVolumeAdaptorTest {

    private static final String FEED_ID = "feedId";

    private PlaybackServiceMediaPlayer mediaPlayer;

    @Before
    public void setUp() throws Exception {
        mediaPlayer = mock(PlaybackServiceMediaPlayer.class);
    }

    @Test
    public void noChangeIfNoFeedMediaPlaying() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PAUSED);

        Playable noFeedMedia = mock(Playable.class);
        when(mediaPlayer.getPlayable()).thenReturn(noFeedMedia);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayerStatusIsError() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.ERROR);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayerStatusIsIndeterminate() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.INDETERMINATE);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayerStatusIsStopped() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.STOPPED);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void noChangeIfPlayableIsNoItemOfAffectedFeed() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PLAYING);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        Feed feed = mockFeed(feedMedia, FEED_ID);
        when(feed.getIdentifyingValue()).thenReturn("wrongFeedId");

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.OFF);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void adaptsPreferencesForLoadedFeedMediaIfPlayerStatusIsPaused() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PAUSED);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = mockFeedPreferences(feedMedia, FEED_ID);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.LIGHT);

        verify(feedPreferences, times(1)).setVolumeReductionSetting(VolumeReductionSetting.LIGHT);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void adaptsPreferencesForLoadedFeedMediaIfPlayerStatusIsPrepared() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PREPARED);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = mockFeedPreferences(feedMedia, FEED_ID);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.LIGHT);

        verify(feedPreferences, times(1)).setVolumeReductionSetting(VolumeReductionSetting.LIGHT);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void adaptsPreferencesForLoadedFeedMediaIfPlayerStatusIsInitializing() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.INITIALIZING);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = mockFeedPreferences(feedMedia, FEED_ID);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.LIGHT);

        verify(feedPreferences, times(1)).setVolumeReductionSetting(VolumeReductionSetting.LIGHT);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void adaptsPreferencesForLoadedFeedMediaIfPlayerStatusIsPreparing() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PREPARING);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = mockFeedPreferences(feedMedia, FEED_ID);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.LIGHT);

        verify(feedPreferences, times(1)).setVolumeReductionSetting(VolumeReductionSetting.LIGHT);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void adaptsPreferencesForLoadedFeedMediaIfPlayerStatusIsSeeking() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.SEEKING);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = mockFeedPreferences(feedMedia, FEED_ID);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.LIGHT);

        verify(feedPreferences, times(1)).setVolumeReductionSetting(VolumeReductionSetting.LIGHT);

        verify(mediaPlayer, never()).pause(anyBoolean(), anyBoolean());
        verify(mediaPlayer, never()).resume();
    }

    @Test
    public void adaptsPreferencesAndForcesVolumeChangeForLoadedFeedMediaIfPlayerStatusIsPlaying() {
        PlaybackVolumeAdaptor playbackVolumeAdaptor = new PlaybackVolumeAdaptor();

        when(mediaPlayer.getPlayerStatus()).thenReturn(PlayerStatus.PLAYING);

        FeedMedia feedMedia = mock(FeedMedia.class);
        when(mediaPlayer.getPlayable()).thenReturn(feedMedia);
        FeedPreferences feedPreferences = mockFeedPreferences(feedMedia, FEED_ID);

        playbackVolumeAdaptor.adaptVolumeIfNecessary(mediaPlayer, FEED_ID, VolumeReductionSetting.HEAVY);

        verify(feedPreferences, times(1)).setVolumeReductionSetting(VolumeReductionSetting.HEAVY);

        verify(mediaPlayer, times(1)).pause(false, false);
        verify(mediaPlayer, times(1)).resume();
    }

    private FeedPreferences mockFeedPreferences(FeedMedia feedMedia, String feedId) {
        Feed feed = mockFeed(feedMedia, feedId);
        FeedPreferences feedPreferences = mock(FeedPreferences.class);
        when(feed.getPreferences()).thenReturn(feedPreferences);
        return feedPreferences;
    }

    private Feed mockFeed(FeedMedia feedMedia, String feedId) {
        FeedItem feedItem = mock(FeedItem.class);
        when(feedMedia.getItem()).thenReturn(feedItem);
        Feed feed = mock(Feed.class);
        when(feed.getIdentifyingValue()).thenReturn(feedId);
        when(feedItem.getFeed()).thenReturn(feed);
        return feed;
    }
}
