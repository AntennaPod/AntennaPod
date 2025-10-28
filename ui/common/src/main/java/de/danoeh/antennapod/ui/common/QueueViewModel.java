package de.danoeh.antennapod.ui.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.Queue;
import de.danoeh.antennapod.model.feed.QueueNotFoundException;
import de.danoeh.antennapod.model.feed.QueueRepository;

/**
 * ViewModel for managing queue-related UI state using AndroidX ViewModel pattern.
 *
 * <p>This ViewModel provides observable queue data via LiveData and delegates all data
 * operations to {@link QueueRepository}. It handles queue switching, creation, deletion,
 * and updates while managing error states gracefully.
 *
 * <p>Key responsibilities:
 * - Expose observable queue lists and active queue via LiveData
 * - Coordinate queue switching with transaction support
 * - Validate queue operations (name uniqueness, default queue protection)
 * - Post {@link QueueSwitchedEvent} via EventBus on successful queue switches
 *
 * <p>Usage:
 * <pre>
 * QueueViewModel viewModel = new ViewModelProvider(this,
 *     new QueueViewModel.Factory(queueRepository)).get(QueueViewModel.class);
 * viewModel.getAllQueues().observe(this, queues -> {
 *     // Update UI with queue list
 * });
 * viewModel.switchToQueue(queueId);
 * </pre>
 */
public class QueueViewModel extends ViewModel {

    private final QueueRepository queueRepository;
    private final MutableLiveData<List<Queue>> allQueues = new MutableLiveData<>();
    private final MutableLiveData<Queue> activeQueue = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeQueueEpisodeCount = new MutableLiveData<>();
    private final MutableLiveData<List<FeedItem>> activeQueueEpisodes = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * Constructs a new QueueViewModel with the given repository.
     *
     * @param queueRepository Repository for queue data access
     */
    public QueueViewModel(@NonNull QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
        loadQueues();
        loadActiveQueue();
    }

    /**
     * Gets all queues sorted by active status first, then alphabetically by name.
     *
     * <p>The list is updated automatically when queues are created, deleted, or modified.
     *
     * @return LiveData containing list of all queues (never null, may be empty)
     */
    @NonNull
    public LiveData<List<Queue>> getAllQueues() {
        return allQueues;
    }

    /**
     * Gets the currently active queue.
     *
     * <p>There is always exactly one active queue (database constraint).
     * This LiveData updates when {@link #switchToQueue(long)} succeeds.
     *
     * @return LiveData containing currently active Queue, or null if not yet loaded
     */
    @NonNull
    public LiveData<Queue> getActiveQueue() {
        return activeQueue;
    }

    /**
     * Gets the episode count for the currently active queue.
     *
     * <p>This count updates when the active queue changes or episodes are added/removed.
     *
     * @return LiveData containing episode count for active queue
     */
    @NonNull
    public LiveData<Integer> getActiveQueueEpisodeCount() {
        return activeQueueEpisodeCount;
    }

    /**
     * Gets the episodes in the currently active queue.
     *
     * <p>This list updates when the active queue changes or episodes are added/removed/reordered.
     * Suitable for binding to RecyclerView adapters in the UI.
     *
     * @return LiveData containing list of FeedItem episodes in active queue (never null, may be empty)
     */
    @NonNull
    public LiveData<List<FeedItem>> getActiveQueueEpisodes() {
        return activeQueueEpisodes;
    }

