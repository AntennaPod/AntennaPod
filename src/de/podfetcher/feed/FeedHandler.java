package de.podfetcher.feed;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class FeedHandler {
	public final static String CHANNEL = "channel";
	public final static String ITEM = "item";
	public final static String TITLE = "title";
	public final static String LINK = "link";
	public final static String DESCR = "description";
	public final static String PUBDATE = "pubDate";
	public final static String ENCLOSURE = "enclosure";
	public final static String IMAGE = "image";
	public final static String URL = "url";
	
	public final static String ENC_URL = "url";
	public final static String ENC_LEN = "length";
	public final static String ENC_TYPE = "type";
	
	public Feed parseFeed(String file) throws ParserConfigurationException, SAXException {
		SAXParserFactory factory =  SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		RSSHandler handler = new RSSHandler();
		try {
			saxParser.parse(new File(file), handler);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return handler.feed;
	}
}
