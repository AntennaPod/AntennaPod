package de.danoeh.antennapod.core.syndication.namespace;

import android.support.annotation.Nullable;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.core.syndication.handler.HandlerState;


public abstract class Namespace {
	@Nullable
    public static final String NSTAG = null;
	@Nullable
    public static final String NSURI = null;
	
	/** Called by a Feedhandler when in startElement and it detects a namespace element 
	 * 	@return The SyndElement to push onto the stack
	 * */
	public abstract SyndElement handleElementStart(String localName, HandlerState state, Attributes attributes);
	
	/** Called by a Feedhandler when in endElement and it detects a namespace element 
	 * 	@return true if namespace handled the element, false if it ignored it
	 * */
	public abstract void handleElementEnd(String localName, HandlerState state);
	
}
