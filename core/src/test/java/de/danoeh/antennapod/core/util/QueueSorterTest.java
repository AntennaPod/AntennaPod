package de.danoeh.antennapod.core.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Test class for QueueSorter.
 */
public class QueueSorterTest {

    @Test
    public void testPermutorForRule_null() {
        assertNull(QueueSorter.getPermutor(null));
    }

    @Test
    public void testPermutorForRule_EPISODE_TITLE_ASC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.EPISODE_TITLE_A_Z);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_EPISODE_TITLE_DESC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.EPISODE_TITLE_Z_A);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_DATE_ASC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.DATE_OLD_NEW);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_DATE_DESC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.DATE_NEW_OLD);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_DURATION_ASC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.DURATION_SHORT_LONG);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_DURATION_DESC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.DURATION_LONG_SHORT);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    @Test
    public void testPermutorForRule_FEED_TITLE_ASC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.FEED_TITLE_A_Z);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 1, 2, 3)); // after sorting
    }

    @Test
    public void testPermutorForRule_FEED_TITLE_DESC() {
        Permutor<FeedItem> permutor = QueueSorter.getPermutor(SortOrder.FEED_TITLE_Z_A);

        List<FeedItem> itemList = getTestList();
        assertTrue(checkIdOrder(itemList, 1, 3, 2)); // before sorting
        permutor.reorder(itemList);
        assertTrue(checkIdOrder(itemList, 3, 2, 1)); // after sorting
    }

    /**
     * Generates a list with test data.
     */
    private List<FeedItem> getTestList() {
        List<FeedItem> itemList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2019, 0, 1);  // January 1st
        Feed feed1 = new Feed(null, null, "Feed title 1");
        FeedItem feedItem1 = new FeedItem(1, "Title 1", null, null, calendar.getTime(), 0, feed1);
        FeedMedia feedMedia1 = new FeedMedia(0, feedItem1, 1000, 0, 0, null, null, null, true, null, 0, 0);
        feedItem1.setMedia(feedMedia1);
        itemList.add(feedItem1);

        calendar.set(2019, 2, 1);  // March 1st
        Feed feed2 = new Feed(null, null, "Feed title 3");
        FeedItem feedItem2 = new FeedItem(3, "Title 3", null, null, calendar.getTime(), 0, feed2);
        FeedMedia feedMedia2 = new FeedMedia(0, feedItem2, 3000, 0, 0, null, null, null, true, null, 0, 0);
        feedItem2.setMedia(feedMedia2);
        itemList.add(feedItem2);

        calendar.set(2019, 1, 1);  // February 1st
        Feed feed3 = new Feed(null, null, "Feed title 2");
        FeedItem feedItem3 = new FeedItem(2, "Title 2", null, null, calendar.getTime(), 0, feed3);
        FeedMedia feedMedia3 = new FeedMedia(0, feedItem3, 2000, 0, 0, null, null, null, true, null, 0, 0);
        feedItem3.setMedia(feedMedia3);
        itemList.add(feedItem3);

        return itemList;
    }

    /**
     * Checks if both lists have the same size and the same ID order.
     *
     * @param itemList Item list.
     * @param ids      List of IDs.
     * @return <code>true</code> if both lists have the same size and the same ID order.
     */
    private boolean checkIdOrder(List<FeedItem> itemList, long... ids) {
        if (itemList.size() != ids.length) {
            return false;
        }

        for (int i = 0; i < ids.length; i++) {
            if (itemList.get(i).getId() != ids[i]) {
                return false;
            }
        }
        return true;
    }
}
