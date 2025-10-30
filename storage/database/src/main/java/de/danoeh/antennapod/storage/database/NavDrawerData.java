package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.Feed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavDrawerData {
    public final List<Feed> feeds;
    public final List<TagItem> tags;
    public final int queueSize;
    public final int numNewItems;
    public final int numDownloadedItems;
    public final Map<Long, Integer> feedCounters;

    public NavDrawerData(List<Feed> feeds,
                         List<TagItem> tags,
                         int queueSize,
                         int numNewItems,
                         int numDownloadedItems,
                         Map<Long, Integer> feedIndicatorValues) {
        this.feeds = feeds;
        this.tags = tags;
        this.queueSize = queueSize;
        this.numNewItems = numNewItems;
        this.numDownloadedItems = numDownloadedItems;
        this.feedCounters = feedIndicatorValues;
    }

    public static class TagItem {
        private final String name;
        private List<Feed> feeds = new ArrayList<>();
        private int counter = 0;
        private boolean isOpen = false;
        private long id;

        public TagItem(String name) {
            this.name = name;
            // Keep IDs >0 but make room for many feeds
            this.id = (Math.abs((long) name.hashCode()) << 20);
        }

        public String getTitle() {
            return name;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public void setOpen(final boolean open) {
            isOpen = open;
        }

        public List<Feed> getFeeds() {
            return feeds;
        }

        public int getCounter() {
            return counter;
        }

        public void addFeed(Feed feed, int feedCounter) {
            counter += feedCounter;
            feeds.add(feed);
        }

        public long getId() {
            return id;
        }
    }
}
