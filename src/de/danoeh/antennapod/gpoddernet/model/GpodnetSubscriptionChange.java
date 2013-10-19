package de.danoeh.antennapod.gpoddernet.model;

import java.util.List;

public class GpodnetSubscriptionChange {
    private List<String> added;
    private List<String> removed;
    private long timestamp;

    public GpodnetSubscriptionChange(List<String> added, List<String> removed,
                                     long timestamp) {
        if (added == null || removed == null) {
            throw new IllegalArgumentException(
                    "added and remove must not be null");
        }
        this.added = added;
        this.removed = removed;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GpodnetSubscriptionChange [added=" + added.toString()
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
