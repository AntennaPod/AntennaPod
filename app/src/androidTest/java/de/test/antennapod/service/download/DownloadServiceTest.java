package de.test.antennapod.service.download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.StubDownloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Consumer;

import static de.test.antennapod.util.event.FeedItemEventListener.withFeedItemEventListener;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @see HttpDownloaderTest for the test of actual download (and saving the file)
 */
@RunWith(AndroidJUnit4.class)
public class DownloadServiceTest {

    private CountDownLatch latch = null;
    private Feed testFeed = null;
    private FeedMedia testMedia11 = null;

    private DownloadService.DownloaderFactory origFactory = null;

    @Before
    public void setUp() throws Exception {
        origFactory = DownloadService.getDownloaderFactory();
        testFeed = setUpTestFeeds();
        testMedia11 = testFeed.getItemAtIndex(0).getMedia();
    }

    private Feed setUpTestFeeds() throws Exception {
        Feed feed = new Feed("url", null, "Test Feed title 1");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        FeedItem item1 = new FeedItem(0, "Item 1-1", "Item 1-1", "url", new Date(), FeedItem.NEW, feed);
        items.add(item1);
        FeedMedia media1 = new FeedMedia(0, item1, 123, 1, 1, "audio/mp3", null, "http://example.com/episode.mp3", false, null, 0, 0);
        item1.setMedia(media1);

        DBWriter.setFeedItem(item1).get();
        return feed;
    }


    @After
    public void tearDown() throws Exception {
        DownloadService.setDownloaderFactory(origFactory);
    }

    @Test
    public void testEventsGeneratedCaseMediaDownloadSuccess() throws Exception {
        // create a stub download that returns successful
        //
        // OPEN: Ideally, I'd like the download time long enough so that multiple in-progress DownloadEvents
        // are generated (to simulate typical download), but it'll make download time quite long (1-2 seconds)
        // to do so
        DownloadService.setDownloaderFactory(new StubDownloaderFactory(50, downloadStatus -> {
           downloadStatus.setSuccessful();
        }));

        withFeedItemEventListener(feedItemEventListener -> {
            try {
                assertEquals(0, feedItemEventListener.getEvents().size());
                assertFalse("The media in test should not yet been downloaded",
                        DBReader.getFeedMedia(testMedia11.getId()).isDownloaded());

                DownloadRequester.getInstance().downloadMedia(InstrumentationRegistry.getTargetContext(),
                        testMedia11);
                Awaitility.await()
                        .atMost(1000, TimeUnit.MILLISECONDS)
                        .until(() -> feedItemEventListener.getEvents().size() > 0);
                assertTrue("After media download has completed, FeedMedia object in db should indicate so.",
                        DBReader.getFeedMedia(testMedia11.getId()).isDownloaded());
            } catch (ConditionTimeoutException cte) {
                fail("The expected FeedItemEvent (for media download complete) has not been posted. "
                        + cte.getMessage());
            }
        });
    }

    private static class StubDownloaderFactory implements DownloadService.DownloaderFactory {
        private final long downloadTime;

        @NonNull
        private final Consumer<DownloadStatus> onDownloadComplete;

        StubDownloaderFactory(long downloadTime, @NonNull Consumer<DownloadStatus> onDownloadComplete) {
            this.downloadTime = downloadTime;
            this.onDownloadComplete = onDownloadComplete;
        }

        @Nullable
        @Override
        public Downloader create(@NonNull DownloadRequest request) {
            return new StubDownloader(request, downloadTime, onDownloadComplete);
        }
    }

}
