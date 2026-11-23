package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
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
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MediaLibrarySessionCallbackTest {
    private static final String EPISODE_TITLE = "Episode Title";
    private static final String PODCAST_TITLE = "Podcast Name";
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

    @Test
    public void onVoiceSearchPodcastName() throws Exception {
        Feed feed = seedPodcast();
        long oldUnplayed = episodeOf(feed, "Second: Henry Kissinger").getId();
        long newUnplayed = episodeOf(feed, "Third").getId();
        assertEquals(String.valueOf(newUnplayed), voiceSearch(PODCAST_TITLE, null));

        Bundle extras = new Bundle();
        extras.putString(MediaStore.EXTRA_MEDIA_ALBUM, PODCAST_TITLE);
        extras.putString(MediaStore.EXTRA_MEDIA_TITLE, "Second");
        assertEquals(String.valueOf(oldUnplayed), voiceSearch("Second of " + PODCAST_TITLE, extras));
    }

    @Test
    public void onVoiceSearchPodcastNameResumesInProgress() throws Exception {
        Feed feed = seedPodcast();
        FeedMedia inProgress = episodeOf(feed, "Second: Henry Kissinger");
        inProgress.setPosition(1000);
        inProgress.setLastPlayedTimeHistory(new Date());
        DBWriter.setFeedMediaPlaybackInformation(inProgress).get(5, TimeUnit.SECONDS);
        assertEquals(String.valueOf(inProgress.getId()), voiceSearch(PODCAST_TITLE, null));
    }

    @Test
    public void onVoiceSearchSentence() throws Exception {
        Feed feed = seedPodcast();
        FeedMedia inProgress = episodeOf(feed, "Second: Henry Kissinger");
        inProgress.setPosition(1000);
        inProgress.setLastPlayedTimeHistory(new Date());
        DBWriter.setFeedMediaPlaybackInformation(inProgress).get(5, TimeUnit.SECONDS);
        long newest = episodeOf(feed, "Third").getId();
        assertEquals(String.valueOf(newest), voiceSearch("the most recent episode of podcast name", null));
        assertEquals(String.valueOf(inProgress.getId()), voiceSearch("an episode of Podcast Name", null));
        assertEquals(String.valueOf(inProgress.getId()),
                voiceSearch("the episode of Podcast Name about Henry Kissinger", null));
        assertEquals(String.valueOf(inProgress.getId()), voiceSearch("the episode about Kissinger", null));
    }

    @Test
    public void onVoiceSearchAddsEpisodeToQueue() throws Exception {
        Feed feed = seedPodcast();
        long newest = episodeOf(feed, "Third").getItemId();
        voiceSearch(PODCAST_TITLE, null);
        long deadline = System.currentTimeMillis() + 5000;
        while (!DBReader.getQueueIDList().contains(newest) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(newest, DBReader.getQueueIDList().get(0));
    }

    @Test
    public void noResumeRightAfterVoiceSearchMiss() throws Exception {
        PlaybackPreferences.writeMediaPlaying(seedEpisode());
        MediaItem searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("Unrelated").build())
                .build();
        callback.onSetMediaItems(session, controllerInfo, Collections.singletonList(searchItem),
                C.INDEX_UNSET, C.TIME_UNSET).get(5, TimeUnit.SECONDS);
        assertEquals(0, callback.onPlaybackResumption(session, controllerInfo)
                .get(5, TimeUnit.SECONDS).mediaItems.size());
    }

    @Test
    public void voiceSearchMissKeepsCurrentEpisode() throws Exception {
        MediaItem current = MediaItemAdapter.fromMediaIdStub(seedEpisode().getId());
        Player player = mock(Player.class);
        when(player.getCurrentMediaItem()).thenReturn(current);
        when(player.getCurrentPosition()).thenReturn(1234L);
        MediaSession playingSession = mock(MediaSession.class);
        when(playingSession.getPlayer()).thenReturn(player);
        MediaItem searchItem = MediaItem.EMPTY.buildUpon()
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setSearchQuery("Unrelated").build())
                .build();
        MediaSession.MediaItemsWithStartPosition result = callback.onSetMediaItems(playingSession, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET).get(5, TimeUnit.SECONDS);
        assertEquals(current, result.mediaItems.get(0));
        assertEquals(1234L, result.startPositionMs);
    }

    private String voiceSearch(String query, Bundle extras) throws Exception {
        MediaItem.RequestMetadata.Builder metadata = new MediaItem.RequestMetadata.Builder().setSearchQuery(query);
        if (extras != null) {
            metadata.setExtras(extras);
        }
        MediaItem searchItem = MediaItem.EMPTY.buildUpon().setRequestMetadata(metadata.build()).build();
        MediaSession.MediaItemsWithStartPosition result = callback.onSetMediaItems(session, controllerInfo,
                Collections.singletonList(searchItem), C.INDEX_UNSET, C.TIME_UNSET).get(5, TimeUnit.SECONDS);
        assertEquals(1, result.mediaItems.size());
        return result.mediaItems.get(0).mediaId;
    }

    private FeedMedia episodeOf(Feed feed, String title) {
        for (FeedItem item : feed.getItems()) {
            if (title.equals(item.getTitle())) {
                return item.getMedia();
            }
        }
        throw new IllegalArgumentException(title);
    }

    private Feed seedPodcast() {
        Feed feed = new Feed("podcastUrl", null, PODCAST_TITLE);
        feed.setItems(new ArrayList<>());
        String[] titles = {"First", "Second: Henry Kissinger", "Third"};
        for (int i = 0; i < titles.length; i++) {
            FeedItem item = new FeedItem();
            item.setItemIdentifier("podcast" + i);
            item.setTitle(titles[i]);
            item.setPubDate(new Date(i * 1000L));
            item.setPlayed(i == 0);
            item.setMedia(new FeedMedia(item, "http://example.com/" + i, 2, "mime"));
            item.setFeed(feed);
            feed.getItems().add(item);
        }
        return FeedDatabaseWriter.updateFeed(context, feed, false);
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
