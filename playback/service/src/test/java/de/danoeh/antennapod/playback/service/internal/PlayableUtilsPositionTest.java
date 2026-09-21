package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueueStub;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PlayableUtilsPositionTest {
    private Context context;
    private Feed feed;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        SynchronizationQueue.setInstance(new SynchronizationQueueStub());
        Feed pending = new Feed("http://example.com/feed", null, "Feed");
        FeedItem item = new FeedItem(0, "Episode", "http://example.com/feed/id0", "link",
                new Date(1000L), FeedItem.UNPLAYED, pending);
        item.setMedia(new FeedMedia(item, "http://example.com/media0.mp3", 1024, "audio/mp3"));
        List<FeedItem> items = new ArrayList<>();
        items.add(item);
        pending.setItems(items);
        feed = FeedDatabaseWriter.updateFeed(context, pending, false);
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    private FeedMedia newEpisode() throws Exception {
        FeedItem item = feed.getItems().get(0);
        DBWriter.markItemsPlayed(FeedItem.NEW, false, Collections.singletonList(item)).get();
        FeedMedia media = DBReader.getFeedMedia(item.getMedia().getId());
        return media;
    }

    private void waitForDatabase() throws Exception {
        DBWriter.clearDownloadLog().get();
    }

    @Test
    public void savingThePositionStoresItAndTheTimeItWasLastPlayed() throws Exception {
        FeedMedia media = newEpisode();
        long timestamp = System.currentTimeMillis();

        PlayableUtils.saveCurrentPosition(media, 42000, timestamp);
        waitForDatabase();

        FeedMedia reloaded = DBReader.getFeedMedia(media.getId());
        assertEquals(42000, reloaded.getPosition());
        assertEquals(timestamp, reloaded.getLastPlayedTimeStatistics());
        assertEquals(timestamp, reloaded.getLastPlayedTimeHistory().getTime());
    }

    @Test
    public void savingAPositionTakesANewEpisodeOutOfTheNewStateInTheDatabase() throws Exception {
        FeedMedia media = newEpisode();

        PlayableUtils.saveCurrentPosition(media, 1000, System.currentTimeMillis());
        waitForDatabase();

        assertFalse(DBReader.getFeedItem(media.getItem().getId()).isNew());
    }

    @Test
    public void startingANewEpisodeFromItsBeginningAlsoTakesItOutOfTheNewState() throws Exception {
        FeedMedia media = newEpisode();

        PlayableUtils.saveCurrentPosition(media, 0, System.currentTimeMillis());
        waitForDatabase();

        assertFalse(DBReader.getFeedItem(media.getItem().getId()).isNew());
    }
}
