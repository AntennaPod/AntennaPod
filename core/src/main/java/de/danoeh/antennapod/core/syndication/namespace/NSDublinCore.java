package de.danoeh.antennapod.core.syndication.namespace;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.util.DateUtils;

public class NSDublinCore extends Namespace {
    private static final String TAG = "NSDublinCore";
    public static final String NSTAG = "dc";
    public static final String NSURI = "http://purl.org/dc/elements/1.1/";

    private static final String ITEM = "item";
    private static final String DATE = "date";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
        if(state.getTagstack().size() >= 2
                && state.getContentBuf() != null) {
            String content = state.getContentBuf().toString();
            SyndElement topElement = state.getTagstack().peek();
            String top = topElement.getName();
            SyndElement secondElement = state.getSecondTag();
            String second = secondElement.getName();
            if (top.equals(DATE) && second.equals(ITEM)) {
                state.getCurrentItem().setPubDate(
                        DateUtils.parse(content));
            }
        }
    }
}
