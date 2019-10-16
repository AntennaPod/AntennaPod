package de.test.antennapod.storage;

import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DownloadItemSelectorEpisodicImpl;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.test.antennapod.storage.FeedTestUtil.FeedsAccessor;

import static de.danoeh.antennapod.core.feed.FeedItem.NEW;
import static de.danoeh.antennapod.core.feed.FeedItem.PLAYED;
import static de.danoeh.antennapod.core.feed.FeedItem.UNPLAYED;
import static de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType.EPISODIC;
import static de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType.SERIAL;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.AUTO_DL_FALSE;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.AUTO_DL_TRUE;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.KEEP_UPDATED_FALSE;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.KEEP_UPDATED_TRUE;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.cFI;
import static de.test.antennapod.storage.DownloadItemSelectorTestUtil.createFeed;
import static de.test.antennapod.storage.FeedTestUtil.saveFeeds;
import static de.test.antennapod.storage.FeedTestUtil.toIds;
import static org.junit.Assert.assertEquals;

public class DownloadItemSelectorEpisodicImplTest {

    private static final String TAG = "DlItemSlctrEpisodicTest";

    private static final boolean DEBUG = false;

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
    public void basic() throws Exception {
        // Setup test data and expectation

        // typical auto-downloadable feed
        Feed f0 = createFeed(0, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

        // have an include filter "1" that will accept "item 1" only
        Feed f1 = createFeed(1, EPISODIC, AUTO_DL_TRUE, "1", KEEP_UPDATED_TRUE,
                cFI(NEW), cFI(NEW), cFI(NEW));

        // non auto-downloadable feed
        Feed f2 = createFeed(2, EPISODIC, AUTO_DL_FALSE, "", KEEP_UPDATED_TRUE,
                cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

        // feed not keep updated, which trumps auto-download settings
        Feed f3 = createFeed(3, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_FALSE,
                cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

        FeedsAccessor a =  saveFeeds(f0, f1, f2, f3);

        // the result is ordered by pubDate descending,
        List<Long> expectedNewItemIds = Arrays.asList(
                a.fiId(1, 1), // fi(1, 0) is excluded by the feed includeFilter
                a.fiId(0, 0)
        );

        debugAllFeeds();

        // Now create the selector under test and exercise it
        DownloadItemSelectorEpisodicImpl selector =
                new DownloadItemSelectorEpisodicImpl();

        List<? extends FeedItem> fiAutoDlActual =
                selector.getAutoDownloadableEpisodes();

        assertEquals("Results should include only auto-downloadable new items. It returns: " + fiAutoDlActual,
                expectedNewItemIds, toIds(fiAutoDlActual));
    }

    @Test
    public void excludeSerials() {
        // Setup test data and expectation

        // serial feeds - to be ignored
        Feed f0 = createFeed(0, SERIAL, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

        // typical auto-downloadable feed
        Feed f1 = createFeed(1, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

        // typical auto-downloadable feed
        Feed f2 = createFeed(2, EPISODIC, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                cFI(PLAYED), cFI(UNPLAYED), cFI(NEW));

        FeedsAccessor a =  saveFeeds(f0, f1, f2);

        // serial feed, f0, is ignored
        List<Long> expectedNewItemIds = Arrays.asList(a.fiId(2, 2), a.fiId(1, 0));

        // Now create the selector under test and exercise it
        DownloadItemSelectorEpisodicImpl selector =
                new DownloadItemSelectorEpisodicImpl();

        List<? extends FeedItem> fiAutoDlActual =
                selector.getAutoDownloadableEpisodes();

        assertEquals("Results should include episodic feed items. It returns: " + fiAutoDlActual,
                expectedNewItemIds, toIds(fiAutoDlActual));
    }

    private void debugAllFeeds() {
        if (DEBUG) {
            // verbose output of the feeds saved, in case the test fails
            // it re-reads from DB to ensure they are up-to-date.
            for (Feed f : DBReader.getFeedList()) {
                debug("feed " + f.getId() + " , " + f.getTitle()
                        + " , autodownload=" + f.getPreferences().getAutoDownload());
                for (FeedItem fi : DBReader.getFeedItemList(f)) {
                    debug("  fi " + fi.getId() + " , " + fi.getTitle() + ", pubDate=" + fi.getPubDate()
                            + ", autodownload=" + fi.getAutoDownload() + ", new=" + fi.isNew());
                }
            }
        }
    }

    private void debug(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
