package de.danoeh.antennapod.core.storage;

import de.danoeh.antennapod.model.feed.Feed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavDrawerData {
    public final List<DrawerItem> items;
    public final int queueSize;
    public final int numNewItems;
    public final int numDownloadedItems;
    public final Map<Long, Integer> feedCounters;
    public final int reclaimableSpace;

    public NavDrawerData(List<DrawerItem> feeds,
                         int queueSize,
                         int numNewItems,
                         int numDownloadedItems,
                         Map<Long, Integer> feedIndicatorValues,
                         int reclaimableSpace) {
        this.items = feeds;
        this.queueSize = queueSize;
        this.numNewItems = numNewItems;
        this.numDownloadedItems = numDownloadedItems;
        this.feedCounters = feedIndicatorValues;
        this.reclaimableSpace = reclaimableSpace;
    }

    public abstract static class DrawerItem {
        public enum Type {
            TAG, FEED
        }

        public final Type type;
        private int layer;
        public long id;

        public DrawerItem(Type type, long id) {
            this.type = type;
            this.id = id;
        }

        public int getLayer() {
            return layer;
        }

        public void setLayer(int layer) {
            this.layer = layer;
        }

        public abstract String getTitle();

        public abstract int getCounter();
    }

    public static class TagDrawerItem extends DrawerItem {
        public final List<DrawerItem> children = new ArrayList<>();
        public final String name;
        public boolean isOpen;

        public TagDrawerItem(String name) {
            // Keep IDs >0 but make room for many feeds
            super(DrawerItem.Type.TAG, Math.abs((long) name.hashCode()) << 20);
            this.name = name;
        }

        public String getTitle() {
            return name;
        }

        public int getCounter() {
            int sum = 0;
            for (DrawerItem item : children) {
                sum += item.getCounter();
            }
            return sum;
        }
    }

    public static class FeedDrawerItem extends DrawerItem {
        public final Feed feed;
        public final int counter;

        public FeedDrawerItem(Feed feed, long id, int counter) {
            super(DrawerItem.Type.FEED, id);
            this.feed = feed;
            this.counter = counter;
        }

        public String getTitle() {
            return feed.getTitle();
        }

        public int getCounter() {
            return counter;
        }
    }
}
