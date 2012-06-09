package de.podfetcher.syndication.handler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.namespace.atom.NSAtom;
import de.podfetcher.syndication.namespace.rss20.NSRSS20;

// TODO implement default namespace
/** Superclass for all SAX Handlers which process Syndication formats */
public class SyndHandler extends DefaultHandler {
	private static final String TAG = "SyndHandler";
	protected HandlerState state;

	public SyndHandler(Feed feed) {
		state = new HandlerState(feed);
		state.namespaces.put("", new NSRSS20()); // TODO remove later
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		Namespace handler = state.namespaces.get(uri);
		if (handler != null) {
			handler.handleElementStart(localName, state, attributes);
			state.tagstack.push(new SyndElement(localName, handler));
			
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

		SyndElement top = state.tagstack.peek();
		if (top.getNamespace() != null) {
			top.getNamespace().handleCharacters(state, ch, start, length);
		}
		// ignore element otherwise
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		Namespace handler = state.namespaces.get(uri);
		if (handler != null) {
			handler.handleElementEnd(localName, state);
			state.tagstack.pop();
			
		}
		
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO remove Namespace
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		Log.d(TAG, "Found Prefix Mapping with prefix " + prefix + " and uri "
				+ uri);
		// Find the right namespace
		if (prefix.equals(NSAtom.NSTAG) || uri.equals(NSAtom.NSURI)) {
			state.namespaces.put(uri, new NSAtom());
		}
	}

	public HandlerState getState() {
		return state;
	}

}
