package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Future;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.Queue;

/**
 * Repository interface for queue operations in AntennaPod.
 *
 * <p>This repository provides comprehensive queue management functionality including:
 * - Queue CRUD operations
 * - Queue switching and activation
 * - Episode management within queues
 * - Queue content operations
 *
 * <p>All write methods return Future objects for async operations, following the DBWriter pattern.
 * Read methods execute synchronously on the caller's thread (following DBReader pattern).
 * Methods that modify data may throw custom exceptions defined in this package.
 *
 * <p>Threading: Write operations are executed on a background ExecutorService thread.
 * Read operations execute synchronously, so callers should ensure they are not on the UI thread.
 *
 * @see Queue
 * @see de.danoeh.antennapod.model.feed.QueueMembership
 */
public interface QueueRepository {

    // ==================== Queue CRUD Operations ====================

    /**
     * Retrieves a queue by its unique identifier.
     *
     * <p>Executes synchronously on caller's thread (DBReader pattern).
     *
     * @param id Queue identifier
     * @return Queue if found, null if not found
     */
    @Nullable
    Queue getQueueById(long id);

    /**
     * Retrieves all queues ordered by active status first, then alphabetically by name.
     *
     * <p>Executes synchronously on caller's thread (DBReader pattern).
     *
     * @return List of all queues (never null, may be empty)
     */
    @NonNull
    List<Queue> getAllQueues();

    /**
     * Creates a new queue in the database.
     *
     * <p>The queue name must be unique. Cannot create a queue with isDefault=true
     * or isActive=true (use switchActiveQueue for activation).
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queue Queue to create (id will be generated, createdAt/modifiedAt set automatically)
     * @return Future that completes when creation is successful, returns generated queue ID
     */
    @NonNull
    Future<Long> createQueue(@NonNull Queue queue);

    /**
     * Updates an existing queue.
     *
     * <p>Can update name, color, and icon. Cannot modify isDefault or isActive flags
     * (use switchActiveQueue for activation). Cannot rename the default queue.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queue Queue with updated properties (must have valid ID)
     * @return Future that completes when update is successful
     */
    @NonNull
    Future<?> updateQueue(@NonNull Queue queue);

    /**
     * Deletes a queue and all its memberships.
     *
     * <p>Cannot delete the default queue (isDefault=true).
     * If deleting the active queue, automatically switches to default queue.
     * All QueueMembership records for this queue are cascade deleted.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queueId Queue identifier to delete
     * @return Future that completes when deletion is successful
     */
    @NonNull
    Future<?> deleteQueue(long queueId);

    // ==================== Queue Switching Operations ====================

    /**
     * Switches the active queue to the specified queue.
     *
     * <p>This operation updates exactly two queues in a transaction:
     * 1. Sets current active queue's isActive = false
     * 2. Sets target queue's isActive = true
     * 3. Updates UserPreferences with new active queue ID
     * 4. Posts QueueSwitchedEvent via EventBus
     *
     * <p>The database constraint ensures exactly one queue has isActive=true at all times.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queueId ID of queue to make active
     * @return Future that completes when switch is successful
     */
    @NonNull
    Future<?> switchActiveQueue(long queueId);

    /**
     * Gets the currently active queue.
     *
     * <p>There is always exactly one active queue (database constraint).
     * This query is fast as it's indexed on isActive column.
     *
     * <p>Executes synchronously on caller's thread (DBReader pattern).
     *
     * @return Currently active Queue, or null if none found (should never happen)
     */
    @Nullable
    Queue getActiveQueue();

    // ==================== Episode Management Operations ====================

    /**
     * Adds an episode to a queue at the next available position.
     *
     * <p>Position is auto-calculated as max(position) + 1 in the target queue.
     * If episode already exists in the queue, this operation is a no-op.
     * Posts QueueContentChangedEvent with ADDED action.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queueId   Queue to add episode to
     * @param episodeId Episode to add
     * @return Future that completes when addition is successful
     */
    @NonNull
    Future<?> addEpisodeToQueue(long queueId, long episodeId);

    /**
     * Removes an episode from a queue.
     *
     * <p>After removal, all subsequent episodes' positions are decremented to maintain
     * continuous position sequence (0, 1, 2, ...).
     * Posts QueueContentChangedEvent with REMOVED action.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queueId   Queue to remove episode from
     * @param episodeId Episode to remove
     * @return Future that completes when removal is successful
     */
    @NonNull
    Future<?> removeEpisodeFromQueue(long queueId, long episodeId);

    /**
     * Gets all episodes in a queue, ordered by position.
     *
     * <p>Returns full FeedItem objects with all properties loaded.
     * Positions are guaranteed to be continuous (0, 1, 2, ...).
     *
     * <p>Executes synchronously on caller's thread (DBReader pattern).
     *
     * @param queueId Queue identifier
     * @return List of episodes in queue order (never null, may be empty)
     */
    @NonNull
    List<FeedItem> getEpisodesForQueue(long queueId);

    /**
     * Moves an episode from one queue to another.
     *
     * <p>This operation is transactional:
     * 1. If episode exists in target queue, remove it first (avoid duplicates)
     * 2. Remove episode from source queue (reorder positions in source)
     * 3. Add episode to target queue at next available position
     * 4. Posts QueueContentChangedEvent for both queues
     *
     * <p>If fromQueueId == toQueueId, this is a no-op.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param fromQueueId Source queue ID
     * @param toQueueId   Target queue ID
     * @param episodeId   Episode to move
     * @return Future that completes when move is successful
     */
    @NonNull
    Future<?> moveEpisodeBetweenQueues(long fromQueueId, long toQueueId, long episodeId);

    // ==================== Queue Content Operations ====================

    /**
     * Removes all episodes from a queue.
     *
     * <p>Deletes all QueueMembership records for this queue.
     * Does not delete the queue itself.
     * Posts QueueContentChangedEvent with REMOVED action for each episode.
     *
     * <p>Executes asynchronously on background thread (DBWriter pattern).
     *
     * @param queueId Queue to clear
     * @return Future that completes when clear is successful
     */
    @NonNull
    Future<?> clearQueue(long queueId);

    /**
     * Gets the number of episodes in a queue.
     *
     * <p>Executes synchronously on caller's thread (DBReader pattern).
     *
     * @param queueId Queue identifier
     * @return Episode count (0 if queue doesn't exist or is empty)
     */
    int getQueueEpisodeCount(long queueId);

    /**
     * Gets all queues that contain a specific episode.
     *
     * <p>Returns empty list if episode is not in any queue.
     * Useful for UI showing which queues contain an episode.
     *
     * <p>Executes synchronously on caller's thread (DBReader pattern).
     *
     * @param episodeId Episode identifier
     * @return List of queues containing this episode (never null, may be empty)
     */
    @NonNull
    List<Queue> getQueuesContainingEpisode(long episodeId);
}
