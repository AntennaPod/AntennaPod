package de.test.antennapod.ui;

import android.test.InstrumentationTestCase;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * Test for the UITestUtils. Makes sure that all URLs are reachable and that the class does not cause any crashes.
 */
public class UITestUtilsTest extends InstrumentationTestCase {

    private UITestUtils uiTestUtils;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        uiTestUtils = new UITestUtils(getInstrumentation().getTargetContext());
        uiTestUtils.setup();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        uiTestUtils.tearDown();
    }

    public void testAddHostedFeeds() throws Exception {
        uiTestUtils.addHostedFeedData();
        final List<Feed> feeds = uiTestUtils.hostedFeeds;
        assertNotNull(feeds);
        assertFalse(feeds.isEmpty());

        for (Feed feed : feeds) {
            testUrlReachable(feed.getDownload_url());
            if (feed.getImage() != null) {
                testUrlReachable(feed.getImage().getDownload_url());
            }
            for (FeedItem item : feed.getItems()) {
                if (item.hasMedia()) {
                    testUrlReachable(item.getMedia().getDownload_url());
                }
            }
        }
    }

    private void testUrlReachable(String strUtl) throws Exception {
        URL url = new URL(strUtl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int rc = conn.getResponseCode();
        assertEquals(HttpURLConnection.HTTP_OK, rc);
        conn.disconnect();
    }

    private void addLocalFeedDataCheck(boolean downloadEpisodes) throws Exception {
        uiTestUtils.addLocalFeedData(downloadEpisodes);
        assertNotNull(uiTestUtils.hostedFeeds);
        assertFalse(uiTestUtils.hostedFeeds.isEmpty());

        for (Feed feed : uiTestUtils.hostedFeeds) {
            assertTrue(feed.getId() != 0);
            if (feed.getImage() != null) {
                assertTrue(feed.getImage().getId() != 0);
            }
            for (FeedItem item : feed.getItems()) {
                assertTrue(item.getId() != 0);
                if (item.hasMedia()) {
                    assertTrue(item.getMedia().getId() != 0);
                    if (downloadEpisodes) {
                        assertTrue(item.getMedia().isDownloaded());
                        assertNotNull(item.getMedia().getFile_url());
                        File file = new File(item.getMedia().getFile_url());
                        assertTrue(file.exists());
                    }
                }
            }
        }
    }

    public void testAddLocalFeedDataNoDownload() throws Exception {
        addLocalFeedDataCheck(false);
    }

    public void testAddLocalFeedDataDownload() throws Exception {
        addLocalFeedDataCheck(true);
    }
}
