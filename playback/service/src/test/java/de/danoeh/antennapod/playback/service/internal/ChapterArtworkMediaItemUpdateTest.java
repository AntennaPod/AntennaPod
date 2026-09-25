package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.source.MediaSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@OptIn(markerClass = UnstableApi.class)
@RunWith(RobolectricTestRunner.class)
public class ChapterArtworkMediaItemUpdateTest {
    private SimpleCache simpleCache;
    private ExoPlayerUtils.ApMediaSourceFactory factory;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        simpleCache = new SimpleCache(new File(context.getCacheDir(), "test-streaming"),
                new NoOpCacheEvictor(), new StandaloneDatabaseProvider(context));
        factory = new ExoPlayerUtils.ApMediaSourceFactory(context, simpleCache);
    }

    @After
    public void tearDown() {
        simpleCache.release();
    }

    private static MediaItem itemWithArtwork(byte[] artwork) {
        return new MediaItem.Builder()
                .setUri("file:///storage/episode.mp3")
                .setMediaId("42")
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle("Episode")
                        .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build())
                .build();
    }

    @Test
    public void artworkOnlyChangeUpdatesMediaItemInPlace() {
        MediaSource source = factory.createMediaSource(itemWithArtwork(new byte[] {1, 2, 3}));
        assertTrue(source.canUpdateMediaItem(itemWithArtwork(new byte[] {4, 5, 6})));
    }

    @Test
    public void differentUriRebuildsMediaSource() {
        MediaItem original = itemWithArtwork(new byte[] {1, 2, 3});
        MediaSource source = factory.createMediaSource(original);
        assertFalse(source.canUpdateMediaItem(
                original.buildUpon().setUri("file:///storage/other.mp3").build()));
    }
}
