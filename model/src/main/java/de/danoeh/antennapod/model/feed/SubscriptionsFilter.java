package de.danoeh.antennapod.model.feed;

import android.text.TextUtils;

import java.util.Arrays;

public class SubscriptionsFilter {
    private static final String divider = ",";

    private final String[] properties;

    public final boolean showIfCounterGreaterZero;
    public final boolean hideNonSubscribedFeeds;
    public final boolean showAutoDownloadEnabled;
    public final boolean showAutoDownloadDisabled;
    public final boolean showUpdatedEnabled;
    public final boolean showUpdatedDisabled;
    public final boolean showEpisodeNotificationEnabled;
    public final boolean showEpisodeNotificationDisabled;

    public static final String COUNTER_GREATER_ZERO = "counter_greater_zero";
    public static final String ENABLED_AUTO_DOWNLOAD = "enabled_auto_download";
    public static final String DISABLED_AUTO_DOWNLOAD = "disabled_auto_download";
    public static final String ENABLED_UPDATES = "enabled_updates";
    public static final String DISABLED_UPDATES = "disabled_updates";
    public static final String EPISODE_NOTIFICATION_ENABLED = "episode_notification_enabled";
    public static final String EPISODE_NOTIFICATION_DISABLED = "episode_notification_disabled";
    public static final String SHOW_NON_SUBSCRIBED_FEEDS = "show_non_subscribed";

    public SubscriptionsFilter(String properties) {
        this(TextUtils.split(properties, divider));
    }


    public SubscriptionsFilter(String[] properties) {
        this.properties = properties;
        showIfCounterGreaterZero = hasProperty(COUNTER_GREATER_ZERO);
        showAutoDownloadEnabled = hasProperty(ENABLED_AUTO_DOWNLOAD);
        showAutoDownloadDisabled = hasProperty(DISABLED_AUTO_DOWNLOAD);
        showUpdatedEnabled = hasProperty(ENABLED_UPDATES);
        showUpdatedDisabled = hasProperty(DISABLED_UPDATES);
        showEpisodeNotificationEnabled = hasProperty(EPISODE_NOTIFICATION_ENABLED);
        showEpisodeNotificationDisabled = hasProperty(EPISODE_NOTIFICATION_DISABLED);
        hideNonSubscribedFeeds = !hasProperty(SHOW_NON_SUBSCRIBED_FEEDS);
    }

    private boolean hasProperty(String property) {
        return Arrays.asList(properties).contains(property);
    }

    public boolean isEnabled() {
        return properties.length > 0;
    }

    public String[] getValues() {
        return properties.clone();
    }

    public String serialize() {
        return TextUtils.join(divider, getValues());
    }
}
