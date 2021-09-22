package de.danoeh.antennapod.parser.feed;

import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.util.TypeResolver;

public class FeedHandler {
    public FeedHandlerResult parseFeed(Feed feed) throws SAXException, IOException,
            ParserConfigurationException, UnsupportedFeedtypeException {
        TypeResolver typeResolver = new TypeResolver();
        TypeResolver.Type type = typeResolver.getType(feed);
        if (
                type.equals(TypeResolver.Type.ATOM)
                        || type.equals(TypeResolver.Type.RSS20)
                        || type.equals(TypeResolver.Type.RSS091)
        ) {
            return XmlFeedHandler.createFeedHandlerResult(feed, type);
        }
        throw new UnsupportedFeedtypeException(TypeResolver.Type.INVALID);
    }


}
