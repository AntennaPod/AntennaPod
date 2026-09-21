package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaSession;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBReader;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class MediaLibrarySessionCallbackUnknownIdTest {
    private static final long KNOWN_MEDIA_ID = 5;
    private static final long DELETED_MEDIA_ID = 999;

    private Context context;
    private MockedStatic<DBReader> dbReader;
    private MediaLibrarySessionCallback callback;

    @Before
    public void setUp() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
        context = RuntimeEnvironment.getApplication();
        Feed feed = new Feed("http://example.com/feed", null, "Feed");
        FeedItem item = new FeedItem(0, "Episode", "http://example.com/feed/id0", "link",
                new java.util.Date(0), FeedItem.UNPLAYED, feed);
        FeedMedia media = new FeedMedia(item, "http://example.com/media.mp3", 1024, "audio/mp3");
        media.setId(KNOWN_MEDIA_ID);
        item.setMedia(media);
        dbReader = Mockito.mockStatic(DBReader.class);
        dbReader.when(() -> DBReader.getFeedMedia(KNOWN_MEDIA_ID)).thenReturn(media);
        dbReader.when(() -> DBReader.getFeedMedia(DELETED_MEDIA_ID)).thenReturn(null);
        callback = new MediaLibrarySessionCallback(context);
    }

    @After
    public void tearDown() {
        dbReader.close();
        RxJavaPlugins.reset();
    }

    private static MediaItem mediaItem(long id) {
        return new MediaItem.Builder().setMediaId(String.valueOf(id)).build();
    }

    @Test
    public void aQueueWhoseRequestedItemWasDeletedStillPlaysTheRemainingEpisodes() throws Exception {
        List<MediaItem> queue = new ArrayList<>();
        queue.add(mediaItem(DELETED_MEDIA_ID));
        queue.add(mediaItem(KNOWN_MEDIA_ID));

        MediaSession.MediaItemsWithStartPosition result =
                callback.onSetMediaItems(null, null, queue, 1, 0).get();

        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(KNOWN_MEDIA_ID), result.mediaItems.get(0).mediaId);
        assertEquals(0, result.startIndex);
    }

    @Test
    public void aQueueContainingOnlyADeletedItemStartsNothing() throws Exception {
        List<MediaItem> queue = new ArrayList<>();
        queue.add(mediaItem(DELETED_MEDIA_ID));

        MediaSession.MediaItemsWithStartPosition result =
                callback.onSetMediaItems(null, null, queue, 0, 0).get();

        assertEquals(0, result.mediaItems.size());
        assertEquals(0, result.startIndex);
    }
}
