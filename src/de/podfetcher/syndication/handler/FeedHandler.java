package de.podfetcher.syndication.handler;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.podfetcher.feed.Feed;

public class FeedHandler {
	
	
	public Feed parseFeed(Feed feed) {
		TypeGetter tg = new TypeGetter();
		tg.getType(feed);
		RSSHandler handler = new RSSHandler(feed);
		try {
			SAXParserFactory factory =  SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new File(feed.getFile_url()), handler);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return handler.state.feed;
	}
}
