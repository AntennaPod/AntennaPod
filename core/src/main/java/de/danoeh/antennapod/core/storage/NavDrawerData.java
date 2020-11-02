package de.danoeh.antennapod.core.storage;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.util.LongIntMap;

import java.util.List;

public class NavDrawerData {
    public final List<DrawerItem> items;
    public final int queueSize;
    public final int numNewItems;
    public final int numDownloadedItems;
    public final LongIntMap feedCounters;
    public final int reclaimableSpace;

    public NavDrawerData(List<DrawerItem> feeds,
                         int queueSize,
                         int numNewItems,
                         int numDownloadedItems,
                         LongIntMap feedIndicatorValues,
                         int reclaimableSpace) {
        this.items = feeds;
        this.queueSize = queueSize;
        this.numNewItems = numNewItems;
        this.numDownloadedItems = numDownloadedItems;
        this.feedCounters = feedIndicatorValues;
        this.reclaimableSpace = reclaimableSpace;
    }

    public static class DrawerItem {
        public enum Type {
            FOLDER, FEED
        }

        public final Type type;
        public final int layer;
        public final long id;

        public DrawerItem(Type type, int layer, long id) {
            this.type = type;
            this.layer = layer;
            this.id = id;
        }
    }

    public static class FolderDrawerItem extends DrawerItem {
        public final List<DrawerItem> children;
        public final String name;

        public FolderDrawerItem(String name, List<DrawerItem> children, int layer, long id) {
            super(DrawerItem.Type.FOLDER, layer, id);
            this.children = children;
            this.name = name;
        }
    }

    public static class FeedDrawerItem extends DrawerItem {
        public final Feed feed;

        public FeedDrawerItem(Feed feed, int layer, long id) {
            super(DrawerItem.Type.FEED, layer, id);
            this.feed = feed;
        }
    }
}
