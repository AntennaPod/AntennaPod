package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;

/**
 * Exception thrown when attempting to create or rename a queue with a name
 * that already exists in the database.
 *
 * <p>Queue names must be unique (database constraint). This exception is thrown
 * by {@link QueueRepository#createQueue} and {@link QueueRepository#updateQueue}
 * when a name conflict is detected.
 *
 * <p><strong>DESIGN NOTE:</strong> This exception represents a validation failure
 * that could ideally be checked before calling the repository (by querying all
 * queue names first). However, it's acceptable as a fallback for runtime detection
 * of constraint violations, especially if caused by concurrent operations.
 *
 * <p>Example usage (exception-based):
 * <pre>
 * try {
 *     queueRepository.createQueue(newQueue);
 * } catch (QueueNameExistsException e) {
 *     // Handle duplicate name - prompt user to choose different name
 * }
 * </pre>
 *
 * <p>Example usage (validation-based, recommended):
 * <pre>
 * List&lt;Queue&gt; allQueues = queueRepository.getAllQueues();
 * for (Queue q : allQueues) {
 *     if (q.getName().equals(queueName)) {
 *         // Show error before attempting create/update
 *         showDuplicateNameError();
 *         return;
 *     }
 * }
 * queueRepository.createQueue(newQueue); // Safe to call now
 * </pre>
 */
public class QueueNameExistsException extends RuntimeException {

    private final String queueName;

    /**
     * Constructs a new exception indicating a queue name conflict.
     *
     * @param queueName The name that already exists
     */
    public QueueNameExistsException(@NonNull String queueName) {
        super("A queue with the name '" + queueName + "' already exists");
        this.queueName = queueName;
    }

    /**
     * Constructs a new exception with a custom message.
     *
     * @param message   Custom error message
     * @param queueName The name that already exists
     */
    public QueueNameExistsException(@NonNull String message, @NonNull String queueName) {
        super(message);
        this.queueName = queueName;
    }

    /**
     * Constructs a new exception with a cause.
     *
     * @param queueName The name that already exists
     * @param cause     The underlying cause
     */
    public QueueNameExistsException(@NonNull String queueName, Throwable cause) {
        super("A queue with the name '" + queueName + "' already exists", cause);
        this.queueName = queueName;
    }

    /**
     * Gets the queue name that caused the conflict.
     *
     * @return The conflicting queue name
     */
    @NonNull
    public String getQueueName() {
        return queueName;
    }
}
