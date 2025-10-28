package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;

/**
 * Exception thrown when attempting to delete or rename the default queue.
 *
 * <p><strong>DEPRECATED DESIGN</strong> - This exception should be eliminated in favor of
 * UI-level validation. The delete and rename buttons should not be shown for the default
 * queue, preventing users from attempting these operations in the first place.
 *
 * <p>The default queue (isDefault=true) is protected and cannot be:
 * - Deleted via {@link QueueRepository#deleteQueue(long)}
 * - Renamed via {@link QueueRepository#updateQueue(Queue)}
 *
 * <p>This protection ensures there is always at least one queue available for
 * episodes, maintaining application stability.
 *
 * <p><strong>Recommended approach:</strong> UI components should check
 * {@link Queue#isDefault()} before showing edit/delete options. This prevents the
 * exception from ever being thrown.
 *
 * <p>Example usage (current):
 * <pre>
 * try {
 *     queueRepository.deleteQueue(defaultQueueId);
 * } catch (DefaultQueueException e) {
 *     // Handle attempt to delete default queue - show error to user
 * }
 * </pre>
 *
 * <p>Example usage (recommended):
 * <pre>
 * Queue queue = queueRepository.getQueueById(queueId);
 * if (!queue.isDefault()) {
 *     // Show delete button only for non-default queues
 *     showDeleteButton();
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