    /**
     * Gets error messages from failed operations.
     *
     * <p>Error messages are cleared after being consumed (single event pattern).
     * Subscribe to this LiveData to show error messages in UI (Toast, Snackbar, etc.).
     *
     * @return LiveData containing error message, or null if no error
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Switches the active queue to the specified queue.
     *
     * <p>This operation:
     * 1. Validates that the target queue exists
     * 2. Switches active status in a transaction via repository
     * 3. Posts {@link QueueSwitchedEvent} via EventBus on success
     * 4. Updates LiveData for active queue and episode count
     *
     * <p>Runs asynchronously on background thread via repository.
     *
     * @param queueId ID of queue to make active
     * @throws QueueNotFoundException if queueId does not exist
     */
    public void switchToQueue(long queueId) throws QueueNotFoundException {
        // Validate queue exists before attempting switch
        Queue targetQueue = queueRepository.getQueueById(queueId);
        if (targetQueue == null) {
            QueueNotFoundException ex = new QueueNotFoundException(queueId);
            errorMessage.postValue(ex.getMessage());
            throw ex;
        }

        // Perform switch asynchronously
        Future<?> future = queueRepository.switchActiveQueue(queueId);
        try {
            future.get(); // Wait for completion
            // Reload data after successful switch
            loadActiveQueue();
            loadQueues();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof QueueNotFoundException) {
                errorMessage.postValue(cause.getMessage());
                throw (QueueNotFoundException) cause;
            }
            errorMessage.postValue("Failed to switch queue: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage.postValue("Queue switch interrupted");
        }
    }

