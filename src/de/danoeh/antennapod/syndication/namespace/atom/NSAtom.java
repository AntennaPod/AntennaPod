package de.danoeh.antennapod.syndication.namespace.atom;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.syndication.handler.HandlerState;
import de.danoeh.antennapod.syndication.namespace.NSRSS20;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;
import de.danoeh.antennapod.syndication.util.SyndDateUtils;
import de.danoeh.antennapod.syndication.util.SyndTypeUtils;

public class NSAtom extends Namespace {
	private static final String TAG = "NSAtom";
	public static final String NSTAG = "atom";
	public static final String NSURI = "http://www.w3.org/2005/Atom";

	private static final String FEED = "feed";
	private static final String ID = "id";
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
	// type-values
	private static final String LINK_TYPE_ATOM = "application/atom+xml";
	private static final String LINK_TYPE_HTML = "text/html";
	private static final String LINK_TYPE_XHTML = "application/xml+xhtml";

	private static final String LINK_TYPE_RSS = "application/rss+xml";

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
			state.getItems().add(state.getCurrentItem());
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
					if (SyndTypeUtils.enclosureTypeValid(type)
							|| (type = SyndTypeUtils
									.getValidMimeTypeFromUrl(href)) != null) {
						state.getCurrentItem().setMedia(
								new FeedMedia(state.getCurrentItem(), href,
										size, type));
					}
				} else if (rel.equals(LINK_REL_PAYMENT)) {
					state.getCurrentItem().setPaymentLink(href);
				}
			} else if (parent.getName().matches(isFeed)) {
				if (rel == null || rel.equals(LINK_REL_ALTERNATE)) {
					String type = attributes.getValue(LINK_TYPE);
					/*
					 * Use as link if a) no type-attribute is given and
					 * feed-object has no link yet b) type of link is
					 * LINK_TYPE_HTML or LINK_TYPE_XHTML
					 */
					if ((type == null && state.getFeed().getLink() == null)
							|| (type != null && (type.equals(LINK_TYPE_HTML) || type.equals(LINK_TYPE_XHTML)))) {
						state.getFeed().setLink(href);
					}
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
			String content;
			if (state.getContentBuf() != null) {
				content = state.getContentBuf().toString();
			} else {
				content = "";
			}
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();

			if (top.matches(isText)) {
				textElement = (AtomText) topElement;
				textElement.setContent(content);
			}

			if (top.equals(ID)) {
				if (second.equals(FEED)) {
					state.getFeed().setFeedIdentifier(content);
				} else if (second.equals(ENTRY)) {
					state.getCurrentItem().setItemIdentifier(content);
				}
			} else if (top.equals(TITLE)) {

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
			} else if (top.equals(UPDATED)) {
				if (second.equals(ENTRY)
						&& state.getCurrentItem().getPubDate() == null) {
					state.getCurrentItem().setPubDate(
							SyndDateUtils.parseRFC3339Date(content));
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
