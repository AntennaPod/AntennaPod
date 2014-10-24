package de.danoeh.antennapod.core.syndication.handler;

import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.syndication.namespace.*;
import de.danoeh.antennapod.core.syndication.namespace.atom.NSAtom;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Superclass for all SAX Handlers which process Syndication formats */
public class SyndHandler extends DefaultHandler {
	private static final String TAG = "SyndHandler";
	private static final String DEFAULT_PREFIX = "";
	protected HandlerState state;

	public SyndHandler(Feed feed, TypeGetter.Type type) {
		state = new HandlerState(feed);
		if (type == TypeGetter.Type.RSS20 || type == TypeGetter.Type.RSS091) {
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
					if (BuildConfig.DEBUG)
						Log.d(TAG, "Recognized Atom namespace");
				}
			} else if (uri.equals(NSContent.NSURI)
					&& prefix.equals(NSContent.NSTAG)) {
				state.namespaces.put(uri, new NSContent());
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Recognized Content namespace");
			} else if (uri.equals(NSITunes.NSURI)
					&& prefix.equals(NSITunes.NSTAG)) {
				state.namespaces.put(uri, new NSITunes());
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Recognized ITunes namespace");
			} else if (uri.equals(NSSimpleChapters.NSURI)
					&& prefix.matches(NSSimpleChapters.NSTAG)) {
				state.namespaces.put(uri, new NSSimpleChapters());
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Recognized SimpleChapters namespace");
			} else if (uri.equals(NSMedia.NSURI)
					&& prefix.equals(NSMedia.NSTAG)) {
				state.namespaces.put(uri, new NSMedia());
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Recognized media namespace");
			}
		}
	}

	private Namespace getHandlingNamespace(String uri, String qName) {
		Namespace handler = state.namespaces.get(uri);
		if (handler == null && !state.defaultNamespaces.empty()
				&& !qName.contains(":")) {
			handler = state.defaultNamespaces.peek();
		}
		return handler;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		state.getFeed().setItems(state.getItems());
	}

	public HandlerState getState() {
		return state;
	}

}
