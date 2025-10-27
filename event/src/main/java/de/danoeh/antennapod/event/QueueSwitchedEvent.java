package de.danoeh.antennapod.event;

import androidx.annotation.NonNull;

/**
 * EventBus event indicating that the active queue has been switched.
 *
 * <p>This event is posted via EventBus whenever
 * {@link de.danoeh.antennapod.model.feed.QueueRepository#switchActiveQueue(long)}
 * successfully completes. UI components can subscribe to this event to update displays,
 * refresh episode lists, or perform other queue-switch-related actions.
 *
 * <p>Example subscription:
 * <pre>
 * {@literal @}Subscribe(threadMode = ThreadMode.MAIN)
 * public void onQueueSwitched(QueueSwitchedEvent event) {
 *     // Update UI to show episodes from new active queue
 *     loadEpisodesForQueue(event.getNewQueueId());
 * }
 * </pre>
 */
public class QueueSwitchedEvent {

    private final long oldQueueId;
    private final long newQueueId;
    private final long timestamp;

    /**
     * Constructs a new QueueSwitchedEvent.
     *
     * @param oldQueueId ID of the previously active queue
     * @param newQueueId ID of the newly active queue
     */
    public QueueSwitchedEvent(long oldQueueId, long newQueueId) {
        this.oldQueueId = oldQueueId;
        this.newQueueId = newQueueId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Gets the ID of the previously active queue.
     *
     * @return Old queue ID
     */
    public long getOldQueueId() {
        return oldQueueId;
    }

    /**
     * Gets the ID of the newly active queue.
     *
     * @return New queue ID
     */
    public long getNewQueueId() {
        return newQueueId;
    }

    /**
     * Gets the timestamp when the switch occurred.
     *
     * @return Timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "QueueSwitchedEvent [oldQueueId=" + oldQueueId
                + ", newQueueId=" + newQueueId
                + ", timestamp=" + timestamp + "]";
    }
}
