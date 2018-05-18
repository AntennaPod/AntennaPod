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

            return Arrays.asList(new Object[][] {
                    {"case default, i.e., add to the end", QUEUE_DEFAULT.size(), optDefault , 0},
                    {"case default (2nd item)", QUEUE_DEFAULT.size(), optDefault , 1},
                    {"case option enqueue at front", 0, optEnqAtFront , 0},
                    {"case option enqueue at front (2nd item)", 1, optEnqAtFront , 1}
            });
        }

        private static final List<FeedItem> QUEUE_DEFAULT = Arrays.asList(tFI(11), tFI(12), tFI(13), tFI(14));

        @Parameter
        public String message;

        @Parameter(1)
        public int posExpected;

        @Parameter(2)
        public Options options;

        @Parameter(3)
        public int posAmongAdded; // the position of feed item to be inserted among the list to be inserted.


        @Test
        public void test() {
            ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);

            int posActual = calculator.calcPosition(posAmongAdded, tFI(101), QUEUE_DEFAULT);
            assertEquals(message, posExpected , posActual);
        }

        private static FeedItem tFI(int id) {
            FeedItem item = new FeedItem(0, "Item" + id, "ItemId" + id, "url",
                    new Date(), FeedItem.PLAYED, FeedMother.anyFeed());
            return item;
        }

    }

}