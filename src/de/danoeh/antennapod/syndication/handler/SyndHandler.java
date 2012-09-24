package de.danoeh.antennapod.syndication.handler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;
import de.danoeh.antennapod.syndication.namespace.atom.NSAtom;
import de.danoeh.antennapod.syndication.namespace.content.NSContent;
import de.danoeh.antennapod.syndication.namespace.itunes.NSITunes;
import de.danoeh.antennapod.syndication.namespace.rss20.NSRSS20;
import de.danoeh.antennapod.syndication.namespace.simplechapters.NSSimpleChapters;

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
		Namespace handler = getHandlingNamespace(uri, qName);
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
					state.contentBuf.append(ch, start, length);
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		Namespace handler = getHandlingNamespace(uri, qName);
		if (handler != null) {
			handler.handleElementEnd(localName, state);
			state.tagstack.pop();

		}
		state.contentBuf = null;

	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		if (state.defaultNamespaces.size() > 1 && prefix.equals(DEFAULT_PREFIX)) {
			state.defaultNamespaces.pop();
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// Find the right namespace
		if (!state.namespaces.containsKey(uri)) {
			if (uri.equals(NSAtom.NSURI)) {
				if (prefix.equals(DEFAULT_PREFIX)) {
					state.defaultNamespaces.push(new NSAtom());
				} else if (prefix.equals(NSAtom.NSTAG)) {
					state.namespaces.put(uri, new NSAtom());
					if (AppConfig.DEBUG)
						Log.d(TAG, "Recognized Atom namespace");
				}
			} else if (uri.equals(NSContent.NSURI)
					&& prefix.equals(NSContent.NSTAG)) {
				state.namespaces.put(uri, new NSContent());
				if (AppConfig.DEBUG)
					Log.d(TAG, "Recognized Content namespace");
			} else if (uri.equals(NSITunes.NSURI)
					&& prefix.equals(NSITunes.NSTAG)) {
				state.namespaces.put(uri, new NSITunes());
				if (AppConfig.DEBUG)
					Log.d(TAG, "Recognized ITunes namespace");
			} else if (uri.equals(NSSimpleChapters.NSURI)
					&& prefix.equals(NSSimpleChapters.NSTAG)) {
				state.namespaces.put(uri, new NSSimpleChapters());
				if (AppConfig.DEBUG)
					Log.d(TAG, "Recognized SimpleChapters namespace");
			}
		}
	}

	private Namespace getHandlingNamespace(String uri, String qName) {
		Namespace handler = state.namespaces.get(uri);
		if (handler == null && !state.defaultNamespaces.empty() && !qName.contains(":")) {
			handler = state.defaultNamespaces.peek();
		}
		return handler;
	}

	public HandlerState getState() {
		return state;
	}

}
