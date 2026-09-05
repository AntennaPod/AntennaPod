package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaSession;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueueStub;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class MediaLibrarySessionCallbackTest {
    private static final String EPISODE_TITLE = "Episode Title";
    private Context context;
    private MediaLibrarySessionCallback callback;
    private final MediaSession session = mock(MediaSession.class);
    private final MediaSession.ControllerInfo controllerInfo = mock(MediaSession.ControllerInfo.class);

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PlaybackPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        SynchronizationQueue.setInstance(new SynchronizationQueueStub());
        callback = new MediaLibrarySessionCallback(context);
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void onSetMediaItemsStub() throws Exception {
        long mediaId = seedEpisode().getId();
        MediaItem browseItem = MediaItemAdapter.fromMediaIdStub(mediaId);
        MediaSession.MediaItemsWithStartPosition result = callback.onSetMediaItems(
                session, controllerInfo, Collections.singletonList(browseItem), C.INDEX_UNSET, C.TIME_UNSET)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(mediaId), result.mediaItems.get(0).mediaId);
        assertEquals(EPISODE_TITLE, result.mediaItems.get(0).mediaMetadata.title);
    }

    @Test
    public void onPlaybackResumption() throws Exception {
        FeedMedia media = seedEpisode();
        PlaybackPreferences.writeMediaPlaying(media);
        MediaSession.MediaItemsWithStartPosition result = callback.onPlaybackResumption(session, controllerInfo)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(media.getId()), result.mediaItems.get(0).mediaId);
    }

    @Test
    public void onAndroidAutoVoiceSearchQuery() throws Exception {
        long mediaId = seedEpisode().getId();
        MediaItem searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery(EPISODE_TITLE).build())
                .build();
        MediaSession.MediaItemsWithStartPosition result = callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET).get(5, TimeUnit.SECONDS);
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(mediaId), result.mediaItems.get(0).mediaId);

        // No match: nothing to play
        searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("Unrelated").build())
                .build();
        result = callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET).get(5, TimeUnit.SECONDS);
        assertEquals(0, result.mediaItems.size());

        // Empty query ("play something"): fall back to playing something rather than nothing, per
        // Android Auto/Assistant voice action guidelines.
        searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("").build())
                .build();
        result = callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET).get(5, TimeUnit.SECONDS);
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(mediaId), result.mediaItems.get(0).mediaId);
    }

    private FeedMedia seedEpisode() {
        Feed feed = new Feed("url", null, null);
        feed.setItems(new ArrayList<>());
        FeedItem item = new FeedItem();
        item.setItemIdentifier("id");
        item.setTitle(EPISODE_TITLE);
        item.setMedia(new FeedMedia(item, "http://example.com", 2, "mime"));
        item.setFeed(feed);
        feed.getItems().add(item);
        return FeedDatabaseWriter.updateFeed(context, feed, false).getItems().get(0).getMedia();
    }
}
