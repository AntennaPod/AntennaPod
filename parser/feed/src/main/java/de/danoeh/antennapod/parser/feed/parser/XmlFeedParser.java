package de.danoeh.antennapod.parser.feed.parser;

import androidx.annotation.NonNull;

import org.apache.commons.io.input.XmlStreamReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.SyndHandler;
import de.danoeh.antennapod.parser.feed.type.TypeResolver;

public class XmlFeedParser implements FeedParser {
    @NonNull
    public FeedHandlerResult createFeedHandlerResult(Feed feed, TypeResolver.Type type)
            throws ParserConfigurationException, SAXException, IOException {
        SyndHandler handler = new SyndHandler(feed, type);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        File file = new File(feed.getFile_url());
        Reader inputStreamReader = new XmlStreamReader(file);
        InputSource inputSource = new InputSource(inputStreamReader);

        saxParser.parse(inputSource, handler);
        inputStreamReader.close();
        return new FeedHandlerResult(handler.state.feed, handler.state.alternateUrls, handler.state.redirectUrl);
    }
}
