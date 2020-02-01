package de.danoeh.antennapod.core.util;

import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * Provides methods for accessing the queue. It is possible to load only a part of the information about the queue that
 * is stored in the database (e.g. sometimes the user just has to test if a specific item is contained in the List.
 * QueueAccess provides an interface for accessing the queue without having to care about the type of the queue
 * representation.
 */
public abstract class QueueAccess {
    /**
     * Returns true if the item is in the queue, false otherwise.
     */
    public abstract boolean contains(long id);

    /**
     * Removes the item from the queue.
     *
     * @return true if the queue was modified by this operation.
     */
    public abstract boolean remove(long id);

    private QueueAccess() {
    }
}
