package de.danoeh.antennapod.playback.service;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LegacyNextChapterTest {
    private MediaSessionCompat.Callback callback;
    private PlaybackServiceMediaPlayer player;
    private Playable playable;

    @Before
    public void setUp() {
        PlaybackService service = new PlaybackService();
        player = mock(PlaybackServiceMediaPlayer.class);
        playable = mock(Playable.class);
        when(playable.getChapters()).thenReturn(null);
        when(player.getPlayable()).thenReturn(playable);
        ReflectionHelpers.setField(service, "mediaPlayer", player);
        callback = ReflectionHelpers.getField(service, "sessionCallback");
    }

    @Test
    public void ignoresNextChapterWhileChaptersAreLoading() {
        callback.onCustomAction(PlaybackService.CUSTOM_ACTION_NEXT_CHAPTER, Bundle.EMPTY);

        verify(player, never()).skip();
        verify(player, never()).seekTo(anyInt());
    }

    @Test
    public void seeksToNextChapterAfterLoadingCompletes() {
        callback.onCustomAction(PlaybackService.CUSTOM_ACTION_NEXT_CHAPTER, Bundle.EMPTY);
        when(playable.getChapters()).thenReturn(List.of(
                new Chapter(0, "First", null, null), new Chapter(60000, "Second", null, null)));
        when(player.getPosition()).thenReturn(10000);

        callback.onCustomAction(PlaybackService.CUSTOM_ACTION_NEXT_CHAPTER, Bundle.EMPTY);

        verify(player).seekTo(60000);
        verify(player, never()).skip();
    }

    @Test
    public void fallsBackToNextEpisodeWhenThereAreNoChapters() {
        when(playable.getChapters()).thenReturn(Collections.emptyList());

        callback.onCustomAction(PlaybackService.CUSTOM_ACTION_NEXT_CHAPTER, Bundle.EMPTY);

        verify(player).skip();
        verify(player, never()).seekTo(anyInt());
    }

    @Test
    public void fallsBackToNextEpisodeAtLastChapter() {
        when(playable.getChapters()).thenReturn(List.of(new Chapter(0, "Only", null, null)));
        when(player.getPosition()).thenReturn(10000);

        callback.onCustomAction(PlaybackService.CUSTOM_ACTION_NEXT_CHAPTER, Bundle.EMPTY);

        verify(player).skip();
        verify(player, never()).seekTo(anyInt());
    }
}
