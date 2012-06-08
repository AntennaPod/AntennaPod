package de.podfetcher.syndication.namespace;

import org.xml.sax.Attributes;

import de.podfetcher.feed.Feed;


public abstract class Namespace {
	
	/** Called by a Feedhandler when in startElement and it detects a namespace element */
	public abstract void handleElement(String localName, Feed feed, Attributes attributes);
	
	/** Called by a Feedhandler when in characters and it detects a namespace element */
	public abstract void handleCharacters(String localName, Feed feed, char ch[], int start, int length);
	
	public abstract String getNsTag();
	public abstract String getNsURI();
}
