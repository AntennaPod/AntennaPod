package de.danoeh.antennapod.core.gpoddernet.model;

import android.support.annotation.NonNull;

import java.util.List;

public class GpodnetSubscriptionChange {
    @NonNull
    private final List<String> added;
    @NonNull
    private final List<String> removed;
    private final long timestamp;

    public GpodnetSubscriptionChange(@NonNull List<String> added,
                                     @NonNull List<String> removed,
                                     long timestamp) {
        this.added = added;
        this.removed = removed;
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "GpodnetSubscriptionChange [added=" + added.toString()
                + ", removed=" + removed.toString() + ", timestamp="
                + timestamp + "]";
    }

    @NonNull
    public List<String> getAdded() {
        return added;
    }

    @NonNull
    public List<String> getRemoved() {
        return removed;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
