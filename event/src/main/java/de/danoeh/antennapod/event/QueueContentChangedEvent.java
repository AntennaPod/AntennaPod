package de.danoeh.antennapod.event;

import androidx.annotation.NonNull;

/**
 * EventBus event indicating that the content of a queue has changed.
 *
 * <p>This event is posted whenever episodes are added to, removed from, or reordered
 * within a queue. UI components can subscribe to this event to refresh episode lists
 * and update queue displays.
 *
 * <p>Event is posted by:
 * - {@link de.danoeh.antennapod.model.feed.QueueRepository#addEpisodeToQueue(long, long)}
 * - {@link de.danoeh.antennapod.model.feed.QueueRepository#removeEpisodeFromQueue(long, long)}
 * - {@link de.danoeh.antennapod.model.feed.QueueRepository#clearQueue(long)}
 * - {@link de.danoeh.antennapod.model.feed.QueueRepository#moveEpisodeBetweenQueues(long, long, long)}
 *
 * <p>Example subscription:
 * <pre>
 * {@literal @}Subscribe(threadMode = ThreadMode.MAIN)
 * public void onQueueContentChanged(QueueContentChangedEvent event) {
 *     if (event.getQueueId() == currentDisplayedQueueId) {
 *         // Refresh episode list for this queue
 *         refreshEpisodeList();
 *     }
 * }
 * </pre>
 */
public class QueueContentChangedEvent {

    /**
     * Type of content change that occurred.
     */
    public enum ChangeType {
        ADDED,      // Episode added to queue
        REMOVED,    // Episode removed from queue
        REORDERED   // Episodes reordered within queue
    }

    private final long queueId;
    private final long episodeId;
    private final ChangeType changeType;
    private final long timestamp;

    /**
     * Constructs a new QueueContentChangedEvent.
     *
     * @param queueId    ID of the queue that changed
     * @param episodeId  ID of the episode affected (0 if multiple/unknown)
     * @param changeType Type of change (ADDED, REMOVED, or REORDERED)
     */
    public QueueContentChangedEvent(long queueId, long episodeId, @NonNull ChangeType changeType) {
        this.queueId = queueId;
        this.episodeId = episodeId;
        this.changeType = changeType;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the ID of the queue that changed.
     *
     * @return Queue ID
     */
    public long getQueueId() {
        return queueId;
    }

    /**
     * Gets the ID of the episode affected by the change.
     *
     * @return Episode ID, or 0 if multiple episodes affected or unknown
     */
    public long getEpisodeId() {
        return episodeId;
    }

    /**
     * Gets the type of content change.
     *
     * @return Change type (ADDED, REMOVED, or REORDERED)
     */
    @NonNull
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Gets the timestamp when the change occurred.
     *
     * @return Timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "QueueContentChangedEvent [queueId=" + queueId
                + ", episodeId=" + episodeId
                + ", changeType=" + changeType
                + ", timestamp=" + timestamp + "]";
    }
}
