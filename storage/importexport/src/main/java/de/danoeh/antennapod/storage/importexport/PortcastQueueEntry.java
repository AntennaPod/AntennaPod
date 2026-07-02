package de.danoeh.antennapod.storage.importexport;

/**
 * Represents a single queue entry in a PortCast file, in queue order.
 */
public class PortcastQueueEntry {
    private String guid;
    private String enclosureUrl;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getEnclosureUrl() {
        return enclosureUrl;
    }

    public void setEnclosureUrl(String enclosureUrl) {
        this.enclosureUrl = enclosureUrl;
    }
}
