package de.danoeh.antennapod.storage.importexport;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed PortCast file.
 */
public class PortcastDocument {
    public static final String STATUS_UNPLAYED = PortcastSymbols.STATUS_UNPLAYED;
    public static final String STATUS_IN_PROGRESS = PortcastSymbols.STATUS_IN_PROGRESS;
    public static final String STATUS_COMPLETED = PortcastSymbols.STATUS_COMPLETED;

    private final List<PortcastSubscription> subscriptions = new ArrayList<>();
    private final List<PortcastQueueEntry> queue = new ArrayList<>();

    public List<PortcastSubscription> getSubscriptions() {
        return subscriptions;
    }

    public List<PortcastQueueEntry> getQueue() {
        return queue;
    }
}
