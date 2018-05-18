package de.danoeh.antennapod.core.storage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMother;
import de.danoeh.antennapod.core.storage.DBWriter.ItemEnqueuePositionCalculator;
import de.danoeh.antennapod.core.storage.DBWriter.ItemEnqueuePositionCalculator.Options;

import static org.junit.Assert.assertEquals;

public class DBWriterTest {

    public static class ItemEnqueuePositionCalculatorTest {

        @Test
        public void testEnqueueDefault() {

            ItemEnqueuePositionCalculator calculator =
                    new ItemEnqueuePositionCalculator(new Options());

            List<FeedItem> curQueue = tQueue();

            int posActual1 = calculator.calcPosition(0, tFI(101), curQueue);
            assertEquals("case default, i.e., add to the end", curQueue.size(), posActual1);

            int posActual2 = calculator.calcPosition(1, tFI(102), curQueue);
            assertEquals("case default (2nd item)", curQueue.size(), posActual2);

        }

        @Test
        public void testEnqueueAtFront() {

            ItemEnqueuePositionCalculator calculator =
                    new ItemEnqueuePositionCalculator(new Options()
                            .setEnqueueAtFront(true));

            List<FeedItem> curQueue = tQueue();

            int posActual1 = calculator.calcPosition(0, tFI(101), curQueue);
            assertEquals("case option enqueue at front", 0, posActual1);

            int posActual2 = calculator.calcPosition(1, tFI(102), curQueue);
            assertEquals("case option enqueue at front (2nd item)", 1, posActual2);

        }

        private static List<FeedItem> tQueue() {
            return Arrays.asList(tFI(11), tFI(12), tFI(13), tFI(14));
        }

        private static FeedItem tFI(int id) {
            FeedItem item = new FeedItem(0, "Item" + id, "ItemId" + id, "url",
                    new Date(), FeedItem.PLAYED, FeedMother.anyFeed());
            return item;
        }

    }

}