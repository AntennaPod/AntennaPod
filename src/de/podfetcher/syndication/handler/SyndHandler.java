package de.podfetcher.syndication.handler;

import org.xml.sax.helpers.DefaultHandler;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;

/** Superclass for all SAX Handlers which process Syndication formats */
public abstract class SyndHandler extends DefaultHandler{
	protected HandlerState state;

	public HandlerState getState() {
		return state;
	}
	
}
