package de.danoeh.antennapod.core.storage;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.feed.FeedFile;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedMother;
import de.danoeh.antennapod.core.storage.DBWriter.ItemEnqueuePositionCalculator;
import de.danoeh.antennapod.core.storage.DBWriter.ItemEnqueuePositionCalculator.Options;

import static org.junit.Assert.assertEquals;

public class DBWriterTest {

    public static class ItemEnqueuePositionCalculatorTest {

        @RunWith(Parameterized.class)
        public static class IEPCBasicTest {
            @Parameters(name = "{index}: case<{0}>, expected:{1}")
            public static Iterable<Object[]> data() {
                Options optDefault = new Options();
                Options optEnqAtFront = new Options().setEnqueueAtFront(true);

                return Arrays.asList(new Object[][]{
                        {"case default, i.e., add to the end",
                                QUEUE_DEFAULT.size(), optDefault, 0, QUEUE_DEFAULT},
                        {"case default (2nd item)",
                                QUEUE_DEFAULT.size(), optDefault, 1, QUEUE_DEFAULT},
                        {"case option enqueue at front",
                                0, optEnqAtFront, 0, QUEUE_DEFAULT},
                        {"case option enqueue at front (2nd item)",
                                1, optEnqAtFront, 1, QUEUE_DEFAULT},
                        {"case empty queue, option default",
                                0, optDefault, 0, QUEUE_EMPTY},
                        {"case empty queue, option enqueue at front",
                                0, optEnqAtFront, 0, QUEUE_EMPTY},
                });
            }

            @Parameter
            public String message;

            @Parameter(1)
            public int posExpected;

            @Parameter(2)
            public Options options;

            @Parameter(3)
            public int posAmongAdded; // the position of feed item to be inserted among the list to be inserted.

            @Parameter(4)
            public List<FeedItem> curQueue;


            public static final int TFI_TO_ADD_ID = 101;

            /**
             * Add a FeedItem with ID {@link #TFI_TO_ADD_ID} with the setup
             */
            @Test
            public void test() {
                ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);

                int posActual = calculator.calcPosition(posAmongAdded, tFI(TFI_TO_ADD_ID), curQueue);
                assertEquals(message, posExpected, posActual);
            }

        }

        @RunWith(Parameterized.class)
        public static class IEPCKeepInProgressAtFrontTest extends IEPCBasicTest {
            @Parameters(name = "{index}: case<{0}>, expected:{1}")
            public static Iterable<Object[]> data() {
                Options optKeepInProgressAtFront =
                        new Options().setEnqueueAtFront(true).setKeepInProgressAtFront(true);
                // edge case: keep in progress without enabling enqueue at front is meaningless
                Options optKeepInProgressAtFrontWithNoEnqueueAtFront =
                        new Options().setKeepInProgressAtFront(true);

                return Arrays.asList(new Object[][]{
                        {"case option keep in progress at front",
                                1, optKeepInProgressAtFront, 0, QUEUE_FRONT_IN_PROGRESS},
                        {"case option keep in progress at front (2nd item)",
                                2, optKeepInProgressAtFront, 1, QUEUE_FRONT_IN_PROGRESS},
                        {"case option keep in progress at front, front item not in progress",
                                0, optKeepInProgressAtFront, 0, QUEUE_DEFAULT},
                        {"case option keep in progress at front, front item no media at all",
                                0, optKeepInProgressAtFront, 0, QUEUE_FRONT_NO_MEDIA}, // No media should not cause any exception
                        {"case option keep in progress at front, but enqueue at front is disabled",
                                QUEUE_FRONT_IN_PROGRESS.size(), optKeepInProgressAtFrontWithNoEnqueueAtFront, 0, QUEUE_FRONT_IN_PROGRESS},
                        {"case empty queue, option keep in progress at front",
                                0, optKeepInProgressAtFront, 0, QUEUE_EMPTY},
                });
            }

            private static final List<FeedItem> QUEUE_FRONT_IN_PROGRESS = Arrays.asList(tFI(11, 60000), tFI(12), tFI(13));

            private static final List<FeedItem> QUEUE_FRONT_NO_MEDIA = Arrays.asList(tFINoMedia(11), tFI(12), tFI(13));

        }

        @RunWith(Parameterized.class)
        public static class ItemEnqueuePositionCalculatorPreserveDownloadOrderTest {

