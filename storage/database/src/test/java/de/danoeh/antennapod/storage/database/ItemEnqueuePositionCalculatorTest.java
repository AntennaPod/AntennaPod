package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.FeedItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static de.danoeh.antennapod.model.feed.FeedPreferences.EnqueueLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ItemEnqueuePositionCalculatorTest {

    @Test
    public void testCalculatePositionBack() {
        ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(EnqueueLocation.BACK);
        List<FeedItem> queue = new ArrayList<>();
        queue.add(createFeedItem(1));
        queue.add(createFeedItem(2));

        int position = calculator.calcPosition(queue, null);
        assertEquals(2, position);
    }

    @Test
    public void testCalculatePositionFront() {
        ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(EnqueueLocation.FRONT);
        List<FeedItem> queue = new ArrayList<>();
        queue.add(createFeedItem(1));
        queue.add(createFeedItem(2));

        int position = calculator.calcPosition(queue, null);
        assertEquals(0, position);
    }

    @Test
    public void testCalculatePositionRandom() {
        ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(EnqueueLocation.RANDOM);
        List<FeedItem> queue = new ArrayList<>();
        queue.add(createFeedItem(1));
        queue.add(createFeedItem(2));

        int position = calculator.calcPosition(queue, null);
        assertTrue(position >= 0 && position <= 2);
    }

    @Test
    public void testCalculatePositionGlobal() {
        ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(EnqueueLocation.GLOBAL);
        List<FeedItem> queue = new ArrayList<>();
        queue.add(createFeedItem(1));
        queue.add(createFeedItem(2));

        int position = calculator.calcPosition(queue, null);
        assertEquals(2, position);
    }

    private FeedItem createFeedItem(long id) {
        FeedItem item = new FeedItem(id, "Item " + id, "item" + id, "url" + id, new java.util.Date(), 0, null);
        item.setId(id);
        return item;
    }
}
