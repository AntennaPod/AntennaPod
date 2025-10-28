package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Data Object representing a named queue collection.
 *
 * <p>Each queue has a unique name (max 50 characters), color, icon, and flags
 * indicating if it's the default queue or the currently active queue.
 *
 * <p>Database constraints:
 * - name: UNIQUE, NOT NULL, max 50 characters
 * - Exactly one queue must have isDefault = true
 * - Exactly one queue must have isActive = true
 */
public class Queue {

    private long id;
    @NonNull
    private String name;
    private int color;
    @NonNull
    private String icon;
    private boolean isDefault;
    private boolean isActive;
    private long createdAt;
    private long modifiedAt;

    /**
     * Constructor for creating a new Queue instance.
     */
    public Queue() {
        this.id = 0;
        this.name = "";
        this.icon = "";
        this.isDefault = false;
        this.isActive = false;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
    }

    /**
     * Constructor used by DBReader to restore a Queue from the database.
     *
     * @param id         Unique queue identifier
     * @param name       Queue display name (max 50 chars)
     * @param color      ARGB color from predefined palette
     * @param icon       Material Design icon resource name
     * @param isDefault  True if this is the protected default queue
     * @param isActive   True if this is the currently active queue
     * @param createdAt  Creation timestamp (milliseconds)
     * @param modifiedAt Last modification timestamp (milliseconds)
     */
    public Queue(long id, @NonNull String name, int color, @NonNull String icon,
                 boolean isDefault, boolean isActive, long createdAt, long modifiedAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.isDefault = isDefault;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    /**
     * Constructor for creating a new Queue with specific properties.
     *
     * @param name      Queue display name (max 50 chars)
     * @param color     ARGB color from predefined palette
     * @param icon      Material Design icon resource name
     * @param isDefault True if this is the protected default queue
     * @param isActive  True if this is the currently active queue
     */
    public Queue(@NonNull String name, int color, @NonNull String icon,
                 boolean isDefault, boolean isActive) {
        this.id = 0;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.isDefault = isDefault;
        this.isActive = isActive;
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        this.modifiedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getIcon() {
        return icon;
    }

    public void setIcon(@NonNull String icon) {
        if (icon == null || icon.isEmpty()) {
            throw new IllegalArgumentException("Queue icon cannot be null or empty");
        }
        this.icon = icon;
        this.modifiedAt = System.currentTimeMillis();
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets the default status of this queue.
     * <p><strong>INTERNAL USE ONLY</strong> - This method should only be called by QueueRepositoryImpl
     * to maintain the invariant that exactly one queue has isDefault = true.
     * External code should not call this method directly.
     *
     * @param isDefault True if this is the protected default queue
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
        this.modifiedAt = System.currentTimeMillis();
    }

    /**
     * Sets the active status of this queue.
     * <p><strong>INTERNAL USE ONLY</strong> - This method should only be called by QueueRepositoryImpl
     * to maintain the invariant that exactly one queue has isActive = true.
     * External code should not call this method directly.
     *
     * @param isActive True if this is the currently active queue
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
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
        return "Queue [id=" + id + ", name=" + name + ", color=" + color
                + ", icon=" + icon + ", isDefault=" + isDefault + ", isActive=" + isActive
                + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt + "]";
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
