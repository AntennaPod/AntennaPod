package de.danoeh.antennapod.core.storage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedMother;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences.EnqueueLocation;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;

import static de.danoeh.antennapod.core.preferences.UserPreferences.EnqueueLocation.AFTER_CURRENTLY_PLAYING;
import static de.danoeh.antennapod.core.preferences.UserPreferences.EnqueueLocation.BACK;
import static de.danoeh.antennapod.core.preferences.UserPreferences.EnqueueLocation.FRONT;
import static de.danoeh.antennapod.core.util.CollectionTestUtil.concat;
import static de.danoeh.antennapod.core.util.CollectionTestUtil.list;
import static de.danoeh.antennapod.core.util.FeedItemUtil.getIdList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class ItemEnqueuePositionCalculatorTest {

    @RunWith(Parameterized.class)
    public static class BasicTest {
        @Parameters(name = "{index}: case<{0}>, expected:{1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"case default, i.e., add to the end",
                            concat(QUEUE_DEFAULT_IDS, TFI_ID),
                            BACK, 0, QUEUE_DEFAULT},
                    {"case default (2nd item)",
                            concat(QUEUE_DEFAULT_IDS, TFI_ID),
                            BACK, 1, QUEUE_DEFAULT},
                    {"case option enqueue at front",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            FRONT, 0, QUEUE_DEFAULT},
                    {"case option enqueue at front (2nd item)",
                            list(11L, TFI_ID, 12L, 13L, 14L),
                            FRONT, 1, QUEUE_DEFAULT},
                    {"case empty queue, option default",
                            list(TFI_ID),
                            BACK, 0, QUEUE_EMPTY},
                    {"case empty queue, option enqueue at front",
                            list(TFI_ID),
                            FRONT, 0, QUEUE_EMPTY},
            });
        }

        @Parameter
        public String message;

        @Parameter(1)
        public List<Long> idsExpected;

        @Parameter(2)
        public EnqueueLocation options;

        @Parameter(3)
        public int posAmongAdded; // the position of feed item to be inserted among the list to be inserted.

        @Parameter(4)
        public List<FeedItem> curQueue;

        public static final long TFI_ID = 101;

        /**
         * Add a FeedItem with ID {@link #TFI_ID} with the setup
         */
        @Test
        public void test() {
            ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);

            // shallow copy to which the test will add items
            List<FeedItem> queue = new ArrayList<>(curQueue);
            FeedItem tFI = createFeedItem(TFI_ID);
            doAddToQueueAndAssertResult(message,
                    calculator, posAmongAdded, tFI, queue, getCurrentlyPlaying(),
                    idsExpected);
        }

        Playable getCurrentlyPlaying() { return null; }
    }

    @RunWith(Parameterized.class)
    public static class AfterCurrentlyPlayingTest extends BasicTest {
        @Parameters(name = "{index}: case<{0}>, expected:{1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"case option after currently playing",
                            list(11L, TFI_ID, 12L, 13L, 14L),
                            AFTER_CURRENTLY_PLAYING, 0, QUEUE_DEFAULT, 11L},
                    {"case option after currently playing (2nd item)",
                            list(11L, 12L, TFI_ID, 13L, 14L),
                            AFTER_CURRENTLY_PLAYING, 1, QUEUE_DEFAULT, 11L},
                    {"case option after currently playing, currently playing in the middle of the queue",
                            list(11L, 12L, 13L, TFI_ID, 14L),
                            AFTER_CURRENTLY_PLAYING, 0, QUEUE_DEFAULT, 13L},
                    {"case option after currently playing, currently playing is not in queue",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            AFTER_CURRENTLY_PLAYING, 0, QUEUE_DEFAULT, 99L},
                    {"case option after currently playing, no currentlyPlaying is null",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            AFTER_CURRENTLY_PLAYING, 0, QUEUE_DEFAULT, ID_CURRENTLY_PLAYING_NULL},
                    {"case option after currently playing, currentlyPlaying is externalMedia",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            AFTER_CURRENTLY_PLAYING, 0, QUEUE_DEFAULT, ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA},
                    {"case empty queue, option after currently playing",
                            list(TFI_ID),
                            AFTER_CURRENTLY_PLAYING, 0, QUEUE_EMPTY, ID_CURRENTLY_PLAYING_NULL},
            });
        }

        @Parameter(5)
        public long idCurrentlyPlaying = -1;

        @Override
        Playable getCurrentlyPlaying() {
            if (ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA == idCurrentlyPlaying) {
                return externalMedia();
            }
            if (ID_CURRENTLY_PLAYING_NULL == idCurrentlyPlaying) {
                return null;
            }
            return createFeedItem(idCurrentlyPlaying).getMedia();
        }

        private static Playable externalMedia() {
            return new ExternalMedia("http://example.com/episode.mp3", MediaType.AUDIO);
        }

        private static final long ID_CURRENTLY_PLAYING_NULL = -1L;
        private static final long ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA = -9999L;

    }

    @RunWith(Parameterized.class)
    public static class ItemEnqueuePositionCalculatorPreserveDownloadOrderTest {

        @Parameters(name = "{index}: case<{0}>")
        public static Iterable<Object[]> data() {
            // Attempts to make test more readable by showing the expected list of ids
            // (rather than the expected positions)
            return Arrays.asList(new Object[][] {
                    {"download order test, enqueue default",
                            concat(QUEUE_DEFAULT_IDS, 101L),
                            concat(QUEUE_DEFAULT_IDS, list(101L, 102L)),
                            concat(QUEUE_DEFAULT_IDS, list(101L, 102L, 201L)),
                            concat(QUEUE_DEFAULT_IDS, list(101L, 102L, 201L, 202L)),
                            BACK, QUEUE_DEFAULT},
                    {"download order test, enqueue at front",
                            concat(101L, QUEUE_DEFAULT_IDS),
                            concat(list(101L, 102L), QUEUE_DEFAULT_IDS),
                            concat(list(101L, 102L, 201L), QUEUE_DEFAULT_IDS),
                            concat(list(101L, 102L, 201L, 202L), QUEUE_DEFAULT_IDS),
                            FRONT, QUEUE_DEFAULT},
            });
        }

        @Parameter
        public String message;

        @Parameter(1)
        public List<Long> idsExpectedAfter101;

        @Parameter(2)
        public List<Long> idsExpectedAfter102;

        // 2XX are for testing bulk insertion cases
        @Parameter(3)
        public List<Long> idsExpectedAfter201;

        @Parameter(4)
        public List<Long> idsExpectedAfter202;

        @Parameter(5)
        public EnqueueLocation options;

        @Parameter(6)
        public List<FeedItem> queueInitial;

        @Test
        public void testQueueOrderWhenDownloading2Items() {

            // Setup class under test
            //
            ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);
            DownloadStateProvider stubDownloadStateProvider = mock(DownloadStateProvider.class);
            stub(stubDownloadStateProvider.isDownloadingFile(any(FeedMedia.class))).toReturn(false);
            calculator.downloadStateProvider = stubDownloadStateProvider;

            // Setup initial data
            // A shallow copy, as the test code will manipulate the queue
            List<FeedItem> queue = new ArrayList<>(queueInitial);


            // Test body

            // User clicks download on feed item 101
            FeedItem tFI101 = setAsDownloading(101, stubDownloadStateProvider);
            doAddToQueueAndAssertResult(message + " (1st download)",
                    calculator, 0, tFI101, queue,
                    idsExpectedAfter101);

            // Then user clicks download on feed item 102
            FeedItem tFI102 = setAsDownloading(102, stubDownloadStateProvider);
            doAddToQueueAndAssertResult(message + " (2nd download, it should preserve order of download)",
                    calculator, 0, tFI102, queue,
                    idsExpectedAfter102);

            // Items 201 and 202 are added as part of a single DBWriter.addQueueItem() calls

            FeedItem tFI201 = setAsDownloading(201, stubDownloadStateProvider);
            doAddToQueueAndAssertResult(message + " (bulk insertion, 1st item)",
                    calculator, 0, tFI201, queue,
                    idsExpectedAfter201);

            FeedItem tFI202 = setAsDownloading(202, stubDownloadStateProvider);
            doAddToQueueAndAssertResult(message + " (bulk insertion, 2nd item)",
                    calculator, 1, tFI202, queue, null,
                    idsExpectedAfter202);

            // TODO: simulate download failure cases.
        }


        private static FeedItem setAsDownloading(int id, DownloadStateProvider stubDownloadStateProvider) {
            FeedItem item = createFeedItem(id);
            FeedMedia media =
                    new FeedMedia(item, "http://download.url.net/" + id
                            , 100000 + id, "audio/mp3");
            media.setId(item.getId());
            item.setMedia(media);

            stub(stubDownloadStateProvider.isDownloadingFile(media)).toReturn(true);

            return item;
        }
    }


    // Common helpers:
    // - common queue (of items) for tests
    // - construct FeedItems for tests

    static void doAddToQueueAndAssertResult(String message,
                                            ItemEnqueuePositionCalculator calculator,
                                            int positionAmongAdd,
                                            FeedItem itemToAdd,
                                            List<FeedItem> queue,
                                            List<Long> idsExpected) {
        doAddToQueueAndAssertResult(message, calculator, positionAmongAdd, itemToAdd, queue, null, idsExpected);
    }

    static void doAddToQueueAndAssertResult(String message,
                                            ItemEnqueuePositionCalculator calculator,
                                            int positionAmongAdd,
                                            FeedItem itemToAdd,
                                            List<FeedItem> queue,
                                            Playable currentlyPlaying,
                                            List<Long> idsExpected) {
        int posActual = calculator.calcPosition(positionAmongAdd, itemToAdd, queue, currentlyPlaying);
        queue.add(posActual, itemToAdd);
        assertEquals(message, idsExpected, getIdList(queue));
    }

    static final List<FeedItem> QUEUE_EMPTY = Collections.unmodifiableList(Arrays.asList());

    static final List<FeedItem> QUEUE_DEFAULT = 
            Collections.unmodifiableList(Arrays.asList(
                    createFeedItem(11), createFeedItem(12), createFeedItem(13), createFeedItem(14)));
    static final List<Long> QUEUE_DEFAULT_IDS =
            QUEUE_DEFAULT.stream().map(fi -> fi.getId()).collect(Collectors.toList());


    static FeedItem createFeedItem(long id) {
        FeedItem item = new FeedItem(id, "Item" + id, "ItemId" + id, "url",
                new Date(), FeedItem.PLAYED, FeedMother.anyFeed());
        FeedMedia media = new FeedMedia(item, "download_url", 1234567, "audio/mpeg");
        media.setId(item.getId());
        item.setMedia(media);
        return item;
    }

}
