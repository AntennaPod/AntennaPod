package de.podfetcher.syndication.handler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import de.podfetcher.feed.Feed;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.namespace.atom.NSAtom;
import de.podfetcher.syndication.namespace.content.NSContent;
import de.podfetcher.syndication.namespace.itunes.NSITunes;
import de.podfetcher.syndication.namespace.rss20.NSRSS20;
import de.podfetcher.syndication.namespace.simplechapters.NSSimpleChapters;

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
		state.contentBuf = new StringBuffer();
		Namespace handler = getHandlingNamespace(uri);
		if (handler != null) {
			SyndElement element = handler.handleElementStart(localName, state,
					attributes);
			state.tagstack.push(element);

		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (!state.tagstack.empty()) {
			if (state.getTagstack().size() >= 2) {
				if (state.contentBuf != null) {
				String content = new String(ch, start, length);
				state.contentBuf.append(content);
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		Namespace handler = getHandlingNamespace(uri);
		if (handler != null) {
			handler.handleElementEnd(localName, state);
			state.tagstack.pop();

		}
		state.contentBuf = null;

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
				Log.d(TAG, "Recognized Atom namespace");
			}
		} else if (uri.equals(NSContent.NSURI) && prefix.equals(NSContent.NSTAG)) {
			state.namespaces.put(uri, new NSContent());
			Log.d(TAG, "Recognized Content namespace");
		} else if (uri.equals(NSITunes.NSURI) && prefix.equals(NSITunes.NSTAG)) {
			state.namespaces.put(uri, new NSITunes());
			Log.d(TAG, "Recognized ITunes namespace");
		} else if (uri.equals(NSSimpleChapters.NSURI) && prefix.equals(NSSimpleChapters.NSTAG)) {
			state.namespaces.put(uri, new NSSimpleChapters());
			Log.d(TAG, "Recognized SimpleChapters namespace");
		}
	}

	private Namespace getHandlingNamespace(String uri) {
		Namespace handler = state.namespaces.get(uri);
		if (handler == null && uri.equals(DEFAULT_PREFIX)
				&& !state.defaultNamespaces.empty()) {
			handler = state.defaultNamespaces.peek();
		}
		return handler;
	}

	public HandlerState getState() {
		return state;
	}

}
