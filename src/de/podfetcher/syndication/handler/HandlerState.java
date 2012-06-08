package de.podfetcher.syndication.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;

/** Contains all relevant information to describe the current state of a SyndHandler.*/
public class HandlerState {
	/** Feed that the Handler is currently processing. */
	protected Feed feed;
	protected FeedItem currentItem;
	protected Stack<String> tagstack;
	/** Namespaces that have been defined so far. */
	protected HashMap<String, Namespace> namespaces;
	
	public HandlerState(Feed feed) {
		this.feed = feed;
		tagstack = new Stack<String>();
		namespaces = new HashMap<String, Namespace>();
	}
	
	
	public Feed getFeed() {
		return feed;
	}
	public FeedItem getCurrentItem() {
		return currentItem;
	}
	public Stack<String> getTagstack() {
		return tagstack;
	}
	
	
}
