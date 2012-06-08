package de.podfetcher.syndication.namespace;

import org.xml.sax.Attributes;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.handler.HandlerState;


public abstract class Namespace {
	public static final String NSTAG = null;
	public static final String NSURI = null;
	
	/** Called by a Feedhandler when in startElement and it detects a namespace element */
	public abstract void handleElement(String localName, HandlerState state, Attributes attributes);
	
	/** Called by a Feedhandler when in characters and it detects a namespace element */
	public abstract void handleCharacters(String localName, Feed feed, char ch[], int start, int length);
	
}
