package de.test.antennapod.ui;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the UITestUtils. Makes sure that all URLs are reachable and that the class does not cause any crashes.
 */
@MediumTest
public class UITestUtilsTest {

    private UITestUtils uiTestUtils;

    @Before
    public void setUp() throws Exception {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testAddHostedFeeds() throws Exception {
        uiTestUtils.addHostedFeedData();
        final List<Feed> feeds = uiTestUtils.hostedFeeds;
        assertNotNull(feeds);
        assertFalse(feeds.isEmpty());

        for (Feed feed : feeds) {
            testUrlReachable(feed.getDownload_url());
            for (FeedItem item : feed.getItems()) {
                if (item.hasMedia()) {
                    testUrlReachable(item.getMedia().getDownload_url());
                }
            }
        }
    }

    public void testUrlReachable(String strUtl) throws Exception {
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

    @Test
    public void testAddLocalFeedDataNoDownload() throws Exception {
        addLocalFeedDataCheck(false);
    }

    @Test
    public void testAddLocalFeedDataDownload() throws Exception {
        addLocalFeedDataCheck(true);
    }
}
