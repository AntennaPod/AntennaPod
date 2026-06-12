package de.danoeh.antennapod.ui.swipeactions;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class MoveToTopSwipeActionTest {

    @Test
    public void testGetId() {
        assertEquals(SwipeAction.MOVE_TO_TOP, new MoveToTopSwipeAction().getId());
    }

    @Test
    public void testWillNotRemove() {
        MoveToTopSwipeAction action = new MoveToTopSwipeAction();
        assertFalse(action.willRemove(new FeedItemFilter(), new FeedItem()));
    }

    @Test
    public void testMoveToBottomGetId() {
        assertEquals(SwipeAction.MOVE_TO_BOTTOM, new MoveToBottomSwipeAction().getId());
    }

    @Test
    public void testMoveToBottomWillNotRemove() {
        MoveToBottomSwipeAction action = new MoveToBottomSwipeAction();
        assertFalse(action.willRemove(new FeedItemFilter(), new FeedItem()));
    }

    @Test
    public void testDistinctIds() {
        assertNotEquals(new MoveToTopSwipeAction().getId(), new MoveToBottomSwipeAction().getId());
    }
}
