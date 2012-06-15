package de.podfetcher.syndication.namespace.rss20;

import java.util.ArrayList;
import java.util.Date;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.syndication.handler.HandlerState;
import de.podfetcher.syndication.handler.SyndHandler;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.util.SyndDateUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Parser for reading RSS-Feeds
 * 
 * @author daniel
 * 
 */
public class NSRSS20 extends Namespace {
	public static final String NSTAG = "rss";
	public static final String NSURI = "";
	
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

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(new FeedItem());
			state.getFeed().getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());

		} else if (localName.equals(ENCLOSURE)) {
			state.getCurrentItem()
					.setMedia(new FeedMedia(state.getCurrentItem(), attributes
							.getValue(ENC_URL), Long.parseLong(attributes
							.getValue(ENC_LEN)), attributes.getValue(ENC_TYPE)));
		} else if (localName.equals(IMAGE)) {
			state.getFeed().setImage(new FeedImage());
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleCharacters(HandlerState state, char[] ch, int start,
			int length) {
		if (state.getTagstack().size() >= 2) {
			String content = new String(ch, start, length);
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();
			if (top.equals(TITLE)) {
				if (second.equals(ITEM)) {
					state.getCurrentItem().setTitle(content);
				} else if (second.equals(CHANNEL)) {
					state.getFeed().setTitle(content);
				} else if (second.equals(IMAGE)) {
					state.getFeed().getImage().setTitle(IMAGE);
				}
			} else if (top.equals(DESCR)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setDescription(content);
				} else if (second.equals(ITEM)) {
					state.getCurrentItem().setDescription(content);
				}
			} else if (top.equals(LINK)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setLink(content);
				} else if (second.equals(ITEM)) {
					state.getCurrentItem().setLink(content);
				}
			} else if (top.equals(PUBDATE) && second.equals(ITEM)) {
				state.getCurrentItem().setPubDate(SyndDateUtils.parseRFC822Date(content));
			} else if (top.equals(URL) && second.equals(IMAGE)) {
				state.getFeed().getImage().setDownload_url(content);
			}
		}
		
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(null);
		}
	}

}
