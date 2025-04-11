package de.danoeh.antennapod.ui.screen.subscriptions;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;

public enum SubscriptionsFilterGroup {
    COUNTER_GREATER_ZERO(new ItemProperties(R.string.subscriptions_counter_greater_zero,
            SubscriptionsFilter.COUNTER_GREATER_ZERO)),
    AUTO_DOWNLOAD(new ItemProperties(R.string.auto_downloaded, SubscriptionsFilter.ENABLED_AUTO_DOWNLOAD),
            new ItemProperties(R.string.not_auto_downloaded, SubscriptionsFilter.DISABLED_AUTO_DOWNLOAD)),
    UPDATED(new ItemProperties(R.string.kept_updated, SubscriptionsFilter.ENABLED_UPDATES),
            new ItemProperties(R.string.not_kept_updated, SubscriptionsFilter.DISABLED_UPDATES)),
    NEW_EPISODE_NOTIFICATION(new ItemProperties(R.string.new_episode_notification_enabled,
                    SubscriptionsFilter.EPISODE_NOTIFICATION_ENABLED),
            new ItemProperties(R.string.new_episode_notification_disabled,
                    SubscriptionsFilter.EPISODE_NOTIFICATION_DISABLED));


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
