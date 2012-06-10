package de.podfetcher.syndication.namespace;

import org.xml.sax.Attributes;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.handler.HandlerState;


public abstract class Namespace {
	public static final String NSTAG = null;
	public static final String NSURI = null;
	
	/** Called by a Feedhandler when in startElement and it detects a namespace element 
	 * 	@return The SyndElement to push onto the stack
	 * */
	public abstract SyndElement handleElementStart(String localName, HandlerState state, Attributes attributes);
	
	/** Called by a Feedhandler when in characters and it detects a namespace element */
	public abstract void handleCharacters(HandlerState state, char ch[], int start, int length);
	
	/** Called by a Feedhandler when in endElement and it detects a namespace element 
	 * 	@return true if namespace handled the element, false if it ignored it
	 * */
	public abstract void handleElementEnd(String localName, HandlerState state);
	
}
