package de.danoeh.antennapod.storage.database;

import android.content.Context;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for issue #8859: after "Reset statistics data", the playback
 * service still holds the pre-reset played duration of the loaded episode in
 * memory. The fix makes the service zero its in-memory copy when the reset
 * event arrives. This test verifies the storage side of the contract: writing
 * playback information from a copy whose played duration was reset to zero
 * and then incremented only by the newly played time persists exactly that
 * value (not the pre-reset one).
 */
@RunWith(RobolectricTestRunner.class)
public class ResetStatisticsTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    private FeedMedia insertPlayedMedia(int playedDuration) throws Exception {
        Feed feed = new Feed(0, null, "title", "link", "description", null,
                null, null, null, null, null, null, "https://example.com/feed", 0);
        Feed feedDb = FeedDatabaseWriter.updateFeed(context, feed, false);
        FeedItem item = new FeedItem(0, "item", "guid", "link", new Date(),
                FeedItem.UNPLAYED, feedDb);
        FeedMedia media = new FeedMedia(0, item, 60000, 10000, 1024, "audio/mpeg",
                null, "https://example.com/episode.mp3", 0, new Date(), playedDuration, 0L);
        item.setMedia(media);
        feedDb.setItems(Collections.singletonList(item));
        DBWriter.setCompleteFeed(feedDb).get();
        DBWriter.setFeedItem(item, false).get();
        // setCompleteFeed does not persist played_duration; the playback service
        // persists it via setFeedMediaPlaybackInformation on every position save.
        DBWriter.setFeedMediaPlaybackInformation(media).get();
        return DBReader.getFeedMedia(item.getMedia().getId());
    }

    @Test
    public void testResetClearsPlayedDurationOfLoadedEpisode() throws Exception {
        FeedMedia media = insertPlayedMedia(360000);
        assertEquals(360000, media.getPlayedDuration());

        // What the fixed playback service does after the reset: zero the in-memory
        // copy, then only newly played time is added on the next position save.
        FeedMedia afterReset = new FeedMedia(media.getId(), media.getItem(),
                media.getDuration(), media.getPosition(), media.getSize(), media.getMimeType(),
                media.getLocalFileUrl(), media.getDownloadUrl(), media.getDownloadDate(),
                media.getLastPlayedTimeHistory(), 0, media.getLastPlayedTimeStatistics());
        afterReset.incrementPlayedDuration(5000);
        DBWriter.setFeedMediaPlaybackInformation(afterReset).get();

        FeedMedia loaded = DBReader.getFeedMedia(afterReset.getId());
        assertEquals(5000, loaded.getPlayedDuration());
    }
}
