package de.danoeh.antennapod.model.queue;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Object for a whole queue.
 *
 * @author Dominik Fill
 */
public class Queue {


    private long id;
    private String name;
    private List<QueueItem> items;

    /**
     * This constructor is used for restoring a queue from the database.
     */
    public Queue(long id, String name) {
        this.id = id;
        this.name = name;
        this.items = new ArrayList<QueueItem>();
    }

    public Queue(String name) {
        this.name = name;
    }

    /**
     * Returns the item at the specified index.
     */
    public QueueItem getItemAtIndex(int position) {
        return items.get(position);
    }

    public long getId() {
        return id;
    }

    //--------------------
    // Getters and Setters
    //--------------------
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<QueueItem> getItems() {
        return items;
    }

    public void setItems(List<QueueItem> list) {
        this.items = list;
    }

    @NonNull
    @Override
    public String toString() {
        return "Queue [id=" + id + ", name=" + name + ", size=" + items.size() + "]";
    }

    @Override
    public boolean equals(Object o) {
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
