package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.util.LongIntMap;

public class SubscriptionsFilter {
    private static final String divider = ",";

    private final String[] properties;

    private boolean showIfCounterGreaterZero = false;

    private boolean showAutoDownloadEnabled = false;
    private boolean showAutoDownloadDisabled = false;

    private boolean showUpdatedEnabled = false;
    private boolean showUpdatedDisabled = false;

    public SubscriptionsFilter(String properties) {
        this(TextUtils.split(properties, divider));
    }


    public SubscriptionsFilter(String[] properties) {
        this.properties = properties;
        for (String property : properties) {
            // see R.arrays.feed_filter_values
            switch (property) {
                case "counter_greater_zero":
                    showIfCounterGreaterZero = true;
                    break;
                case "enabled_auto_download":
                    showAutoDownloadEnabled = true;
                    break;
                case "disabled_auto_download":
                    showAutoDownloadDisabled = true;
                    break;
                case "enabled_updates":
                    showUpdatedEnabled = true;
                    break;
                case "disabled_updates":
                    showUpdatedDisabled = true;
                    break;
                default:
                    break;
            }
        }
    }

    public boolean isEnabled() {
        return properties.length > 0;
    }

    /**
     * Run a list of feed items through the filter.
     */
    public List<Feed> filter(List<Feed> items, LongIntMap feedCounters) {
        if (properties.length == 0) {
            return items;
        }

        List<Feed> result = new ArrayList<>();

        for (Feed item : items) {
            FeedPreferences itemPreferences = item.getPreferences();

            // If the item does not meet a requirement, skip it.
            if (showAutoDownloadEnabled && !itemPreferences.getAutoDownload()) {
                continue;
            } else if (showAutoDownloadDisabled && itemPreferences.getAutoDownload()) {
                continue;
            }

            if (showUpdatedEnabled && !itemPreferences.getKeepUpdated()) {
                continue;
            } else if (showUpdatedDisabled && itemPreferences.getKeepUpdated()) {
                continue;
            }

            // If the item reaches here, it meets all criteria (except counter > 0)
            result.add(item);
        }

        if (showIfCounterGreaterZero) {
            for (int i = result.size() - 1; i >= 0; i--) {
                if (feedCounters.get(result.get(i).getId()) <= 0) {
                    result.remove(i);
                }
            }
        }

        return result;
    }

    public String[] getValues() {
        return properties.clone();
    }

    public String serialize() {
        return TextUtils.join(divider, getValues());
    }
}
