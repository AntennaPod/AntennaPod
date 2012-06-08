package de.podfetcher.syndication.handler;

import java.util.ArrayList;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedMedia;

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
	
	public ArrayList<FeedItem> items;
	public FeedItem currentItem;
	public StringBuilder strBuilder;
	public Feed feed;
	public String active_root_element; // channel or item or image
	public String active_sub_element; // Not channel or item

	public RSSHandler(Feed f) {
		super();
		this.feed = f;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (active_sub_element != null) {
			strBuilder.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equals(ITEM)) {
			currentItem.setFeed(feed);
			items.add(currentItem);
		} else if (localName.equals(TITLE)) {
			if (active_root_element.equals(CHANNEL)) {
				feed.setTitle(strBuilder.toString());
			} else if(active_root_element.equals(ITEM)) {
				currentItem.setTitle(strBuilder.toString());
			} else if(active_root_element.equals(IMAGE)) {
				feed.getImage().setTitle(strBuilder.toString());
			}
		} else if (localName.equals(DESCR)) {
			if (active_root_element.equals(CHANNEL)) {
				feed.setDescription(strBuilder.toString());
			} else {
				currentItem.setDescription(strBuilder.toString());
			}
		} else if (localName.equals(LINK)) {
			if (active_root_element.equals(CHANNEL)) {
				feed.setLink(strBuilder.toString());
			} else if(active_root_element.equals(ITEM)){
				currentItem.setLink(strBuilder.toString());
			} 
		} else if (localName.equals(PUBDATE)) {
			if (active_root_element.equals(ITEM)) {
				currentItem.setPubDate(strBuilder.toString());
			}
		} else if (localName.equals(URL)) {
			if(active_root_element.equals(IMAGE)) {
				feed.getImage().setDownload_url(strBuilder.toString());
			}
		} else if(localName.equals(IMAGE)) {
			active_root_element = CHANNEL;
		}
		active_sub_element = null;
		strBuilder = new StringBuilder();
	}


	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (localName.equals(CHANNEL)) {
			if(feed == null) {
				feed = new Feed();
			}
			active_root_element = localName;
		} else if (localName.equals(ITEM)) {
			currentItem = new FeedItem();
			active_root_element = localName;
		} else if (localName.equals(TITLE)) {
			active_sub_element = localName;
		} else if (localName.equals(DESCR)) {
			active_sub_element = localName;
		} else if (localName.equals(LINK)) {
			active_sub_element = localName;
		} else if (localName.equals(PUBDATE)) {
			active_sub_element = localName;
		} else if (localName.equals(ENCLOSURE)) {
			currentItem.setMedia(new FeedMedia(currentItem,
											  attributes.getValue(ENC_URL),
											  Long.parseLong(attributes.getValue(ENC_LEN)),
											  attributes.getValue(ENC_TYPE)));
		} else if(localName.equals(IMAGE)) {
			feed.setImage(new FeedImage());
			active_root_element = localName;
		} else if(localName.equals(URL)) {
			active_sub_element = qName;
		}
		super.startElement(uri, localName, qName, attributes);
	}
	

}
