package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Exception thrown when a queue switch operation fails to maintain database invariants.
 *
 * <p>The database has a critical constraint: exactly one queue must have isActive=true at
 * all times. This exception is thrown by {@link QueueRepository#switchActiveQueue(long)}
 * or {@link QueueRepository#getActiveQueue()} when this invariant is violated.
 *
 * <p>Possible causes:
 * - Transaction failure during queue switch
 * - Database corruption (multiple or zero active queues)
 * - Concurrent modification issues
 *
 * <p>Example usage:
 * <pre>
 * try {
 *     queueRepository.switchActiveQueue(newQueueId).blockingAwait();
 * } catch (QueueSwitchException e) {
 *     // Handle switch failure - may need to restore from backup
 * }
 * </pre>
 */
public class QueueSwitchException extends RuntimeException {

    private final Long oldQueueId;
    private final Long newQueueId;

    /**
     * Constructs a new exception for a queue switch failure.
     *
     * @param message     Error message describing the failure
     * @param oldQueueId  Previous active queue ID (null if unknown)
     * @param newQueueId  Target active queue ID (null if unknown)
     */
    public QueueSwitchException(@NonNull String message,
                                 @Nullable Long oldQueueId,
                                 @Nullable Long newQueueId) {
        super(message);
        this.oldQueueId = oldQueueId;
        this.newQueueId = newQueueId;
    }

    /**
     * Constructs a new exception with a cause.
     *
     * @param message     Error message describing the failure
     * @param oldQueueId  Previous active queue ID (null if unknown)
     * @param newQueueId  Target active queue ID (null if unknown)
     * @param cause       The underlying cause
     */
    public QueueSwitchException(@NonNull String message,
                                 @Nullable Long oldQueueId,
                                 @Nullable Long newQueueId,
                                 Throwable cause) {
        super(message, cause);
        this.oldQueueId = oldQueueId;
        this.newQueueId = newQueueId;
    }

    /**
     * Constructs a new exception indicating no active queue was found.
     *
     * @param message Error message
     */
    public QueueSwitchException(@NonNull String message) {
        this(message, null, null);
    }

    /**
     * Constructs a new exception with a cause, indicating no active queue.
     *
     * @param message Error message
     * @param cause   The underlying cause
     */
    public QueueSwitchException(@NonNull String message, Throwable cause) {
        this(message, null, null, cause);
    }

    /**
     * Gets the previous active queue ID involved in the failed switch.
     *
     * @return Old queue ID, or null if not applicable/unknown
     */
    @Nullable
    public Long getOldQueueId() {
        return oldQueueId;
    }

    /**
     * Gets the target queue ID for the failed switch.
     *
     * @return New queue ID, or null if not applicable/unknown
     */
    @Nullable
    public Long getNewQueueId() {
        return newQueueId;
    }
}
