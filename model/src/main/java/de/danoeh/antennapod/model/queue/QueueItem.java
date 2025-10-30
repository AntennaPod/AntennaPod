package de.danoeh.antennapod.model.queue;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Item within a queue.
 *
 * @author Dominik Fill
 */
public class QueueItem implements Serializable {

    private long id;
    private long queueId;
    private long feedItemId;
    private long feedId;
    private int position;

    /**
     * This constructor is used by DBReader.
     * */
    public QueueItem(long id, long queueID, long feedItemId, long feedId, int position) {
        this.id = id;
        this.queueId = queueID;
        this.feedItemId = feedItemId;
        this.feedId = feedId;
        this.position = position;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public long getFeedItemId() {
        return feedItemId;
    }

    public void setFeedItemId(long feedItemId) {
        this.feedItemId = feedItemId;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @NonNull
    @Override
    public String toString() {
        return "QueueItem [id=" + id + ", queueId=" + queueId + ", feedItemId =" + feedItemId + ", feedId=" + feedId
                + ", position =" + position + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QueueItem queueItem = (QueueItem) o;
        return id == queueItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
