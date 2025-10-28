package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Data Object representing a named queue collection.
 *
 * <p>Each queue has a unique name (max 50 characters) and is managed by the QueueRepository.
 *
 * <p>This POJO models only the intrinsic queue properties (id, name, timestamps).
 *
 * <p>Global system state (which queue is default, which is active) is managed by the
 * QueueRepository, not stored in Queue objects. This prevents out-of-sync state
 * where multiple Queue objects might claim to be default or active.
 *
 * <p>Database constraints:
 * - name: UNIQUE, NOT NULL, max 50 characters
 */
public class Queue {

    private long id;
    @NonNull
    private String name;
    private long createdAt;
    private long modifiedAt;

    /**
     * Constructor for creating a new Queue instance.
     */
    public Queue() {
        this.id = 0;
        this.name = "";
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
    }

    /**
     * Constructor used by DBReader to restore a Queue from the database.
     *
     * @param id         Unique queue identifier
     * @param name       Queue display name (max 50 chars)
     * @param createdAt  Creation timestamp (milliseconds)
     * @param modifiedAt Last modification timestamp (milliseconds)
     */
    public Queue(long id, @NonNull String name, long createdAt, long modifiedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    /**
     * Constructor for creating a new Queue with a name.
     *
     * @param name Queue display name (max 50 chars)
     */
    public Queue(@NonNull String name) {
        this.id = 0;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Sets the queue name.
     *
     * @param name Queue display name (max 50 characters, unique constraint)
     */
    public void setName(@NonNull String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Queue name cannot exceed 50 characters");
        }
        this.name = name;
        this.modifiedAt = System.currentTimeMillis();
    }


    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "Queue [id=" + id + ", name=" + name + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt + "]";
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Queue queue = (Queue) o;
        return id == queue.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