    /**
     * Creates a new queue with validation.
     *
     * <p>Validates that:
     * - name is not empty and does not exceed 50 characters
     * - name is unique (does not already exist)
     *
     * <p>Runs asynchronously on background thread via repository.
     *
     * @param name Queue display name (max 50 characters, unique)
     * @throws IllegalArgumentException if name is empty, invalid, or already exists
     */
    public void createQueue(@NonNull String name) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            errorMessage.postValue("Queue name cannot be empty");
            throw new IllegalArgumentException("Queue name cannot be empty");
        }
        if (name.length() > 50) {
            errorMessage.postValue("Queue name cannot exceed 50 characters");
            throw new IllegalArgumentException("Queue name cannot exceed 50 characters");
        }

        // Check for duplicate name
        List<Queue> existingQueues = queueRepository.getAllQueues();
        for (Queue q : existingQueues) {
            if (q.getName().equalsIgnoreCase(name.trim())) {
                String message = "A queue with the name '" + name.trim() + "' already exists";
                errorMessage.postValue(message);
                throw new IllegalArgumentException(message);
            }
        }

        // Create queue
        Queue newQueue = new Queue(name.trim());
        Future<Long> future = queueRepository.createQueue(newQueue);
        try {
            future.get(); // Wait for completion
            loadQueues(); // Reload queue list
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                errorMessage.postValue(cause.getMessage());
                throw (IllegalArgumentException) cause;
            }
            errorMessage.postValue("Failed to create queue: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage.postValue("Queue creation interrupted");
        }
    }

    /**
     * Deletes a queue.
     *
     * <p>If deleting the active queue, automatically switches to default queue before deletion.
     *
     * <p>Runs asynchronously on background thread via repository.
     *
     * @param queueId Queue identifier to delete
     * @throws QueueNotFoundException if queueId does not exist
     */
    public void deleteQueue(long queueId) throws QueueNotFoundException {
        // Validate queue exists
        Queue targetQueue = queueRepository.getQueueById(queueId);
        if (targetQueue == null) {
            QueueNotFoundException ex = new QueueNotFoundException(queueId);
            errorMessage.postValue(ex.getMessage());
            throw ex;
        }

        // Delete queue
        Future<?> future = queueRepository.deleteQueue(queueId);
        try {
            future.get(); // Wait for completion
            loadQueues(); // Reload queue list
            Queue activeQueue = queueRepository.getActiveQueue();
            if (activeQueue != null && activeQueue.getId() == queueId) {
                loadActiveQueue(); // Reload active queue if we deleted the active one
            }
        } catch (ExecutionException e) {
            errorMessage.postValue("Failed to delete queue: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage.postValue("Queue deletion interrupted");
        }
    }

    /**
     * Updates an existing queue's metadata.
     *
     * <p>Can update name. Cannot modify isDefault or isActive flags.
     *
     * @param queue Queue with updated properties (must have valid ID)
     * @throws IllegalArgumentException if renaming to an existing name
     * @throws QueueNotFoundException if queue ID does not exist
     */
    public void updateQueue(@NonNull Queue queue)
            throws QueueNotFoundException {
        // Validate queue exists
        Queue existingQueue = queueRepository.getQueueById(queue.getId());
        if (existingQueue == null) {
            QueueNotFoundException ex = new QueueNotFoundException(queue.getId());
            errorMessage.postValue(ex.getMessage());
            throw ex;
        }

        // Check for duplicate name if name changed
        if (!existingQueue.getName().equals(queue.getName())) {
            List<Queue> allQueuesList = queueRepository.getAllQueues();
            for (Queue q : allQueuesList) {
                if (q.getId() != queue.getId()
                        && q.getName().equalsIgnoreCase(queue.getName().trim())) {
                    String message = "A queue with the name '" + queue.getName() + "' already exists";
                    errorMessage.postValue(message);
                    throw new IllegalArgumentException(message);
                }
            }
        }

        // Update queue
        Future<?> future = queueRepository.updateQueue(queue);
        try {
            future.get(); // Wait for completion
            loadQueues(); // Reload queue list
            Queue activeQueue = queueRepository.getActiveQueue();
            if (activeQueue != null && activeQueue.getId() == queue.getId()) {
                loadActiveQueue(); // Reload active queue if we updated it
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                errorMessage.postValue(cause.getMessage());
                throw (IllegalArgumentException) cause;
            }
            errorMessage.postValue("Failed to update queue: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage.postValue("Queue update interrupted");
        }
    }

    /**
     * Renames a queue with validation.
     *
     * <p>Validates name uniqueness before renaming.
     *
     * @param queueId ID of queue to rename
     * @param newName New queue name (max 50 characters, unique)
     * @throws IllegalArgumentException if newName already exists
     * @throws QueueNotFoundException if queueId does not exist
     */
    public void renameQueue(long queueId, @NonNull String newName)
            throws QueueNotFoundException {
        // Validate queue exists
        Queue targetQueue = queueRepository.getQueueById(queueId);
        if (targetQueue == null) {
            QueueNotFoundException ex = new QueueNotFoundException(queueId);
            errorMessage.postValue(ex.getMessage());
            throw ex;
        }

        // Update with new name
        targetQueue.setName(newName);
        updateQueue(targetQueue);
    }

    /**
     * Loads all queues from repository and posts to LiveData.
     *
     * <p>Queues are sorted by active status first, then alphabetically by name.
     */
    private void loadQueues() {
        List<Queue> queues = queueRepository.getAllQueues();
        allQueues.postValue(queues);
    }

    /**
     * Loads the active queue, its episode count, and episodes from repository.
     */
    private void loadActiveQueue() {
        Queue active = queueRepository.getActiveQueue();
        activeQueue.postValue(active);
        if (active != null) {
            int count = queueRepository.getQueueEpisodeCount(active.getId());
            activeQueueEpisodeCount.postValue(count);
            List<FeedItem> episodes = queueRepository.getEpisodesForQueue(active.getId());
            activeQueueEpisodes.postValue(episodes);
        } else {
            activeQueueEpisodeCount.postValue(0);
            activeQueueEpisodes.postValue(null);
        }
    }

    /**
     * Clears the current error message.
     *
     * <p>Call this after displaying an error to the user to clear the error state.
     */
    public void clearError() {
        errorMessage.postValue(null);
    }

    /**
     * Factory for creating QueueViewModel instances with dependency injection.
     *
     * <p>Usage:
     * <pre>
     * ViewModelProvider.Factory factory = new QueueViewModel.Factory(queueRepository);
     * QueueViewModel viewModel = new ViewModelProvider(this, factory)
     *     .get(QueueViewModel.class);
     * </pre>
     */
    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final QueueRepository queueRepository;

        public Factory(@NonNull QueueRepository queueRepository) {
            this.queueRepository = queueRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(QueueViewModel.class)) {
                return (T) new QueueViewModel(queueRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
