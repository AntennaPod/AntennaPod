package de.danoeh.antennapod.core.sync;

import de.danoeh.antennapod.core.R;

public enum SynchronizationProviderViewData {
    GPODDER_NET(
            SyncService.SYNC_PROVIDER_CHOICE_GPODDER_NET,
            R.string.gpodnet_description,
            R.drawable.gpodder_icon
    ),
    NEXTCLOUD_GPODDER(
            SyncService.SYNC_PROVIDER_CHOICE_NEXTCLOUD,
            R.string.preference_synchronization_summary_nextcloud,
            R.drawable.nextcloud_logo
    ),
    ;

    private final String name;
    private final int iconResource;
    private final int summaryResource;

    SynchronizationProviderViewData(String name, int summaryResource, int iconResource) {
        this.name = name;
        this.iconResource = iconResource;
        this.summaryResource = summaryResource;
    }

    public String getName() {
        return name;
    }

    public int getIconResource() {
        return iconResource;
    }

    public int getSummaryResource() {
        return summaryResource;
    }
}
