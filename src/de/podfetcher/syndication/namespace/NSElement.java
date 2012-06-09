package de.podfetcher.syndication.namespace;

import org.xml.sax.Attributes;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.handler.HandlerState;

/** Defines a XML Element of a specific namespace */
public abstract class NSElement extends SyndElement{
	protected Namespace namespace;
	
	public NSElement(String name, Namespace namespace) {
		super(name);
		this.namespace = namespace;
	}

	/** Called by its namespace if the processing of the element gets more complex */
	public abstract void handleElement(String localName, HandlerState state, Attributes attributes);

	@Override
	public Namespace getNamespace() {
		return namespace;
	}
	
}
