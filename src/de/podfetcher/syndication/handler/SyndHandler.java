package de.podfetcher.syndication.handler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.namespace.atom.NSAtom;

/** Superclass for all SAX Handlers which process Syndication formats */
public abstract class SyndHandler extends DefaultHandler{
	private static final String TAG = "SyndHandler";
	protected HandlerState state;

	public SyndHandler(Feed feed) {
		state = new HandlerState(feed);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		state.tagstack.push(new SyndElement(qName));
		
		
		Namespace handler = state.namespaces.get(uri);
		if (handler != null) {
			handler.handleElement(localName, state, attributes);
		}
	}
	
	

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		state.tagstack.pop();
	}



	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO remove Namespace
	}



	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		Log.d(TAG, "Found Prefix Mapping with prefix " + prefix + " and uri " + uri);
		// Find the right namespace
		if (prefix.equals(NSAtom.NSTAG) || uri.equals(NSAtom.NSURI)) {
			state.namespaces.put(uri, new NSAtom());
		}
	}

	public HandlerState getState() {
		return state;
	}
	
}
