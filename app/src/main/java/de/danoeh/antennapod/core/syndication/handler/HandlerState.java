package de.danoeh.antennapod.core.syndication.handler;

import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.syndication.namespace.Namespace;
import de.danoeh.antennapod.core.syndication.namespace.SyndElement;

/**
 * Contains all relevant information to describe the current state of a
 * SyndHandler.
 */
public class HandlerState {

    /**
     * Feed that the Handler is currently processing.
     */
    protected Feed feed;
    /**
     * Contains links to related feeds, e.g. feeds with enclosures in other formats. The key of the map is the
     * URL of the feed, the value is the title
     */
    protected Map<String, String> alternateUrls;
    protected ArrayList<FeedItem> items;
    protected FeedItem currentItem;
    protected Stack<SyndElement> tagstack;
    /**
     * Namespaces that have been defined so far.
     */
    protected Map<String, Namespace> namespaces;
    protected Stack<Namespace> defaultNamespaces;
    /**
     * Buffer for saving characters.
     */
    protected StringBuffer contentBuf;

    /**
     * Temporarily saved objects.
     */
    protected Map<String, Object> tempObjects;

    public HandlerState(Feed feed) {
        this.feed = feed;
        alternateUrls = new ArrayMap<>();
        items = new ArrayList<>();
        tagstack = new Stack<>();
        namespaces = new ArrayMap<>();
        defaultNamespaces = new Stack<>();
        tempObjects = new ArrayMap<>();
    }

    public Feed getFeed() {
        return feed;
    }

    public ArrayList<FeedItem> getItems() {
        return items;
    }

    public FeedItem getCurrentItem() {
        return currentItem;
    }

    public Stack<SyndElement> getTagstack() {
        return tagstack;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public void setCurrentItem(FeedItem currentItem) {
        this.currentItem = currentItem;
    }

    /**
     * Returns the SyndElement that comes after the top element of the tagstack.
     */
    public SyndElement getSecondTag() {
        SyndElement top = tagstack.pop();
        SyndElement second = tagstack.peek();
        tagstack.push(top);
        return second;
    }

    public SyndElement getThirdTag() {
        SyndElement top = tagstack.pop();
        SyndElement second = tagstack.pop();
        SyndElement third = tagstack.peek();
        tagstack.push(second);
        tagstack.push(top);
        return third;
    }

    public StringBuffer getContentBuf() {
        return contentBuf;
    }

    public void addAlternateFeedUrl(String title, String url) {
        alternateUrls.put(url, title);
    }

    public Map<String, Object> getTempObjects() {
        return tempObjects;
    }
}
