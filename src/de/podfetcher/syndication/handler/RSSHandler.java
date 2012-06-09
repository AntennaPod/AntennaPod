package de.podfetcher.syndication.handler;

import java.util.ArrayList;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.syndication.namespace.SyndElement;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Parser for reading RSS-Feeds
 * 
 * @author daniel
 * 
 */
public class RSSHandler extends SyndHandler {
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


	public RSSHandler(Feed feed) {
		super(feed);
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (state.tagstack.size() >= 2) {
			String content = new String(ch, start, length);
			SyndElement topElement = state.tagstack.pop();
			String top = topElement.getName();
			String second = state.tagstack.peek().getName();
			state.tagstack.push(topElement);
			if (top.equals(TITLE)) {
				if (second.equals(ITEM)) {
					state.currentItem.setTitle(content);
				} else if (second.equals(CHANNEL)) {
					state.feed.setTitle(content);
				} else if (second.equals(IMAGE)) {
					state.feed.getImage().setTitle(IMAGE);
				}
			} else if (top.equals(DESCR)) {
				if (second.equals(CHANNEL)) {
					state.feed.setDescription(content);
				} else if (second.equals(ITEM)) {
					state.feed.setDescription(content);
				}
			} else if (top.equals(LINK)) {
				if (second.equals(CHANNEL)) {
					state.feed.setLink(content);
				} else if (second.equals(ITEM)) {
					state.currentItem.setLink(content);
				}
			} else if (top.equals(PUBDATE) && second.equals(ITEM)) {
				state.currentItem.setPubDate(content);
			} else if (top.equals(URL) && second.equals(IMAGE)) {
				state.feed.getImage().setDownload_url(content);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equals(ITEM)) {
			state.currentItem = null;
		}
		super.endElement(uri, localName, qName);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		if (localName.equals(ITEM)) {
			state.currentItem = new FeedItem();
			state.feed.getItems().add(state.currentItem);
			state.currentItem.setFeed(state.feed);

		} else if (localName.equals(ENCLOSURE)) {
			state.currentItem
					.setMedia(new FeedMedia(state.currentItem, attributes
							.getValue(ENC_URL), Long.parseLong(attributes
							.getValue(ENC_LEN)), attributes.getValue(ENC_TYPE)));
		} else if (localName.equals(IMAGE)) {
			state.feed.setImage(new FeedImage());
		}

		super.startElement(uri, localName, qName, attributes);
	}

}
