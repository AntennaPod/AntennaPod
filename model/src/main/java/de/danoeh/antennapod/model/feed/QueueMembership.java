package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Data Object representing the many-to-many relationship between queues and episodes.
 *
 * <p>This junction entity tracks which episodes are in which queues, along with
 * their position within each queue and when they were added.
 *
 * <p>Database constraints:
 * - Composite primary key: (queueId, episodeId)
 * - Foreign key queueId references Queues.id (cascade delete)
 * - Foreign key episodeId references FeedItems.id (cascade delete)
 * - position: Non-negative integer, unique within a queue
 * - An episode can appear at most once per queue
 */
public class QueueMembership {

    private long queueId;
    private long episodeId;
    private int position;
    private long addedAt;

    /**
     * Default constructor.
     */
    public QueueMembership() {
        this.queueId = 0;
        this.episodeId = 0;
        this.position = 0;
        this.addedAt = System.currentTimeMillis();
    }

    /**
     * Constructor used by DBReader to restore a QueueMembership from the database.
     *
     * @param queueId   Queue containing this episode
     * @param episodeId Episode in the queue
     * @param position  0-based position in queue ordering
     * @param addedAt   Timestamp when episode was added (milliseconds)
     */
    public QueueMembership(long queueId, long episodeId, int position, long addedAt) {
        this.queueId = queueId;
        this.episodeId = episodeId;
        this.position = position;
        this.addedAt = addedAt;
    }

    /**
     * Constructor for creating a new QueueMembership.
     *
     * @param queueId   Queue containing this episode
     * @param episodeId Episode in the queue
     * @param position  0-based position in queue ordering
     */
    public QueueMembership(long queueId, long episodeId, int position) {
        this(queueId, episodeId, position, System.currentTimeMillis());
    }

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public long getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(long episodeId) {
        this.episodeId = episodeId;
    }

    public int getPosition() {
        return position;
    }

    /**
     * Sets the position within the queue.
     *
     * @param position 0-based position (must be non-negative)
     */
    public void setPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Position must be non-negative");
        }
        this.position = position;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "QueueMembership [queueId=" + queueId + ", episodeId=" + episodeId
                + ", position=" + position + ", addedAt=" + addedAt + "]";
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QueueMembership that = (QueueMembership) o;
        return queueId == that.queueId && episodeId == that.episodeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queueId, episodeId);
    }
}
