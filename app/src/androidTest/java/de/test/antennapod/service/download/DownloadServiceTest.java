package de.test.antennapod.service.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.event.DownloadEvent;
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

import static org.junit.Assert.assertEquals;
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
        FeedMedia media1 = new FeedMedia(0, item1, 123, 1, 1, "mime_type", null, "download_url", false, null, 0, 0);
        item1.setMedia(media1);

        DBWriter.setFeedItem(item1).get();
        trace("feed.id=" + feed.getId() + " , item1.id=" + item1.getId());
        return feed;
    }


    @After
    public void tearDown() throws Exception {
        DownloadService.setDownloaderFactory(origFactory);
    }

    private static enum DownloadState {
        UNKNOWN, IN_PROGRESS, DONE_SUCCESS
    }


    private interface EventsAsserter {
        int getExpectedNumSignificantEvents();
    }

    private class DownloadSuccessEventsAsserter implements EventsAsserter {
        private DownloadState downloadState = DownloadState.UNKNOWN;

        @Override
        public int getExpectedNumSignificantEvents() {
            return 2; // in-progress, then an explicit success
        }

        /**
         * The actual logic in asserting the sequence of DownloadEvents generated,
         * as well as the correspond states in the database.
         */
        @Subscribe
        public void onEvent(DownloadEvent event) {
            DownloadStatus status = getDownloadStatus(event, FeedMedia.FEEDFILETYPE_FEEDMEDIA, testMedia11.getId());
            trace("DownloadEvent: " + event);
            trace("  status: " + status);
            if (status == null) {
                return; // no relevant update
                // OPEN: We don't care for cases that the downloader is removed (once it's complete/cancelled)
            }
            if (downloadState == DownloadState.UNKNOWN) {
                if (!status.isDone()) {
                    downloadState = DownloadState.IN_PROGRESS;
                    latch.countDown();
                } else {
                    fail("Unexpected download status, current state = " + downloadState + " status: " + status);
                }
            } else if (downloadState == DownloadState.IN_PROGRESS) {
                if (status.isSuccessful()) {
                    FeedMedia fmUpdated = DBReader.getFeedMedia(testMedia11.getId());
                    // Ensure when media download success message is generated,
                    // the state in the DB must have been brought up-to-date.
                    assertTrue("Downloaded state in db should have been set",
                            fmUpdated.isDownloaded());
                    assertTrue("Download file path should have been set",
                            StringUtils.isNotEmpty(fmUpdated.getLocalMediaUrl()));
                    downloadState = DownloadState.DONE_SUCCESS;
                    latch.countDown();
                } else if (!status.isDone()) {
                    // still in progress, do nothing
                } else {
                    fail("Unexpected download status, current state = " + downloadState + " status: " + status);
                }
            }
        }
    }

    private static DownloadStatus getDownloadStatus(DownloadEvent event, int fileType, long fileId) {
        for (Downloader downloader : event.update.downloaders) {
            if (downloader.getDownloadRequest().getFeedfileType() == fileType &&
                    downloader.getDownloadRequest().getFeedfileId() == fileId) {
                return downloader.getResult();
            }
        }
        return null;
    }

    @Test
    public void testDownloadEventsGeneratedCaseDownloadSuccess() throws Exception {
        // create a stub download that returns successful
        //
        // OPEN: Ideally, I'd like the download time long enough so that multiple in-progress DownloadEvents
        // are generated (to simulate typical download), but it'll make download time quite long (1-2 seconds)
        // to do so
        DownloadService.setDownloaderFactory(new StubDownloaderFactory(50, downloadStatus -> {
           downloadStatus.setSuccessful();
        }));

        DownloadSuccessEventsAsserter eventsAsserter = new DownloadSuccessEventsAsserter();
        EventBus.getDefault().register(eventsAsserter);
        latch = new CountDownLatch(eventsAsserter.getExpectedNumSignificantEvents());

        try {
            DownloadRequester.getInstance().downloadMedia(InstrumentationRegistry.getTargetContext(),
                    testMedia11);
            latch.await(1000, TimeUnit.MILLISECONDS);
            assertEquals("Ensuring all expected significant events have been generated.",
                    0, latch.getCount());
        } finally {
            EventBus.getDefault().unregister(eventsAsserter);
        }
    }

    private static void trace(String msg) {
//        System.err.println("DBG - " + msg);
    }

    private static class StubDownloaderFactory implements DownloadService.DownloaderFactory {
        private final long downloadTime;

        @NonNull
        private final Consumer<DownloadStatus> onDownloadComplete;

        public StubDownloaderFactory(long downloadTime, @NonNull Consumer<DownloadStatus> onDownloadComplete) {
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
