package de.danoeh.antennapod.core.feed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.model.feed.FeedPreferences;

public class TagFilter {

    private final Set<String> tagIds;

    private boolean showIfCounterGreaterZero = false;

    private boolean showAutoDownloadEnabled = false;
    private boolean showAutoDownloadDisabled = false;

    private boolean showUpdatedEnabled = false;
    private boolean showUpdatedDisabled = false;

    private boolean showEpisodeNotificationEnabled = false;
    private boolean showEpisodeNotificationDisabled = false;


    public TagFilter(Set<String> tagIds) {
        this.tagIds = tagIds;
    }


    /**
     * Run a list of feed items through the filter.
     */
    public List<NavDrawerData.DrawerItem> filter(List<NavDrawerData.FolderDrawerItem> folders) {
        Set<NavDrawerData.DrawerItem> items = new HashSet<>();
        for (NavDrawerData.FolderDrawerItem folder : folders) {
            if (tagIds.size() == 0 && folder.name.equals(FeedPreferences.TAG_ROOT)) {
                items.addAll(folder.children);
                return new ArrayList<>(items);
            }
            if (tagIds.contains(String.valueOf(folder.id))) {
                items.addAll(folder.children);
            }
        }

        return new ArrayList<>(items);
    }

}
