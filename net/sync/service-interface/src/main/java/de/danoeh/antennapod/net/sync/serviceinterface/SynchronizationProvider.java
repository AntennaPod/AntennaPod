package de.danoeh.antennapod.net.sync.serviceinterface;

public enum SynchronizationProvider {
    GPODDER_NET("GPODDER_NET"),
    NEXTCLOUD_GPODDER("NEXTCLOUD_GPODDER");

    public static SynchronizationProvider fromIdentifier(String provider) {
        for (SynchronizationProvider synchronizationProvider : SynchronizationProvider.values()) {
            if (synchronizationProvider.getIdentifier().equals(provider)) {
                return synchronizationProvider;
            }
        }
        return null;
    }

    private final String identifier;

    SynchronizationProvider(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
