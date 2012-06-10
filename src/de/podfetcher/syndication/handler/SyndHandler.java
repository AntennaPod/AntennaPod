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


/** Superclass for all SAX Handlers which process Syndication formats */
public class SyndHandler extends DefaultHandler {
	private static final String TAG = "SyndHandler";
	private static final String DEFAULT_PREFIX = "";
	protected HandlerState state;

	public SyndHandler(Feed feed, TypeGetter.Type type) {
		state = new HandlerState(feed);
		if (type == TypeGetter.Type.RSS20) {
			state.defaultNamespaces.push(new NSRSS20());
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		Namespace handler = getHandlingNamespace(uri);
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
		Namespace handler = getHandlingNamespace(uri);
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
		// Find the right namespace
		if (uri.equals(NSAtom.NSURI)) {
			if (prefix.equals(DEFAULT_PREFIX)) {
				state.defaultNamespaces.push(new NSAtom());
			} else if (prefix.equals(NSAtom.NSTAG)) {
				state.namespaces.put(uri, new NSAtom());
			}
		}
	}
	
	private Namespace getHandlingNamespace(String uri) {
		Namespace handler = state.namespaces.get(uri);
		if (handler == null &&  uri.equals(DEFAULT_PREFIX) && !state.defaultNamespaces.empty()) {
			handler = state.defaultNamespaces.peek();
		}
		return handler;
	}

	public HandlerState getState() {
		return state;
	}

}
