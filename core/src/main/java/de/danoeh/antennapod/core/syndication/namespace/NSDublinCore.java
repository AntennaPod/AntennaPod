package de.danoeh.antennapod.core.syndication.namespace;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.model.feed.FeedItem;
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
        if (state.getCurrentItem() != null && state.getContentBuf() != null &&
            state.getTagstack() != null && state.getTagstack().size() >= 2) {
            FeedItem currentItem = state.getCurrentItem();
            String top = state.getTagstack().peek().getName();
            String second = state.getSecondTag().getName();
            if (DATE.equals(top) && ITEM.equals(second)) {
                String content = state.getContentBuf().toString();
                currentItem.setPubDate(DateUtils.parseOrNullIfFuture(content));
            }
        }
    }

}
