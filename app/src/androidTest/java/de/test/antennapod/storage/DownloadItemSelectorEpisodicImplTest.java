package de.test.antennapod.storage;

import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadItemSelectorEpisodicImpl;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

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
    public void testGetAutoDownloadableEpisodes() throws Exception {
        // Setup test data and expectation
        List<Long> expectedItemIds = new ArrayList<>();
        {
            List<Feed> feeds = DBTestUtils.saveFeedlist(4, 3, true);
            {
                Feed fAutoDl = feeds.get(0);

                FeedItem fi0_in = fAutoDl.getItemAtIndex(0);
                fi0_in.setNew();
                DBWriter.setFeedItem(fi0_in).get();
                expectedItemIds.add(fi0_in.getId());

                FeedItem fi0_outUnplayed = fAutoDl.getItemAtIndex(1);
                fi0_outUnplayed.setPlayed(false);
                DBWriter.setFeedItem(fi0_outUnplayed).get();

                FeedItem fi0_outPlayed = fAutoDl.getItemAtIndex(1);
                fi0_outPlayed.setPlayed(true);
                DBWriter.setFeedItem(fi0_outPlayed).get();

                // set feed download preferences
                // Do it after updating feedItems, before some feedItem operations
                // could overwrite the settings,
                fAutoDl.getPreferences().setAutoDownload(true);
                DBWriter.setFeedPreferences(fAutoDl.getPreferences()).get();
                DBWriter.setFeedsItemsAutoDownload(fAutoDl, true).get();
            }

            {
                Feed fAutoDlSome = feeds.get(1);

                // title "item 0" is not in the include filter below
                FeedItem fi1_outByFilter = fAutoDlSome.getItemAtIndex(0);
                fi1_outByFilter.setNew();
                DBWriter.setFeedItem(fi1_outByFilter).get();
                FeedItem fi1_in = fAutoDlSome.getItemAtIndex(1);
                fi1_in.setNew();
                DBWriter.setFeedItem(fi1_in).get();
                expectedItemIds.add(fi1_in.getId());

                // Find the item number to be set as the include filter
                Matcher matcher = Pattern.compile("item (\\d+)").matcher(fi1_in.getTitle());
                matcher.matches();
                String inFilterStr = matcher.group(1);
                debug("include filter for feed " + fAutoDlSome.getId() + " : " + inFilterStr);

                fAutoDlSome.getPreferences().setAutoDownload(true);
                DBWriter.setFeedsItemsAutoDownload(fAutoDlSome, true).get();
                fAutoDlSome.getPreferences().setFilter(new FeedFilter(inFilterStr, ""));
                DBWriter.setFeedPreferences(fAutoDlSome.getPreferences()).get();
            }

            {
                Feed fNotAutoDl = feeds.get(2);

                FeedItem fi2_out = fNotAutoDl.getItemAtIndex(0);
                fi2_out.setNew();
                DBWriter.setFeedItem(fi2_out).get();

                fNotAutoDl.getPreferences().setAutoDownload(false);
                DBWriter.setFeedPreferences(fNotAutoDl.getPreferences()).get();
                DBWriter.setFeedsItemsAutoDownload(fNotAutoDl, false).get();
            }

            {
                Feed fNotKeepUpdate = feeds.get(3);

                FeedItem fi3_out = fNotKeepUpdate.getItemAtIndex(0);
                fi3_out.setNew();
                DBWriter.setFeedItem(fi3_out).get();

                fNotKeepUpdate.getPreferences().setAutoDownload(true);
                DBWriter.setFeedsItemsAutoDownload(fNotKeepUpdate, true).get();
                fNotKeepUpdate.getPreferences().setKeepUpdated(false); // keep updated false make it gone
                DBWriter.setFeedPreferences(fNotKeepUpdate.getPreferences()).get();
            }

            if (DEBUG) {
                // verbose output of the feeds saved, in case the test fails
                // it re-reads from DB to ensure they are up-to-date.
                for (Feed f : DBReader.getFeedList()) {
                    debug("feed " + f.getId() + " , " + f.getTitle() + " , autodownload=" + f.getPreferences().getAutoDownload());
                    for (FeedItem fi : DBReader.getFeedItemList(f)) {
                        debug("  fi " + fi.getId() + " , " + fi.getTitle() + ", pubDate=" + fi.getPubDate()
                                + ", autodownload=" + fi.getAutoDownload() + ", new=" + fi.isNew());
                    }
                }
            }

            Collections.reverse(expectedItemIds); // the result is ordered by pubDate descending, thus reversing them
        }

        // Now create the selector under test
        DownloadItemSelectorEpisodicImpl selector = new DownloadItemSelectorEpisodicImpl();

        List<? extends FeedItem> fiAutoDlActual = selector.getAutoDownloadableEpisodes(new APDownloadAlgorithm.ItemProviderDefaultImpl());

        assertEquals("Results should include only auto-downlodable new items. It actually returns: " + fiAutoDlActual,
                expectedItemIds, toItemIds(fiAutoDlActual));
    }

    private List<Long> toItemIds(List<? extends FeedItem> feedItems) {
        List<Long> result = new ArrayList<>(feedItems.size());
        for (FeedItem fi : feedItems) {
            result.add(fi.getId());
        }
        return result;
    }

    private void debug(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }

}
