package de.danoeh.antennapod.syndication.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.XmlStreamReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.danoeh.antennapod.feed.Feed;

public class FeedHandler {

	public Feed parseFeed(Feed feed) throws SAXException, IOException,
			ParserConfigurationException, UnsupportedFeedtypeException {
		TypeGetter tg = new TypeGetter();
		TypeGetter.Type type = tg.getType(feed);
		SyndHandler handler = new SyndHandler(feed, type);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser saxParser = factory.newSAXParser();
		File file = new File(feed.getFile_url());
		InputStream inputStream = new FileInputStream(file);
		Reader inputStreamReader = new XmlStreamReader(file);
		InputSource inputSource = new InputSource(inputStreamReader);

		saxParser.parse(inputSource, handler);
		inputStream.close();
		return handler.state.feed;
	}
}
