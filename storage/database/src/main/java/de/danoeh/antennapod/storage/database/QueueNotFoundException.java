package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

/**
 * Exception thrown when attempting to access a queue that does not exist in the database.
 *
 * <p>This exception is thrown by various {@link QueueRepository} methods when
 * a queue lookup by ID fails to find a matching record.
 *
 * <p>Example usage:
 * <pre>
 * try {
 *     Queue queue = queueRepository.getQueueById(nonExistentId).blockingGet();
 * } catch (QueueNotFoundException e) {
 *     // Handle missing queue - may have been deleted, show error
 * }
 * </pre>
 */
public class QueueNotFoundException extends RuntimeException {

    private final long queueId;

    /**
     * Constructs a new exception indicating a queue was not found.
     *
     * @param queueId The queue ID that was not found
     */
    public QueueNotFoundException(long queueId) {
        super("Queue with ID " + queueId + " not found");
        this.queueId = queueId;
    }

    /**
     * Constructs a new exception with a custom message.
     *
     * @param message Custom error message
     * @param queueId The queue ID that was not found
     */
    public QueueNotFoundException(@NonNull String message, long queueId) {
        super(message);
        this.queueId = queueId;
    }

    /**
     * Constructs a new exception with a cause.
     *
     * @param queueId The queue ID that was not found
     * @param cause   The underlying cause
     */
    public QueueNotFoundException(long queueId, Throwable cause) {
        super("Queue with ID " + queueId + " not found", cause);
        this.queueId = queueId;
    }

    /**
     * Gets the queue ID that was not found.
     *
     * @return Queue ID
     */
    public long getQueueId() {
        return queueId;
    }
}
