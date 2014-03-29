package de.danoeh.antennapod.syndication.handler;

import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Contains all relevant information to describe the current state of a
 * SyndHandler.
 */
public class HandlerState {

	/** Feed that the Handler is currently processing. */
	protected Feed feed;
	protected ArrayList<FeedItem> items;
	protected FeedItem currentItem;
	protected Stack<SyndElement> tagstack;
	/** Namespaces that have been defined so far. */
	protected HashMap<String, Namespace> namespaces;
	protected Stack<Namespace> defaultNamespaces;
	/** Buffer for saving characters. */
	protected StringBuffer contentBuf;

	public HandlerState(Feed feed) {
		this.feed = feed;
		items = new ArrayList<FeedItem>();
		tagstack = new Stack<SyndElement>();
		namespaces = new HashMap<String, Namespace>();
		defaultNamespaces = new Stack<Namespace>();
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

}
