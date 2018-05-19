package de.danoeh.antennapod.core.storage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedMother;
import de.danoeh.antennapod.core.storage.DBWriter.ItemEnqueuePositionCalculator;
import de.danoeh.antennapod.core.storage.DBWriter.ItemEnqueuePositionCalculator.Options;

import static org.junit.Assert.assertEquals;

public class DBWriterTest {

    @RunWith(Parameterized.class)
    public static class ItemEnqueuePositionCalculatorTest {

        @Parameters(name = "{index}: case<{0}>, expected:{1}")
        public static Iterable<Object[]> data() {
            Options optDefault = new Options();
            Options optEnqAtFront = new Options().setEnqueueAtFront(true);
            Options optKeepInProgressAtFront =
                    new Options().setEnqueueAtFront(true).setKeepInProgressAtFront(true);
            // edge case: keep in progress without enabling enqueue at front is meaningless
            Options optKeepInProgressAtFrontWithNoEnqueueAtFront =
                    new Options().setKeepInProgressAtFront(true);


            return Arrays.asList(new Object[][] {
                    {"case default, i.e., add to the end",
                            QUEUE_DEFAULT.size(), optDefault , 0, QUEUE_DEFAULT},
                    {"case default (2nd item)",
                            QUEUE_DEFAULT.size(), optDefault , 1, QUEUE_DEFAULT},
                    {"case option enqueue at front",
                            0, optEnqAtFront , 0, QUEUE_DEFAULT},
                    {"case option enqueue at front (2nd item)",
                            1, optEnqAtFront , 1, QUEUE_DEFAULT},
                    {"case option keep in progress at front",
                            1, optKeepInProgressAtFront , 0, QUEUE_FRONT_IN_PROGRESS},
                    {"case option keep in progress at front (2nd item)",
                            2, optKeepInProgressAtFront , 1, QUEUE_FRONT_IN_PROGRESS},
                    {"case option keep in progress at front, front item not in progress",
                            0, optKeepInProgressAtFront , 0, QUEUE_DEFAULT},
                    {"case option keep in progress at front, front item no media at all",
                            0, optKeepInProgressAtFront , 0, QUEUE_FRONT_NO_MEDIA}, // No media should not cause any exception
                    {"case option keep in progress at front, but enqueue at front is disabled",
                            QUEUE_FRONT_IN_PROGRESS.size(), optKeepInProgressAtFrontWithNoEnqueueAtFront , 0, QUEUE_FRONT_IN_PROGRESS},
                    {"case empty queue, option default",
                            0, optDefault, 0, QUEUE_EMPTY},
                    {"case empty queue, option enqueue at front",
                            0, optEnqAtFront, 0, QUEUE_EMPTY},
                    {"case empty queue, option keep in progress at front",
                            0, optKeepInProgressAtFront, 0, QUEUE_EMPTY},

            });
        }

        private static final List<FeedItem> QUEUE_EMPTY = Arrays.asList();

        private static final List<FeedItem> QUEUE_DEFAULT = Arrays.asList(tFI(11), tFI(12), tFI(13), tFI(14));

        private static final List<FeedItem> QUEUE_FRONT_IN_PROGRESS = Arrays.asList(tFI(11, 60000), tFI(12), tFI(13));

        private static final List<FeedItem> QUEUE_FRONT_NO_MEDIA = Arrays.asList(tFINoMedia(11), tFI(12), tFI(13));

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


        @Test
        public void test() {
            ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);

            int posActual = calculator.calcPosition(posAmongAdded, tFI(101), curQueue);
            assertEquals(message, posExpected , posActual);
        }

        private static FeedItem tFI(int id) {
            return tFI(id, -1);
        }

        private static FeedItem tFI(int id, int position) {
            FeedItem item = tFINoMedia(id);
            FeedMedia media = new FeedMedia(item, "download_url", 1234567, "audio/mpeg");
            item.setMedia(media);

            if (position >= 0) {
                media.setPosition(position);
            }

            return item;
        }

        private static FeedItem tFINoMedia(int id) {
            FeedItem item = new FeedItem(0, "Item" + id, "ItemId" + id, "url",
                    new Date(), FeedItem.PLAYED, FeedMother.anyFeed());
            return item;
        }
    }

}
