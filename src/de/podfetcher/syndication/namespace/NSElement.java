package de.podfetcher.syndication.namespace;

import org.xml.sax.Attributes;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.handler.HandlerState;

/** Defines a XML Element of a specific namespace */
public abstract class NSElement {
	/** Called by its namespace if the processing of the element gets more complex */
	public abstract void handleElement(String localName, HandlerState state, Attributes attributes);
}
