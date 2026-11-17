package de.danoeh.antennapod.playback.service;

import androidx.media3.common.Player;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class Media3NextChapterTest {
    private Media3PlaybackService service;
    private Player player;
    private FeedMedia playable;

    @Before
    public void setUp() {
        service = new Media3PlaybackService();
        player = mock(Player.class);
        playable = mock(FeedMedia.class);
        when(playable.getChapters()).thenReturn(null);
        ReflectionHelpers.setField(service, "player", player);
        ReflectionHelpers.setField(service, "currentPlayable", playable);
    }

    @Test
    public void ignoresNextChapterWhileChaptersAreLoading() {
        ReflectionHelpers.callInstanceMethod(service, "seekToNextChapter");

        verify(playable, never()).getItem();
        verifyNoInteractions(player);
    }

    @Test
    public void seeksToNextChapterAfterLoadingCompletes() {
        ReflectionHelpers.callInstanceMethod(service, "seekToNextChapter");
        when(playable.getChapters()).thenReturn(List.of(
                new Chapter(0, "First", null, null), new Chapter(60000, "Second", null, null)));
        when(player.getCurrentPosition()).thenReturn(10000L);

        ReflectionHelpers.callInstanceMethod(service, "seekToNextChapter");

        verify(player).seekTo(60000L);
        verify(playable, never()).getItem();
    }

    @Test
    public void fallsBackToNextEpisodeWhenThereAreNoChapters() {
        when(playable.getChapters()).thenReturn(Collections.emptyList());

        ReflectionHelpers.callInstanceMethod(service, "seekToNextChapter");

        verify(playable).getItem();
        verify(player, never()).seekTo(anyLong());
    }

    @Test
    public void fallsBackToNextEpisodeAtLastChapter() {
        when(playable.getChapters()).thenReturn(List.of(new Chapter(0, "Only", null, null)));
        when(player.getCurrentPosition()).thenReturn(10000L);

        ReflectionHelpers.callInstanceMethod(service, "seekToNextChapter");

        verify(playable).getItem();
        verify(player, never()).seekTo(anyLong());
    }
}
