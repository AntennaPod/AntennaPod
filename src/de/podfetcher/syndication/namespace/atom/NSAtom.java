package de.podfetcher.syndication.namespace.atom;

import org.xml.sax.Attributes;

import android.util.Log;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.syndication.handler.HandlerState;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.namespace.rss20.NSRSS20;
import de.podfetcher.syndication.util.SyndDateUtils;

public class NSAtom extends Namespace {
	private static final String TAG = "NSAtom";
	public static final String NSTAG = "atom";
	public static final String NSURI = "http://www.w3.org/2005/Atom";

	private static final String FEED = "feed";
	private static final String TITLE = "title";
	private static final String ENTRY = "entry";
	private static final String LINK = "link";
	private static final String UPDATED = "updated";
	private static final String AUTHOR = "author";
	private static final String CONTENT = "content";
	private static final String IMAGE = "logo";
	private static final String SUBTITLE = "subtitle";
	private static final String PUBLISHED = "published";

	private static final String TEXT_TYPE = "type";
	// Link
	private static final String LINK_HREF = "href";
	private static final String LINK_REL = "rel";
	private static final String LINK_TYPE = "type";
	private static final String LINK_TITLE = "title";
	private static final String LINK_LENGTH = "length";
	// rel-values
	private static final String LINK_REL_ALTERNATE = "alternate";
	private static final String LINK_REL_ENCLOSURE = "enclosure";
	private static final String LINK_REL_PAYMENT = "payment";
	private static final String LINK_REL_RELATED = "related";
	private static final String LINK_REL_SELF = "self";

	/** Regexp to test whether an Element is a Text Element. */
	private static final String isText = TITLE + "|" + CONTENT + "|" + "|"
			+ SUBTITLE;

	public static final String isFeed = FEED + "|" + NSRSS20.CHANNEL;
	public static final String isFeedItem = ENTRY + "|" + NSRSS20.ITEM;
	
	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ENTRY)) {
			state.setCurrentItem(new FeedItem());
			state.getFeed().getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());
		} else if (localName.matches(isText)) {
			String type = attributes.getValue(TEXT_TYPE);
			return new AtomText(localName, this, type);
		} else if (localName.equals(LINK)) {
			String href = attributes.getValue(LINK_HREF);
			String rel = attributes.getValue(LINK_REL);
			SyndElement parent = state.getTagstack().peek();
			if (parent.getName().matches(isFeedItem)) {
				if (rel == null || rel.equals(LINK_REL_ALTERNATE)) {
					state.getCurrentItem().setLink(href);
				} else if (rel.equals(LINK_REL_ENCLOSURE)) {
					String strSize = attributes.getValue(LINK_LENGTH);
					long size = 0;
					if (strSize != null)
						size = Long.parseLong(strSize);
					String type = attributes.getValue(LINK_TYPE);
					String download_url = attributes
							.getValue(LINK_REL_ENCLOSURE);
					state.getCurrentItem().setMedia(
							new FeedMedia(state.getCurrentItem(), download_url,
									size, type));
				} else if (rel.equals(LINK_REL_PAYMENT)) {
					state.getCurrentItem().setPaymentLink(href);
				}
			} else if (parent.getName().matches(isFeed)) {
				if (rel == null || rel.equals(LINK_REL_ALTERNATE)) {
					state.getFeed().setLink(href);
				} else if (rel.equals(LINK_REL_PAYMENT)) {
					state.getFeed().setPaymentLink(href);
				}
			}
		}
		return new SyndElement(localName, this);
	}


	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ENTRY)) {
			state.setCurrentItem(null);
		}

		if (state.getTagstack().size() >= 2) {
			AtomText textElement = null;
			String content = state.getContentBuf().toString();
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();

			if (top.matches(isText)) {
				textElement = (AtomText) topElement;
				textElement.setContent(content);
			}

			if (top.equals(TITLE)) {

				if (second.equals(FEED)) {
					state.getFeed().setTitle(textElement.getProcessedContent());
				} else if (second.equals(ENTRY)) {
					state.getCurrentItem().setTitle(
							textElement.getProcessedContent());
				}
			} else if (top.equals(SUBTITLE)) {
				if (second.equals(FEED)) {
					state.getFeed().setDescription(
							textElement.getProcessedContent());
				}
			} else if (top.equals(CONTENT)) {
				if (second.equals(ENTRY)) {
					state.getCurrentItem().setDescription(
							textElement.getProcessedContent());
				}
			} else if (top.equals(PUBLISHED)) {
				if (second.equals(ENTRY)) {
					state.getCurrentItem().setPubDate(
							SyndDateUtils.parseRFC3339Date(content));
				}
			} else if (top.equals(IMAGE)) {
				state.getFeed().setImage(new FeedImage(content, null));
			}

		}
	}

}
