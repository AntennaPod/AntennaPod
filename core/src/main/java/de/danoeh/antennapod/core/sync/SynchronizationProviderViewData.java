package de.danoeh.antennapod.core.sync;

import de.danoeh.antennapod.core.R;

public enum SynchronizationProviderViewData {
    GPODDER_NET(
            "GPODDER_NET",
            R.string.gpodnet_description,
            R.drawable.gpodder_icon
    ),
    NEXTCLOUD_GPODDER(
            "NEXTCLOUD_GPODDER",
            R.string.synchronization_summary_nextcloud,
            R.drawable.nextcloud_logo
    ),
    ;

    private final String identifier;
    private final int iconResource;
    private final int summaryResource;

    SynchronizationProviderViewData(String identifier, int summaryResource, int iconResource) {
        this.identifier = identifier;
        this.iconResource = iconResource;
        this.summaryResource = summaryResource;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getIconResource() {
        return iconResource;
    }

    public int getSummaryResource() {
        return summaryResource;
    }
}