            @Parameters(name = "{index}: case<{0}>, expected:{1}")
            public static Iterable<Object[]> data() {
                Options optDefault = new Options();
                Options optEnqAtFront = new Options().setEnqueueAtFront(true);

                return Arrays.asList(new Object[][] {
                        {"download order test, enqueue default",
                                QUEUE_DEFAULT.size(), QUEUE_DEFAULT.size() + 1,
                                QUEUE_DEFAULT.size() + 2, QUEUE_DEFAULT.size() + 3,
                                optDefault, QUEUE_DEFAULT},
                        {"download order test, enqueue at front",
                                0, 1,
                                2, 3,
                                optEnqAtFront, QUEUE_DEFAULT},
                });
            }

            @Parameter
            public String message;

            @Parameter(1)
            public int pos101Expected;

            @Parameter(2)
            public int pos102Expected;

            // 2XX are for testing bulk insertion cases
            @Parameter(3)
            public int pos201Expected;

            @Parameter(4)
            public int pos202Expected;

            @Parameter(5)
            public Options options;

            @Parameter(6)
            public List<FeedItem> queueInitial;

            @Test
            public void testQueueOrderWhenDownloading2Items() {

                // Setup class under test
                //
                ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);
                MockDownloadRequester mockDownloadRequester = new MockDownloadRequester();
                calculator.requester = mockDownloadRequester;

                // Setup initial data
                // A shallow copy, as the test code will manipulate the queue
                List<FeedItem> queue = new ArrayList<>(queueInitial);


                // Test body

                // User clicks download on feed item 101
                FeedItem tFI101 = tFI_isDownloading(101, mockDownloadRequester);

                int pos101Actual = calculator.calcPosition(0, tFI101, queue);
                queue.add(pos101Actual, tFI101);
                assertEquals(message + " (1st download)",
                        pos101Expected, pos101Actual);

                // Then user clicks download on feed item 102
                FeedItem tFI102 = tFI_isDownloading(102, mockDownloadRequester);
                int pos102Actual = calculator.calcPosition(0, tFI102, queue);
                queue.add(pos102Actual, tFI102);
                assertEquals(message + " (2nd download, it should preserve order of download)",
                        pos102Expected, pos102Actual);

                // Items 201 and 202 are added as part of a single DBWriter.addQueueItem() calls

                FeedItem tFI201 = tFI_isDownloading(201, mockDownloadRequester);
                int pos201Actual = calculator.calcPosition(0, tFI201, queue);
                queue.add(pos201Actual, tFI201);
                assertEquals(message + " (bulk insertion, 1st item)", pos201Expected, pos201Actual);

                FeedItem tFI202 = tFI_isDownloading(202, mockDownloadRequester);
                int pos202Actual = calculator.calcPosition(1, tFI202, queue);
                queue.add(pos202Actual, tFI202);
                assertEquals(message + " (bulk insertion, 2nd item)", pos202Expected, pos202Actual);

                // TODO: simulate download failure cases.
            }


            private static FeedItem tFI_isDownloading(int id, MockDownloadRequester requester) {
                FeedItem item = tFI(id);
                FeedMedia media =
                        new FeedMedia(item, "http://download.url.net/" + id
                                , 100000 + id, "audio/mp3");
                media.setId(item.getId());
                item.setMedia(media);

                requester.mockDownloadingFile(media, true);

                return item;
            }

            private static class MockDownloadRequester implements FeedFileDownloadStatusRequesterInterface {

                private Map<Long, Boolean> downloadingByIds = new HashMap<>();

                @Override
                public synchronized boolean isDownloadingFile(@NonNull FeedFile item) {
                    return downloadingByIds.getOrDefault(item.getId(), false);
                }

                // All other parent methods should not be called

                public void mockDownloadingFile(FeedFile item, boolean isDownloading) {
                    downloadingByIds.put(item.getId(), isDownloading);
                }
            }
        }


        // Common helpers:
        // - common queue (of items) for tests
        // - construct FeedItems for tests

        static final List<FeedItem> QUEUE_EMPTY = Collections.unmodifiableList(Arrays.asList());

        static final List<FeedItem> QUEUE_DEFAULT = Collections.unmodifiableList(Arrays.asList(tFI(11), tFI(12), tFI(13), tFI(14)));


        static FeedItem tFI(int id) {
            return tFI(id, -1);
        }

        static FeedItem tFI(int id, int position) {
            FeedItem item = tFINoMedia(id);
            FeedMedia media = new FeedMedia(item, "download_url", 1234567, "audio/mpeg");
            media.setId(item.getId());
            item.setMedia(media);

            if (position >= 0) {
                media.setPosition(position);
            }

            return item;
        }

        static FeedItem tFINoMedia(int id) {
            FeedItem item = new FeedItem(id, "Item" + id, "ItemId" + id, "url",
                    new Date(), FeedItem.PLAYED, FeedMother.anyFeed());
            return item;
        }
    }

}
