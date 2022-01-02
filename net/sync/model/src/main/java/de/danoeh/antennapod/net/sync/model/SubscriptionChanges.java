package de.danoeh.antennapod.net.sync.model;

import androidx.annotation.NonNull;

import java.util.List;

public class SubscriptionChanges {
    private final List<String> added;
    private final List<String> removed;
    private final long timestamp;

    public SubscriptionChanges(@NonNull List<String> added,
                               @NonNull List<String> removed,
                               long timestamp) {
        this.added = added;
        this.removed = removed;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SubscriptionChange [added=" + added.toString()
                + ", removed=" + removed.toString() + ", timestamp="
                + timestamp + "]";
    }

    public List<String> getAdded() {
        return added;
    }

    public List<String> getRemoved() {
        return removed;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
