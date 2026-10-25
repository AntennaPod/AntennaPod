package de.danoeh.antennapod.playback.service;

import android.content.Context;
import androidx.core.util.Consumer;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PlaybackServiceStarterTest {

    @Test
    public void deletedLocalFileReloadsSameEpisodeFromRemoteSource() {
        Context context = mock(Context.class);
        FeedMedia media = mock(FeedMedia.class);
        when(media.getId()).thenReturn(42L);
        when(media.localFileAvailable()).thenReturn(false);
        when(media.getStreamUrl()).thenReturn("https://example.com/episode.mp3");

        MediaController controller = mock(MediaController.class);
        when(controller.getCurrentMediaItem()).thenReturn(new MediaItem.Builder()
                .setMediaId("42")
                .setUri("file:///downloads/episode.mp3")
                .build());
        when(controller.isPlaying()).thenReturn(true);

        try (MockedStatic<PlaybackController> binding = mockStatic(PlaybackController.class)) {
            binding.when(() -> PlaybackController.bindToMedia3Service(eq(context), any()))
                    .thenAnswer(invocation -> {
                        Consumer<MediaController> consumer = invocation.getArgument(1);
                        consumer.accept(controller);
                        return null;
                    });

            new PlaybackServiceStarter(context, media).start();
        }

        verify(controller).setMediaItem(argThat(item -> "42".equals(item.mediaId)));
        verify(controller).prepare();
        verify(controller).play();
    }

    @Test
    public void sameMediaIdWithDeletedLocalFileIsNotCurrentMedia() {
        FeedMedia media = mock(FeedMedia.class);
        when(media.getId()).thenReturn(42L);
        when(media.localFileAvailable()).thenReturn(false);
        when(media.getStreamUrl()).thenReturn("https://example.com/episode.mp3");
        MediaItem currentMediaItem = new MediaItem.Builder()
                .setMediaId("42")
                .setUri("file:///downloads/episode.mp3")
                .build();

        assertFalse(PlaybackServiceStarter.isCurrentMediaItem(media, currentMediaItem));
    }

    @Test
    public void sameMediaIdAndUriIsCurrentMedia() {
        FeedMedia media = mock(FeedMedia.class);
        when(media.getId()).thenReturn(42L);
        when(media.localFileAvailable()).thenReturn(false);
        when(media.getStreamUrl()).thenReturn("https://example.com/episode.mp3");
        MediaItem currentMediaItem = new MediaItem.Builder()
                .setMediaId("42")
                .setUri("https://example.com/episode.mp3")
                .build();

        assertTrue(PlaybackServiceStarter.isCurrentMediaItem(media, currentMediaItem));
    }
}
