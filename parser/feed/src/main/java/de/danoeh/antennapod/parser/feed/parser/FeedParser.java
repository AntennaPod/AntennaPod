package de.danoeh.antennapod.parser.feed.parser;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;
import de.danoeh.antennapod.parser.feed.util.TypeResolver;

public interface FeedParser {
    FeedHandlerResult createFeedHandlerResult(Feed feed, TypeResolver.Type type) throws ParserConfigurationException,
            SAXException, IOException, UnsupportedFeedtypeException, JSONException;
}
