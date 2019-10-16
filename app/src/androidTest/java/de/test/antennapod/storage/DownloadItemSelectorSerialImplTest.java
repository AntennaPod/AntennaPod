package de.test.antennapod.storage;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadItemSelectorSerialImpl;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.test.antennapod.storage.FeedTestUtil.FeedsAccessor;

import static de.danoeh.antennapod.core.feed.FeedItem.NEW;
import static de.danoeh.antennapod.core.feed.FeedItem.PLAYED;
import static de.danoeh.antennapod.core.feed.FeedItem.UNPLAYED;
import static de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType.EPISODIC;
import static de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType.SERIAL;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.AUTO_DL_TRUE;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.KEEP_UPDATED_TRUE;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.cFI;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.createFeed;
import static de.test.antennapod.storage.FeedTestUtil.saveFeeds;
import static de.test.antennapod.storage.FeedTestUtil.toIds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DownloadItemSelectorSerialImplTest {

    private static final String TAG = "DlItemSlctrSerialTest";

    @After
    public void tearDown() throws Exception {
        // Leave DB as-is so that if a test fails, the state of the DB can still be inspected.
        // setUp() will delete the database anyway.
        /// assertTrue(PodDBAdapter.deleteDatabase());
    }

    @Before
    public void setUp() throws Exception {
        // create new database
        PodDBAdapter.init(ApplicationProvider.getApplicationContext());
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

    }

    @Test
    public void basic_Initial() throws Exception {
        // - 3 serial ones, 3 items each, all unplayed
        // - some non-serial one
        // - no serial one has played before

        // Setup test data
        Feed f0 = createFeed(0, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED));
        Feed f1 = createFeed("T Feed 1", SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), cFI(UNPLAYED), cFI(UNPLAYED));
        Feed f2 = createFeed("A Feed 2", SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), cFI(UNPLAYED), cFI(UNPLAYED));
        Feed f3 = createFeed("M Feed 3", SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), cFI(UNPLAYED), cFI(UNPLAYED));
        Feed f4 = createFeed(4, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), cFI(UNPLAYED), cFI(UNPLAYED));
        FeedsAccessor a = saveFeeds(f0, f1, f2, f3, f4);

        List<Long> expected = toIds(a.fi(1, 0), a.fi(2, 0), a.fi(3, 0));

        // Run actual test

        // Now create the selector under test and exercise it
        DownloadItemSelectorSerialImpl selector = new DownloadItemSelectorSerialImpl();

        // Test individual components of the implementation
        {
            // - The specific feed titles in test data ensure that the candidates are ordered by id
            List<Feed> actual = selector.getSerialFeedsOrderedById();
            assertEquals("getSerialFeedsOrderedById() - actual: " + actual,
                    toIds(f1, f2, f3), toIds(actual));
        }
        {
            FeedItem lastPlayed = selector.getLastPlayedSerialFeedItem();
            assertNull("getLastPlayedSerialFeedItem() - should be null",
                    lastPlayed);
        }
        { // none last played, so the order is the same as ids
            List<Feed> actual = selector.getSerialFeedsOrderedByDownloadOrder();
            assertEquals("getSerialFeedsOrderedById() - actual: " + actual,
                    toIds(f1, f2, f3), toIds(actual));
        }
        {
            FeedItem nextToDownload = selector.getNextItemToDownloadForSerial(f1);
            assertEquals("getNextItemToDownloadForSerial()",
                    a.fi(1,0), nextToDownload);
        }

        // Test Overall
        List<? extends FeedItem> fiActual = selector.getAutoDownloadableEpisodes();
        assertEquals("Basic, initial case -  It returns: " + fiActual,
                expected, toIds(fiActual));
    }

    @Test
    public void basic_Ongoing() throws Exception {
        // - 3 serial ones, 3 items each
        // - feed 0: all played
        // - feed 1: all unplayed, but 1 downloaded
        // - feed 2: all unplayed
        // - feed 3: 1 played, 1 ongoing
        // - feed 4: all unplayed
        // - the latest serial: feed3,  ongoing one
        // - expectation: feed4, feed2, feed3, feed1
        // Also test:
        // - new flag does not matter (treated it the same as unplayed)
        // - feed with downloaded item are pushed to the end

        // Setup test data
        Feed f0 = createFeed(0, SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(PLAYED), cFI(PLAYED), cFI(PLAYED));
        Feed f1 = createFeed("T Feed 1", SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), // but downloaded (to be set later)
                cFI(UNPLAYED));
        Feed f2 = createFeed("A Feed 2", SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), cFI(UNPLAYED), cFI(UNPLAYED));
        Feed f3 = createFeed("M Feed 3", SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), // will not be picked
                cFI(PLAYED),
                cFI(UNPLAYED), // in-progress (to be set later)
                cFI(NEW) // the one to be picked, new flag
        );
        Feed f4 = createFeed(4, SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), cFI(UNPLAYED), cFI(UNPLAYED));
        Feed f5 = createFeed(5, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(PLAYED), cFI(PLAYED)); // played time to be set later
        Feed f6 = createFeed(6, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED)); // in-progress, to be set later
        FeedsAccessor a = saveFeeds(f0, f1, f2, f3, f4, f5, f6);
        { // mark fi(1,0) as downloaded, so the feed will be pushed to the end
            FeedMedia fmDownloaded =  a.fi(1, 0).getMedia();
            fmDownloaded.setFile_url("file://downloaded.mp3"); // MUST set, or setDownloaded will be useless
            fmDownloaded.setDownloaded(true);
            DBWriter.setFeedMedia(fmDownloaded).get();
        }
        { // ensure fi(3,2) is the ongoing one (with the most recent played timestamp)
            // also set the last playback time for some media to be more realistic
            setLastPlaybackTimeDescending(a.fi(5,1), a.fi(6,0), a.fi(5,0), a.fi(3,2), a.fi(0,0));
        }

        List<Long> expected = toIds(a.fi(4, 0), a.fi(2, 0), a.fi(3, 3), a.fi(1,1));

        // Run actual test

        // Now create the selector under test and exercise it
        DownloadItemSelectorSerialImpl selector = new DownloadItemSelectorSerialImpl();

        // Test individual components of the implementation
        {
            FeedItem lastPlayed = selector.getLastPlayedSerialFeedItem();
            assertEquals("getLastPlayedSerialFeedItem()",
                    a.fi(3,2), lastPlayed);
        }
        {
            List<Feed> actual = selector.getSerialFeedsOrderedByDownloadOrder();
            assertEquals("getSerialFeedsOrderedById() - actual: " + actual,
                    toIds(f4, f0, f1, f2, f3), toIds(actual));
        }
        {
            FeedItem nextToDownload = selector.getNextItemToDownloadForSerial(f3);
            assertEquals("getNextItemToDownloadForSerial() - the one after the ongoing",
                    a.fi(3,3), nextToDownload);
        }
        {
            FeedItem nextToDownload = selector.getNextItemToDownloadForSerial(f1);
            assertEquals("getNextItemToDownloadForSerial() - skip downloaded",
                    a.fi(1,1), nextToDownload);
        }

        // Test overall
        List<? extends FeedItem> fiActual = selector.getAutoDownloadableEpisodes();
        assertEquals("Basic, ongoing case -  It returns: " + fiActual,
                expected, toIds(fiActual));
    }

    @Test
    public void boundary_noSerialFeed() throws Exception {
        Feed f0 = createFeed(0, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED));
        Feed f1 = createFeed(1, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED));
        FeedsAccessor a = saveFeeds(f0, f1);

        // Now create the selector under test and exercise it
        DownloadItemSelectorSerialImpl selector = new DownloadItemSelectorSerialImpl();
        List<? extends FeedItem> fiActual = selector.getAutoDownloadableEpisodes();
        assertEquals("Basic, no serial feed -  It returns: " + fiActual,
                Collections.emptyList(), toIds(fiActual));

    }

    @Test
    public void boundary_noSerialFeedWithDownloadables() throws Exception {
        Feed f0 = createFeed(0, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED));
        Feed f1 = createFeed(1, SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(UNPLAYED), // but downloaded (to be set later)
                cFI(PLAYED));
        FeedsAccessor a = saveFeeds(f0, f1);
        { // mark fi(1,0) as downloaded, so the feed will be pushed to the end
            FeedMedia fmDownloaded =  a.fi(1, 0).getMedia();
            fmDownloaded.setFile_url("file://downloaded.mp3"); // MUST set, or setDownloaded will be useless
            fmDownloaded.setDownloaded(true);
            DBWriter.setFeedMedia(fmDownloaded).get();
        }

        // Now create the selector under test and exercise it
        DownloadItemSelectorSerialImpl selector = new DownloadItemSelectorSerialImpl();
        List<? extends FeedItem> fiActual = selector.getAutoDownloadableEpisodes();
        assertEquals("Basic, no serial feed with downloadables -  It returns: " + fiActual,
                Collections.emptyList(), toIds(fiActual));
    }

    private void setLastPlaybackTimeDescending(FeedItem... feedItems) throws Exception {
        final long lastPlaybackTimeBase = System.currentTimeMillis() + 1000 * feedItems.length;

        for (int i =0; i < feedItems.length; i++) {
            FeedItem item = feedItems[i];
            FeedMedia media = item.getMedia();
            media.setLastPlayedTime(lastPlaybackTimeBase - i * 1000);
            if (item.isPlayed()) {
                media.setPlaybackCompletionDate(new Date(lastPlaybackTimeBase - i * 1000));
            }
            DBWriter.setFeedMedia(media).get();
        }
    }
}
