package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.LongIntMap;

public class SubscriptionsFilter {
    private final String[] properties;

    private boolean showIfCounterGreaterZero = false;

    private boolean showAutoDownloadEnabled = false;
    private boolean showAutoDownloadDisabled = false;

    private boolean showUpdatedEnabled = false;
    private boolean showUpdatedDisabled = false;

    public SubscriptionsFilter(String properties) {
        this(TextUtils.split(properties, ","));
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

    public boolean areSubscriptionsFiltered() {
        return properties.length > 0;
    }

    /**
     * Run a list of feed items through the filter.
     */
    public List<Feed> filter(List<Feed> items, PodDBAdapter adapter) {
        if (properties.length == 0) {
            return items;
        }

        List<Feed> result = new ArrayList<>();

        // Check for filter combinations that will always return an empty list
        if (showAutoDownloadDisabled && showAutoDownloadEnabled) {
            return result;
        } else if (showUpdatedDisabled && showUpdatedEnabled) {
            return result;
        }

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
            long[] feedIds = new long[result.size()];
            for (int i = 0; i < feedIds.length; i++) {
                feedIds[i] = result.get(i).getId();
            }
            final LongIntMap feedCounters = adapter.getFeedCounters(feedIds);

            for (int i = result.size() - 1; i >= 0; i--) {
                if (feedCounters.get(result.get(i).getId()) <= 0) {
                    feedCounters.delete(result.get(i).getId());
                    result.remove(i);
                }
            }
        }

        return result;
    }

    public String[] getValues() {
        return properties.clone();
    }

}
