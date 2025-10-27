package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

/**
 * Exception thrown when attempting to delete or rename the default queue.
 *
 * <p>The default queue (isDefault=true) is protected and cannot be:
 * - Deleted via {@link QueueRepository#deleteQueue(long)}
 * - Renamed via {@link QueueRepository#updateQueue(de.danoeh.antennapod.model.feed.Queue)}
 *
 * <p>This protection ensures there is always at least one queue available for
 * episodes, maintaining application stability.
 *
 * <p>Example usage:
 * <pre>
 * try {
 *     queueRepository.deleteQueue(defaultQueueId).blockingAwait();
 * } catch (DefaultQueueException e) {
 *     // Handle attempt to delete default queue - show error to user
 * }
 * </pre>
 */
public class DefaultQueueException extends RuntimeException {

    private final long queueId;
    private final String operationAttempted;

    /**
     * Constructs a new exception for a default queue operation violation.
     *
     * @param queueId            ID of the default queue
     * @param operationAttempted Description of the prohibited operation (e.g., "delete", "rename")
     */
    public DefaultQueueException(long queueId, @NonNull String operationAttempted) {
        super("Cannot " + operationAttempted + " the default queue (ID: " + queueId + ")");
        this.queueId = queueId;
        this.operationAttempted = operationAttempted;
    }

    /**
     * Constructs a new exception with a custom message.
     *
     * @param message            Custom error message
     * @param queueId            ID of the default queue
     * @param operationAttempted Description of the prohibited operation
     */
    public DefaultQueueException(@NonNull String message, long queueId,
                                  @NonNull String operationAttempted) {
        super(message);
        this.queueId = queueId;
        this.operationAttempted = operationAttempted;
    }

    /**
     * Constructs a new exception with a cause.
     *
     * @param queueId            ID of the default queue
     * @param operationAttempted Description of the prohibited operation
     * @param cause              The underlying cause
     */
    public DefaultQueueException(long queueId, @NonNull String operationAttempted, Throwable cause) {
        super("Cannot " + operationAttempted + " the default queue (ID: " + queueId + ")", cause);
        this.queueId = queueId;
        this.operationAttempted = operationAttempted;
    }

    /**
     * Gets the ID of the default queue involved in the violation.
     *
     * @return Queue ID
     */
    public long getQueueId() {
        return queueId;
    }

    /**
     * Gets the operation that was attempted on the default queue.
     *
     * @return Operation description (e.g., "delete", "rename")
     */
    @NonNull
    public String getOperationAttempted() {
        return operationAttempted;
    }
}
