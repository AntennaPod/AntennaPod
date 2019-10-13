package de.test.antennapod.storage;

import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DownloadItemSelectorEpisodicImpl;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import static de.danoeh.antennapod.core.feed.FeedItem.NEW;
import static de.danoeh.antennapod.core.feed.FeedItem.PLAYED;
import static de.danoeh.antennapod.core.feed.FeedItem.UNPLAYED;
import static org.junit.Assert.assertEquals;

public class DownloadItemSelectorEpisodicImplTest {

    private static final String TAG = "DlItemSlctrEpisodicTest";

    private static final boolean DEBUG = false;

    private static final boolean AUTO_DL_TRUE = true;
    private static final boolean AUTO_DL_FALSE = false;
    private static final boolean KEEP_UPDATED_TRUE = true;
    private static final boolean KEEP_UPDATED_FALSE = false;

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
    public void testGetAutoDownloadableEpisodes() throws Exception {
        // Setup test data and expectation
        List<Long> expectedNewItemIds = new ArrayList<>();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        try {
            adapter.open();

            // typical auto-downloadable feed
            Feed f0 = createFeed(0, AUTO_DL_TRUE, "", KEEP_UPDATED_TRUE,
                    cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

            // have an include filter "1" that will accept "item 1" only
            Feed f1 = createFeed(1, AUTO_DL_TRUE, "1", KEEP_UPDATED_TRUE,
                    cFI(NEW), cFI(NEW), cFI(NEW));

            // non auto-downloadable feed
            Feed f2 = createFeed(2, AUTO_DL_FALSE, "", KEEP_UPDATED_TRUE,
                    cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

            // feed not keep updated, which trumps auto-download settings
            Feed f3 = createFeed(3, AUTO_DL_TRUE, "", KEEP_UPDATED_FALSE,
                    cFI(NEW), cFI(UNPLAYED), cFI(PLAYED));

            adapter.setCompleteFeed(f0, f1, f2, f3);

            expectedNewItemIds.add((f0.getItemAtIndex(0).getId()));
            expectedNewItemIds.add((f1.getItemAtIndex(1).getId()));

            // the result is ordered by pubDate descending, thus reversing them
            Collections.reverse(expectedNewItemIds);
        } finally {
            adapter.close();
        }

        debugAllFeeds();

        // Now create the selector under test and exercise it
        DownloadItemSelectorEpisodicImpl selector =
                new DownloadItemSelectorEpisodicImpl(new APDownloadAlgorithm.ItemProviderDefaultImpl());

        List<? extends FeedItem> fiAutoDlActual =
                selector.getAutoDownloadableEpisodes();

        assertEquals("Results should include only auto-downloadable new items. It returns: " + fiAutoDlActual,
                expectedNewItemIds, toItemIds(fiAutoDlActual));
    }

    private List<Long> toItemIds(List<? extends FeedItem> feedItems) {
        List<Long> result = new ArrayList<>(feedItems.size());
        for (FeedItem fi : feedItems) {
            result.add(fi.getId());
        }
        return result;
    }

    //
    // Helpers to populate test data
    //

    private static long curPubDateMillis = System.currentTimeMillis() - 100000;

    private static Feed createFeed(int titleId, boolean isAutoDownload, String includeFilter, boolean isKeepUpdated,
                                   FeedItem... feedItems) {
        Feed f = new Feed(0, null, "feed " + titleId, null, "link" + titleId, "descr", null, null,
                null, null, "id" + titleId, null, null, "url" + titleId, false, false, null, null, false);

        FeedPreferences fPrefs =
                new FeedPreferences(0, isAutoDownload, FeedPreferences.AutoDeleteAction.GLOBAL, null, null);
        fPrefs.setKeepUpdated(isKeepUpdated);
        fPrefs.setFilter(new FeedFilter(includeFilter, ""));
        f.setPreferences(fPrefs);

        for (int j = 0; j < feedItems.length; j++) {
            FeedItem fi = feedItems[j];
            fi.setFeed(f);
            curPubDateMillis += 1000; // ensure p
            fi.setPubDate(new Date(curPubDateMillis));
            fi.setAutoDownload(isAutoDownload);
            fi.setTitle("item " + j);

            f.getItems().add(fi);
        }

        return f;
    }

    /**
     * @return a skeleton (incomplete) FeedItem of the specified state, createFeed() will fill in the details.
     */
    private static FeedItem cFI(int playState) {
        FeedItem item = new FeedItem();
        switch (playState) {
            case NEW:
                item.setNew();
                break;
            case UNPLAYED:
                item.setPlayed(false);
                break;
            case PLAYED:
                item.setPlayed(true);
                break;
            default:
                throw new IllegalArgumentException("Invalid playState: " + playState);
        }

        FeedMedia media = new FeedMedia(item, "url", 1, "audio/mp3");
        item.setMedia(media);

        return item;
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
