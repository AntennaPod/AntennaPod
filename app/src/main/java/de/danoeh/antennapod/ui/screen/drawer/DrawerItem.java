package de.danoeh.antennapod.ui.screen.drawer;

import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.NavDrawerData;

public class DrawerItem {
    private final Feed feed;
    private final NavDrawerData.TagItem tag;
    private int counter;
    private final int layer;

    public DrawerItem(@NonNull Feed feed, int counter, int layer) {
        this.tag = null;
        this.feed = feed;
        this.counter = counter;
        this.layer = layer;
    }

    public DrawerItem(@NonNull NavDrawerData.TagItem tag) {
        this.feed = null;
        this.tag = tag;
        this.counter = 0;
        this.layer = 0;
    }

    public boolean isFeed() {
        return feed != null;
    }

    public Feed asFeed() {
        return feed;
    }

    public NavDrawerData.TagItem asTag() {
        return tag;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }

    public int getLayer() {
        return layer;
    }

    public String getTitle() {
        return isFeed() ? feed.getTitle() : tag.getTitle();
    }

    public long getId() {
        return isFeed() ? feed.getId() : tag.getId();
    }
}