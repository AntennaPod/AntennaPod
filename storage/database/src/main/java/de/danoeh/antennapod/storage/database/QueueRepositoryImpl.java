package de.danoeh.antennapod.storage.database;

import android.database.SQLException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Future;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.event.QueueContentChangedEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.Queue;
import de.danoeh.antennapod.model.feed.QueueNotFoundException;
import de.danoeh.antennapod.model.feed.QueueRepository;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Implementation of QueueRepository interface for queue operations in AntennaPod.
 *
 * <p>This implementation follows the DBWriter/DBReader pattern:
 * - Write operations execute asynchronously on a background ExecutorService
 * - Read operations execute synchronously on the caller's thread
 * - All database operations use PodDBAdapter for data access
 *
 * <p>Thread Safety: This class is thread-safe. Write operations are serialized
 * through a single-threaded executor, and read operations use synchronized PodDBAdapter access.
 *
 * <p>Transaction Handling: Critical operations like switchActiveQueue and moveEpisodeBetweenQueues
 * use database transactions to ensure atomicity and data consistency.
 *
 * @see QueueRepository
 * @see PodDBAdapter
 */
public class QueueRepositoryImpl implements QueueRepository {

    private static final String TAG = "QueueRepositoryImpl";

    /**
     * Singleton instance of the repository.
     */
    private static QueueRepositoryImpl instance;

