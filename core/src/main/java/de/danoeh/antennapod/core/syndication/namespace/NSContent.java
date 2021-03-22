package de.danoeh.antennapod.core.syndication.namespace;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.core.syndication.handler.HandlerState;

public class NSContent extends Namespace {
    public static final String NSTAG = "content";
    public static final String NSURI = "http://purl.org/rss/1.0/modules/content/";

    private static final String ENCODED = "encoded";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state, Attributes attributes) {
        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
        if (ENCODED.equals(localName) && state.getCurrentItem() != null && state.getContentBuf() != null) {
            state.getCurrentItem().setDescriptionIfLonger(state.getContentBuf().toString());
        }
    }

}
