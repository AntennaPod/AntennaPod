package de.podfetcher.feed;

import java.util.ArrayList;
import de.podfetcher.feed.FeedHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Parser for reading RSS-Feeds
 * 
 * @author daniel
 * 
 */
public class RSSHandler extends DefaultHandler {
	public ArrayList<FeedItem> items;
	public FeedItem currentItem;
	public StringBuilder strBuilder;
	public Feed feed;
	public String active_root_element; // channel or item or image
	public String active_sub_element; // Not channel or item

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (active_sub_element != null) {
			strBuilder.append(ch, start, length);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		feed.items = items;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase(FeedHandler.ITEM)) {
			currentItem.feed = feed;
			items.add(currentItem);
		} else if (qName.equalsIgnoreCase(FeedHandler.TITLE)) {
			if (active_root_element.equalsIgnoreCase(FeedHandler.CHANNEL)) {
				feed.title = strBuilder.toString();
			} else if(active_root_element.equalsIgnoreCase(FeedHandler.TITLE)) {
				currentItem.title = strBuilder.toString();
			} else if(active_root_element.equalsIgnoreCase(FeedHandler.IMAGE)) {
				feed.image.title = strBuilder.toString();
			}
		} else if (qName.equalsIgnoreCase(FeedHandler.DESCR)) {
			if (active_root_element.equalsIgnoreCase(FeedHandler.CHANNEL)) {
				feed.description = strBuilder.toString();
			} else {
				currentItem.description = strBuilder.toString();
			}
		} else if (qName.equalsIgnoreCase(FeedHandler.LINK)) {
			if (active_root_element.equalsIgnoreCase(FeedHandler.CHANNEL)) {
				feed.link = strBuilder.toString();
			} else if(active_root_element.equalsIgnoreCase(FeedHandler.TITLE)){
				currentItem.link = strBuilder.toString();
			} 
		} else if (qName.equalsIgnoreCase(FeedHandler.PUBDATE)) {
			if (active_root_element.equalsIgnoreCase(FeedHandler.ITEM)) {
				currentItem.pubDate = strBuilder.toString();
			}
		} else if (qName.equalsIgnoreCase(FeedHandler.URL)) {
			if(active_root_element.equalsIgnoreCase(FeedHandler.IMAGE)) {
				feed.image.download_url = strBuilder.toString();
			}
		} else if(qName.equalsIgnoreCase(FeedHandler.IMAGE)) {
			active_root_element = FeedHandler.CHANNEL;
		}
		active_sub_element = null;
		strBuilder = new StringBuilder();
	}

	@Override
	public void startDocument() throws SAXException {
		items = new ArrayList<FeedItem>();
		strBuilder = new StringBuilder();

	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(FeedHandler.CHANNEL)) {
			feed = new Feed();
			active_root_element = qName;
		} else if (qName.equalsIgnoreCase(FeedHandler.ITEM)) {
			currentItem = new FeedItem();
			active_root_element = qName;
		} else if (qName.equalsIgnoreCase(FeedHandler.TITLE)) {
			active_sub_element = qName;
		} else if (qName.equalsIgnoreCase(FeedHandler.DESCR)) {
			active_sub_element = qName;
		} else if (qName.equalsIgnoreCase(FeedHandler.LINK)) {
			active_sub_element = qName;
		} else if (qName.equalsIgnoreCase(FeedHandler.PUBDATE)) {
			active_sub_element = qName;
		} else if (qName.equalsIgnoreCase(FeedHandler.ENCLOSURE)) {
			currentItem.media = new FeedMedia(currentItem,
											  attributes.getValue(FeedHandler.ENC_URL),
											  Long.parseLong(attributes.getValue(FeedHandler.ENC_LEN)),
											  attributes.getValue(FeedHandler.ENC_TYPE));
		} else if(qName.equalsIgnoreCase(FeedHandler.IMAGE)) {
			feed.image = new FeedImage();
			active_root_element = qName;
		} else if(qName.equalsIgnoreCase(FeedHandler.URL)) {
			active_sub_element = qName;
		}

	}

}
