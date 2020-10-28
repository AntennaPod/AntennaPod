package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.core.R;

public enum SubscriptionsFilterGroup {
    COUNTER_GREATER_ZERO(new ItemProperties(R.string.subscriptions_counter_greater_zero, "counter_greater_zero")),
    AUTO_DOWNLOAD(new ItemProperties(R.string.auto_downloaded, "enabled_auto_download"),
            new ItemProperties(R.string.not_auto_downloaded, "disabled_auto_download")),
    UPDATED(new ItemProperties(R.string.kept_updated, "enabled_updates"),
            new ItemProperties(R.string.not_kept_updated, "disabled_updates"));


    public final ItemProperties[] values;

    SubscriptionsFilterGroup(ItemProperties... values) {
        this.values = values;
    }

    public static class ItemProperties {

        public final int displayName;
        public final String filterId;

        public ItemProperties(int displayName, String filterId) {
            this.displayName = displayName;
            this.filterId = filterId;
        }

    }
}
