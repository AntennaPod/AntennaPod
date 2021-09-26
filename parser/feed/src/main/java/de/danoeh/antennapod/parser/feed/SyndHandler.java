package de.danoeh.antennapod.parser.feed;

import android.util.Log;

import de.danoeh.antennapod.parser.feed.util.TypeGetter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.namespace.Content;
import de.danoeh.antennapod.parser.feed.namespace.DublinCore;
import de.danoeh.antennapod.parser.feed.namespace.Itunes;
import de.danoeh.antennapod.parser.feed.namespace.Media;
import de.danoeh.antennapod.parser.feed.namespace.Rss20;
import de.danoeh.antennapod.parser.feed.namespace.SimpleChapters;
import de.danoeh.antennapod.parser.feed.namespace.Namespace;
import de.danoeh.antennapod.parser.feed.namespace.PodcastIndex;
import de.danoeh.antennapod.parser.feed.element.SyndElement;
import de.danoeh.antennapod.parser.feed.namespace.Atom;

/** Superclass for all SAX Handlers which process Syndication formats */
public class SyndHandler extends DefaultHandler {
    private static final String TAG = "SyndHandler";
    private static final String DEFAULT_PREFIX = "";
    public final HandlerState state;

    public SyndHandler(Feed feed, TypeGetter.Type type) {
        state = new HandlerState(feed);
        if (type == TypeGetter.Type.RSS20 || type == TypeGetter.Type.RSS091) {
            state.defaultNamespaces.push(new Rss20());
        }
    }

    @Override
    public void startElement(String uri, String localName, String qualifiedName,
            Attributes attributes) throws SAXException {
        state.contentBuf = new StringBuilder();
        Namespace handler = getHandlingNamespace(uri, qualifiedName);
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
    public void endElement(String uri, String localName, String qualifiedName)
            throws SAXException {
        Namespace handler = getHandlingNamespace(uri, qualifiedName);
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
            if (uri.equals(Atom.NSURI)) {
                if (prefix.equals(DEFAULT_PREFIX)) {
                    state.defaultNamespaces.push(new Atom());
                } else if (prefix.equals(Atom.NSTAG)) {
                    state.namespaces.put(uri, new Atom());
                    Log.d(TAG, "Recognized Atom namespace");
                }
            } else if (uri.equals(Content.NSURI)
                    && prefix.equals(Content.NSTAG)) {
                state.namespaces.put(uri, new Content());
                Log.d(TAG, "Recognized Content namespace");
            } else if (uri.equals(Itunes.NSURI)
                    && prefix.equals(Itunes.NSTAG)) {
                state.namespaces.put(uri, new Itunes());
                Log.d(TAG, "Recognized ITunes namespace");
            } else if (uri.equals(SimpleChapters.NSURI)
                    && prefix.matches(SimpleChapters.NSTAG)) {
                state.namespaces.put(uri, new SimpleChapters());
                Log.d(TAG, "Recognized SimpleChapters namespace");
            } else if (uri.equals(Media.NSURI)
                    && prefix.equals(Media.NSTAG)) {
                state.namespaces.put(uri, new Media());
                Log.d(TAG, "Recognized media namespace");
            } else if (uri.equals(DublinCore.NSURI)
                    && prefix.equals(DublinCore.NSTAG)) {
                state.namespaces.put(uri, new DublinCore());
                Log.d(TAG, "Recognized DublinCore namespace");
            } else if (uri.equals(PodcastIndex.NSURI) || uri.equals(PodcastIndex.NSURI2)
                    && prefix.equals(PodcastIndex.NSTAG)) {
                state.namespaces.put(uri, new PodcastIndex());
                Log.d(TAG, "Recognized PodcastIndex namespace");
            }
        }
    }

    private Namespace getHandlingNamespace(String uri, String qualifiedName) {
        Namespace handler = state.namespaces.get(uri);
        if (handler == null && !state.defaultNamespaces.empty()
                && !qualifiedName.contains(":")) {
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
