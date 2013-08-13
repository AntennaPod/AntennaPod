package de.danoeh.antennapod.util;

import de.danoeh.antennapod.feed.FeedItem;

import java.util.Iterator;
import java.util.List;

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

    public static QueueAccess IDListAccess(final List<Long> ids) {
        return new QueueAccess() {
            @Override
            public boolean contains(long id) {
                return (ids != null) && ids.contains(id);
            }

            @Override
            public boolean remove(long id) {
                return ids.remove(id);
            }


        };
    }

    public static QueueAccess ItemListAccess(final List<FeedItem> items) {
        return new QueueAccess() {
            @Override
            public boolean contains(long id) {
                if (items == null) {
                    return false;
                }
                Iterator<FeedItem> it = items.iterator();
                for (FeedItem i = it.next(); it.hasNext(); i = it.next()) {
                    if (i.getId() == id) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean remove(long id) {
                Iterator<FeedItem> it = items.iterator();
                for (FeedItem i = it.next(); it.hasNext(); i = it.next()) {
                    if (i.getId() == id) {
                        it.remove();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static QueueAccess NotInQueueAccess() {
        return new QueueAccess() {
            @Override
            public boolean contains(long id) {
                return false;
            }

            @Override
            public boolean remove(long id) {
                return false;
            }
        };

    }

}