    /**
     * Gets the singleton instance of QueueRepositoryImpl.
     *
     * @return The singleton instance
     */
    public static synchronized QueueRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new QueueRepositoryImpl();
        }
        return instance;
    }

    private QueueRepositoryImpl() {
        // Private constructor for singleton pattern
    }

    // ==================== Queue CRUD Operations ====================

    @Nullable
    @Override
    public Queue getQueueById(long id) {
        Log.d(TAG, "getQueueById: id=" + id);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.selectQueueById(id);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    @Override
    public List<Queue> getAllQueues() {
        Log.d(TAG, "getAllQueues");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.selectAllQueues();
        } finally {
            adapter.close();
        }
    }

    @NonNull
    @Override
    public Future<Long> createQueue(@NonNull Queue queue) {
        Log.d(TAG, "createQueue: name=" + queue.getName());
        return DBWriter.submitDbTaskWithResult(() -> {
            // Validate queue name
            if (queue.getName() == null || queue.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Queue name cannot be null or empty");
            }
            if (queue.getName().length() > 50) {
                throw new IllegalArgumentException("Queue name cannot exceed 50 characters");
            }

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                // Check for duplicate name
                List<Queue> existingQueues = adapter.selectAllQueues();
                for (Queue existing : existingQueues) {
                    if (existing.getName().equals(queue.getName())) {
                        throw new IllegalArgumentException("A queue with the name '" + queue.getName() + "' already exists");
                    }
                }

                // Set timestamps
                long now = System.currentTimeMillis();
                queue.setCreatedAt(now);
                queue.setModifiedAt(now);

                // Insert queue
                long queueId = adapter.insertQueue(queue);
                Log.d(TAG, "Queue created with ID: " + queueId);
                return queueId;
            } catch (SQLException e) {
                Log.e(TAG, "Error creating queue", e);
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint")) {
                    throw new IllegalArgumentException("A queue with the name '" + queue.getName() + "' already exists", e);
                }
                throw e;
            } finally {
                adapter.close();
            }
        });
    }

    @NonNull
    @Override
    public Future<?> updateQueue(@NonNull Queue queue) {
        Log.d(TAG, "updateQueue: id=" + queue.getId() + ", name=" + queue.getName());
        return DBWriter.submitDbTask(() -> {
            if (queue.getId() == 0) {
                throw new IllegalArgumentException("Queue ID must be set for update");
            }

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                // Get existing queue
                Queue existing = adapter.selectQueueById(queue.getId());
                if (existing == null) {
                    throw new QueueNotFoundException(queue.getId());
                }

                // Check for duplicate name if name is changing
                if (!existing.getName().equals(queue.getName())) {
                    List<Queue> allQueues = adapter.selectAllQueues();
                    for (Queue q : allQueues) {
                        if (q.getId() != queue.getId() && q.getName().equals(queue.getName())) {
                            throw new IllegalArgumentException("A queue with the name '" + queue.getName() + "' already exists");
                        }
                    }
                }

                // Update modifiedAt timestamp
                queue.setModifiedAt(System.currentTimeMillis());

                // Update queue name and timestamps (default/active status is managed separately)
                adapter.updateQueue(queue);
                Log.d(TAG, "Queue updated: id=" + queue.getId());
            } catch (SQLException e) {
                Log.e(TAG, "Error updating queue", e);
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint")) {
                    throw new IllegalArgumentException("A queue with the name '" + queue.getName() + "' already exists", e);
                }
                throw e;
            } finally {
                adapter.close();
            }
        });
    }

    @NonNull
    @Override
    public Future<?> deleteQueue(long queueId) {
        Log.d(TAG, "deleteQueue: id=" + queueId);
        return DBWriter.submitDbTask(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                Queue queue = adapter.selectQueueById(queueId);
                if (queue == null) {
                    throw new QueueNotFoundException(queueId);
                }

                // If deleting the active queue, switch to default queue first
                if (adapter.getActiveQueueId() == queueId) {
                    Log.d(TAG, "Deleting active queue, switching to default queue first");
                    Queue defaultQueue = adapter.selectDefaultQueue();
                    if (defaultQueue != null) {
                        switchActiveQueueSynchronous(adapter, defaultQueue.getId());
                    }
                }

                // Delete queue (cascade deletes memberships via foreign key)
                adapter.deleteQueue(queueId);
                Log.d(TAG, "Queue deleted: id=" + queueId);
            } finally {
                adapter.close();
            }
        });
    }

    // ==================== Queue Switching Operations ====================

    @NonNull
    @Override
    public Future<?> switchActiveQueue(long queueId) {
        Log.d(TAG, "switchActiveQueue: queueId=" + queueId);
        return DBWriter.submitDbTask(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                switchActiveQueueSynchronous(adapter, queueId);
            } finally {
                adapter.close();
            }
        });
    }

    /**
     * Switches the active queue in a transaction (synchronous helper method).
     *
     * <p>This operation is atomic:
     * 1. Deactivates current active queue
     * 2. Activates target queue
     * 3. Updates UserPreferences
     *
     * @param adapter Open PodDBAdapter instance
     * @param queueId Target queue ID
     * @throws QueueNotFoundException if target queue doesn't exist
     */
    private void switchActiveQueueSynchronous(PodDBAdapter adapter, long queueId) {
        try {
            // Verify target queue exists
            Queue targetQueue = adapter.selectQueueById(queueId);
            if (targetQueue == null) {
                throw new QueueNotFoundException(queueId);
            }

            // If already active, nothing to do
            if (adapter.getActiveQueueId() == queueId) {
                Log.d(TAG, "Queue " + queueId + " is already active");
                return;
            }

            // Begin transaction
            adapter.beginTransaction();
            try {
                // Update active queue flag in database (atomically deactivates old, activates new)
                adapter.setActiveQueue(queueId);

                // Update UserPreferences
                UserPreferences.setActiveQueueId(queueId);

                adapter.setTransactionSuccessful();
                Log.d(TAG, "Queue switch successful: " + queueId);
            } finally {
                adapter.endTransaction();
            }

            // Auto-cleanup: Remove 100% played episodes from target queue (FR-042)
            cleanupPlayedEpisodes(adapter, queueId);

            // Post event after transaction completes
            // EventBus.getDefault().post(new QueueSwitchedEvent(queueId));
        } catch (Exception e) {
            Log.e(TAG, "Queue switch failed", e);
            throw new RuntimeException("Failed to switch active queue to " + queueId, e);
        }
    }

    /**
     * Removes all 100% played episodes from the specified queue (FR-042).
     *
     * <p>Called automatically when switching to a queue to clean up completed episodes.
     * Episodes are removed from queue but not deleted from the library.
     *
     * <p>TODO (T049): Implement full cleanup logic by checking FeedMedia playback duration
     * against position. Currently marked for Phase 4 implementation.
     *
     * @param adapter Open PodDBAdapter instance
     * @param queueId Queue to clean up
     */
    private void cleanupPlayedEpisodes(PodDBAdapter adapter, long queueId) {
        // TODO (T049): Implement cleanup when FeedItem/FeedMedia API is clarified
        // Future implementation should:
        // 1. Get episodes from selectEpisodesForQueue(queueId)
        // 2. Check if episode.getMedia().getDuration() <= episode.getMedia().getPosition()
        // 3. Remove episode via deleteQueueMembership(queueId, episodeId)
        Log.d(TAG, "Episode cleanup placeholder for queue " + queueId + " (T049)");
    }

    @Nullable
    @Override
    public Queue getActiveQueue() {
        Log.d(TAG, "getActiveQueue");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.selectActiveQueue();
        } finally {
            adapter.close();
        }
    }

    @Nullable
    @Override
    public Queue getDefaultQueue() {
        Log.d(TAG, "getDefaultQueue");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.selectDefaultQueue();
        } finally {
            adapter.close();
        }
    }

    // ==================== Episode Management Operations ====================

    @NonNull
    @Override
    public Future<?> addEpisodeToQueue(long queueId, long episodeId) {
        Log.d(TAG, "addEpisodeToQueue: queueId=" + queueId + ", episodeId=" + episodeId);
        return DBWriter.submitDbTask(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                // Verify queue exists
                Queue queue = adapter.selectQueueById(queueId);
                if (queue == null) {
                    throw new QueueNotFoundException(queueId);
                }

                // Check if episode already exists in queue
                if (adapter.queueMembershipExists(queueId, episodeId)) {
                    Log.d(TAG, "Episode " + episodeId + " already in queue " + queueId);
                    return;
                }

                // Calculate next position (max + 1)
                int nextPosition = adapter.getMaxPositionInQueue(queueId) + 1;

                // Insert membership
                adapter.insertQueueMembership(queueId, episodeId, nextPosition);
                Log.d(TAG, "Episode added to queue at position " + nextPosition);

                // Post event to notify UI of queue content change
                EventBus.getDefault().post(new QueueContentChangedEvent(queueId, episodeId,
                        QueueContentChangedEvent.ChangeType.ADDED));
            } finally {
                adapter.close();
            }
        });
    }

    @NonNull
    @Override
    public Future<?> removeEpisodeFromQueue(long queueId, long episodeId) {
        Log.d(TAG, "removeEpisodeFromQueue: queueId=" + queueId + ", episodeId=" + episodeId);
        return DBWriter.submitDbTask(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                adapter.beginTransaction();
                try {
                    // Delete membership
                    adapter.deleteQueueMembership(queueId, episodeId);

                    // Reorder remaining episodes to fill gaps
                    adapter.reorderQueueMembershipsAfterRemoval(queueId);

                    adapter.setTransactionSuccessful();
                    Log.d(TAG, "Episode removed from queue and positions reordered");
                } finally {
                    adapter.endTransaction();
                }

                // Post event to notify UI of queue content change
                EventBus.getDefault().post(new QueueContentChangedEvent(queueId, episodeId,
                        QueueContentChangedEvent.ChangeType.REMOVED));
            } finally {
                adapter.close();
            }
        });
    }

    @NonNull
    @Override
    public List<FeedItem> getEpisodesForQueue(long queueId) {
        Log.d(TAG, "getEpisodesForQueue: queueId=" + queueId);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.selectEpisodesForQueue(queueId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    @Override
    public Future<?> moveEpisodeBetweenQueues(long fromQueueId, long toQueueId, long episodeId) {
        Log.d(TAG, "moveEpisodeBetweenQueues: from=" + fromQueueId + ", to=" + toQueueId
                + ", episode=" + episodeId);
        return DBWriter.submitDbTask(() -> {
            // No-op if same queue
            if (fromQueueId == toQueueId) {
                Log.d(TAG, "Source and target queues are the same, no-op");
                return;
            }

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                adapter.beginTransaction();
                try {
                    // If episode exists in target queue, remove it first
                    if (adapter.queueMembershipExists(toQueueId, episodeId)) {
                        adapter.deleteQueueMembership(toQueueId, episodeId);
                        adapter.reorderQueueMembershipsAfterRemoval(toQueueId);
                    }

                    // Remove from source queue
                    adapter.deleteQueueMembership(fromQueueId, episodeId);
                    adapter.reorderQueueMembershipsAfterRemoval(fromQueueId);

                    // Add to target queue at next available position
                    int nextPosition = adapter.getMaxPositionInQueue(toQueueId) + 1;
                    adapter.insertQueueMembership(toQueueId, episodeId, nextPosition);

                    adapter.setTransactionSuccessful();
                    Log.d(TAG, "Episode moved between queues successfully");
                } finally {
                    adapter.endTransaction();
                }

                // Post events for both queues to notify UI of content changes
                EventBus.getDefault().post(new QueueContentChangedEvent(fromQueueId, episodeId,
                        QueueContentChangedEvent.ChangeType.REMOVED));
                EventBus.getDefault().post(new QueueContentChangedEvent(toQueueId, episodeId,
                        QueueContentChangedEvent.ChangeType.ADDED));
            } finally {
                adapter.close();
            }
        });
    }

    // ==================== Queue Content Operations ====================

    @NonNull
    @Override
    public Future<?> clearQueue(long queueId) {
        Log.d(TAG, "clearQueue: queueId=" + queueId);
        return DBWriter.submitDbTask(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                adapter.deleteAllQueueMemberships(queueId);
                Log.d(TAG, "Queue cleared: " + queueId);

                // Post event to notify UI that queue content was cleared
                // episodeId=0 indicates all episodes were removed, not a specific one
                EventBus.getDefault().post(new QueueContentChangedEvent(queueId, 0,
                        QueueContentChangedEvent.ChangeType.REMOVED));
            } finally {
                adapter.close();
            }
        });
    }

    @Override
    public int getQueueEpisodeCount(long queueId) {
        Log.d(TAG, "getQueueEpisodeCount: queueId=" + queueId);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.countQueueEpisodes(queueId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    @Override
    public List<Queue> getQueuesContainingEpisode(long episodeId) {
        Log.d(TAG, "getQueuesContainingEpisode: episodeId=" + episodeId);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.selectQueuesContainingEpisode(episodeId);
        } finally {
            adapter.close();
        }
    }

    /**
     * Reorders episodes in a queue based on a new episode ID list.
     *
     * <p>This method is not in the QueueRepository interface but is provided
     * for completeness. It bulk-updates positions for episodes in the queue
     * based on their order in the provided list.
     *
     * @param queueId    Queue to reorder
     * @param episodeIds Episode IDs in new order
     * @return Future that completes when reorder is successful
     */
    @NonNull
    public Future<?> reorderQueueEpisodes(long queueId, @NonNull List<Long> episodeIds) {
        Log.d(TAG, "reorderQueueEpisodes: queueId=" + queueId + ", count=" + episodeIds.size());
        return DBWriter.submitDbTask(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            try {
                adapter.beginTransaction();
                try {
                    // Update positions based on list order
                    for (int i = 0; i < episodeIds.size(); i++) {
                        adapter.updateQueueMembershipPosition(queueId, episodeIds.get(i), i);
                    }

                    adapter.setTransactionSuccessful();
                    Log.d(TAG, "Queue episodes reordered successfully");
                } finally {
                    adapter.endTransaction();
                }

                // Post event to notify UI that queue was reordered
                // episodeId=0 indicates entire queue was reordered, not a single episode change
                EventBus.getDefault().post(new QueueContentChangedEvent(queueId, 0,
                        QueueContentChangedEvent.ChangeType.REMOVED));
            } finally {
                adapter.close();
            }
        });
    }
}
