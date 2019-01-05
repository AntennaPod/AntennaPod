package de.danoeh.antennapod.core.gpoddernet.model;

import android.support.annotation.NonNull;

import java.util.List;

public class GpodnetSubscriptionChange {
    private final List<String> added;
    private final List<String> removed;
    private final long timestamp;

    public GpodnetSubscriptionChange(@NonNull List<String> added,
                                     @NonNull List<String> removed,
                                     long timestamp) {
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
