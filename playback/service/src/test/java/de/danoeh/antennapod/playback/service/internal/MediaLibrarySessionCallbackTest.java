package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Looper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
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
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class MediaLibrarySessionCallbackTest {
    private static final String EPISODE_TITLE = "Episode Title";
    private Context context;
    private MediaLibrarySessionCallback callback;
    private int playbackInteractions;
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
        callback = new MediaLibrarySessionCallback(context) {
            @Override
            protected void onPlaybackInteraction() {
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                playbackInteractions++;
            }
        };
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void onSetMediaItemsStub() throws Exception {
        long mediaId = seedEpisode().getId();
        MediaItem browseItem = MediaItemAdapter.fromMediaIdStub(mediaId);
        MediaSession.MediaItemsWithStartPosition result = await(callback.onSetMediaItems(
                session, controllerInfo, Collections.singletonList(browseItem), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(mediaId), result.mediaItems.get(0).mediaId);
        assertEquals(EPISODE_TITLE, result.mediaItems.get(0).mediaMetadata.title);
        assertEquals(1, playbackInteractions);
    }

    @Test
    public void onPlaybackResumption() throws Exception {
        FeedMedia media = seedEpisode();
        PlaybackPreferences.writeMediaPlaying(media);
        MediaSession.MediaItemsWithStartPosition result = callback.onPlaybackResumption(session, controllerInfo)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(media.getId()), result.mediaItems.get(0).mediaId);
        assertEquals(0, playbackInteractions);
    }

    @Test
    public void onAndroidAutoVoiceSearchQuery() throws Exception {
        long mediaId = seedEpisode().getId();
        MediaItem searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery(EPISODE_TITLE).build())
                .build();
        MediaSession.MediaItemsWithStartPosition result = await(callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(mediaId), result.mediaItems.get(0).mediaId);

        assertEquals(1, playbackInteractions);

        // No match: nothing to play
        searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("Unrelated").build())
                .build();
        result = await(callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(0, result.mediaItems.size());
        assertEquals(1, playbackInteractions);

        // Empty query ("play something"): fall back to playing something rather than nothing, per
        // Android Auto/Assistant voice action guidelines.
        searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("").build())
                .build();
        result = await(callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(1, result.mediaItems.size());
        assertEquals(String.valueOf(mediaId), result.mediaItems.get(0).mediaId);
        assertEquals(2, playbackInteractions);
    }

    @Test
    public void unresolvedAndFailedSelectionsDoNotReset() throws Exception {
        long mediaId = seedEpisode().getId();
        for (boolean succeeds : new boolean[] {false, true}) {
            playbackInteractions = 0;
            SettableFuture<List<MediaItem>> loaded = SettableFuture.create();
            MediaLibrarySessionCallback delayed = new MediaLibrarySessionCallback(context) {
                @Override
                public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession mediaSession,
                        MediaSession.ControllerInfo controller, List<MediaItem> items) {
                    return loaded;
                }

                @Override
                protected void onPlaybackInteraction() {
                    assertEquals(Looper.getMainLooper(), Looper.myLooper());
                    playbackInteractions++;
                }
            };
            MediaItem selected = MediaItemAdapter.fromMediaIdStub(mediaId);
            ListenableFuture<MediaSession.MediaItemsWithStartPosition> result = delayed.onSetMediaItems(
                    session, controllerInfo, Collections.singletonList(selected), C.INDEX_UNSET, C.TIME_UNSET);
            assertEquals(0, playbackInteractions);
            if (succeeds) {
                loaded.set(Collections.singletonList(selected));
            } else {
                loaded.setException(new IllegalStateException("Selection failed"));
            }
            assertEquals(succeeds ? 1 : 0, await(result).mediaItems.size());
            assertEquals(succeeds ? 1 : 0, playbackInteractions);
        }
    }

    @Test
    public void emptyVoiceFallbackDoesNotReset() throws Exception {
        MediaItem request = MediaItem.EMPTY.buildUpon().setRequestMetadata(
                new MediaItem.RequestMetadata.Builder().setSearchQuery("").build()).build();
        MediaSession.MediaItemsWithStartPosition result = await(callback.onSetMediaItems(
                session, controllerInfo, Collections.singletonList(request), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(0, result.mediaItems.size());
        assertEquals(0, playbackInteractions);
    }

    @Test
    public void pauseAndResumeAreNotInteractions() {
        Player player = mock(Player.class);
        when(session.getPlayer()).thenReturn(player);
        Player.Commands commands = new Player.Commands.Builder().add(Player.COMMAND_PLAY_PAUSE).build();
        when(player.getPlayWhenReady()).thenReturn(false);
        callback.onPlayerInteractionFinished(session, controllerInfo, commands);
        assertEquals(0, playbackInteractions);
        when(player.getPlayWhenReady()).thenReturn(true);
        callback.onPlayerInteractionFinished(session, controllerInfo, commands);
        assertEquals(0, playbackInteractions);
    }

    @Test
    public void onlyIndexedEpisodeSeekingIsAnInteraction() {
        int[] commands = {Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SET_SPEED_AND_PITCH};
        for (int command : commands) {
            playbackInteractions = 0;
            callback.onPlayerInteractionFinished(session, controllerInfo,
                    new Player.Commands.Builder().add(command).build());
            assertEquals(command == Player.COMMAND_SEEK_TO_MEDIA_ITEM ? 1 : 0, playbackInteractions);
        }
    }

    @Test
    public void queueEditsVolumeAndPreparationAreNotInteractions() {
        callback.onPlayerInteractionFinished(session, controllerInfo, new Player.Commands.Builder()
                .add(Player.COMMAND_CHANGE_MEDIA_ITEMS).add(Player.COMMAND_SET_VOLUME)
                .add(Player.COMMAND_PREPARE).add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM).build());
        assertEquals(0, playbackInteractions);
    }

    @Test
    public void clearingMediaItemsIsNotAnInteraction() throws Exception {
        await(callback.onSetMediaItems(session, controllerInfo, Collections.emptyList(), C.INDEX_UNSET, C.TIME_UNSET));
        assertEquals(0, playbackInteractions);
    }

    @Test
    public void notificationSeekAndSkipCommandsDoNotReset() throws Exception {
        when(session.getPlayer()).thenReturn(mock(Player.class));
        callback.onCustomCommand(session, controllerInfo, MediaLibrarySessionCallback.SESSION_COMMAND_REWIND,
                Bundle.EMPTY).get();
        callback.onCustomCommand(session, controllerInfo, MediaLibrarySessionCallback.SESSION_COMMAND_FAST_FORWARD,
                Bundle.EMPTY).get();
        callback.onCustomCommand(session, controllerInfo, MediaLibrarySessionCallback.SESSION_COMMAND_SKIP_EPISODE,
                Bundle.EMPTY).get();
        assertEquals(0, playbackInteractions);
        verify(session.getPlayer()).seekBack();
        verify(session.getPlayer()).seekForward();
        verify(session.getPlayer()).seekToNextMediaItem();
    }

    @Test
    public void hardwareSeekDoesNotReset() {
        when(session.getPlayer()).thenReturn(mock(Player.class));
        Intent down = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND));
        callback.onMediaButtonEvent(session, controllerInfo, down);
        Intent up = new Intent(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND));
        callback.onMediaButtonEvent(session, controllerInfo, up);
        assertEquals(0, playbackInteractions);
        verify(session.getPlayer()).seekBack();
    }

    private <T> T await(ListenableFuture<T> future) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!future.isDone() && System.nanoTime() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        return future.get(0, TimeUnit.SECONDS);
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
