package de.podfetcher.syndication.handler;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.podfetcher.feed.Feed;

public class FeedHandler {

	public Feed parseFeed(Feed feed) throws SAXException, IOException,
			ParserConfigurationException, UnsupportedFeedtypeException {
		TypeGetter tg = new TypeGetter();
		TypeGetter.Type type = tg.getType(feed);
		SyndHandler handler = new SyndHandler(feed, type);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(new File(feed.getFile_url()), handler);

		return handler.state.feed;
	}
}
