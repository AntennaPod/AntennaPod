package de.podfetcher.syndication.namespace.atom;

import org.xml.sax.Attributes;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.syndication.handler.HandlerState;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;

public class NSAtom extends Namespace {
	public static final String NSTAG = "atom";
	public static final String NSURI = "http://www.w3.org/2005/Atom";

	private static final String FEED = "feed";
	private static final String TITLE = "title";
	private static final String ENTRY = "entry";
	private static final String LINK = "link";
	private static final String UPDATED = "updated";
	private static final String AUTHOR = "author";
	private static final String CONTENT = "content";

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
	private static final String LINK_REL_RELATED = "related";
	private static final String LINK_REL_SELF = "self";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ENTRY)) {
			state.setCurrentItem(new FeedItem());
			state.getFeed().getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());
		} else if (localName.equals(TITLE) || localName.equals(CONTENT)) {
			String type = attributes.getValue(null, TEXT_TYPE);
			return new AtomText(localName, this, type);
		} else if (localName.equals(LINK)) {
			String href = attributes.getValue(null, LINK_HREF);
			String rel = attributes.getValue(null, LINK_REL);
			SyndElement parent = state.getTagstack().peek();
			if (parent.getName().equals(ENTRY)) {
				if (rel == null || rel.equals(LINK_REL_ALTERNATE)) {
					state.getCurrentItem().setLink(href);
				} else if (rel.equals(LINK_REL_ENCLOSURE)) {
					long size = Long.parseLong(attributes.getValue(null,
							LINK_LENGTH));
					String type = attributes.getValue(null, LINK_TYPE);
					String download_url = attributes.getValue(null,
							LINK_REL_ENCLOSURE);
					state.getCurrentItem().setMedia(
							new FeedMedia(state.getCurrentItem(), download_url,
									size, type));
				}
			} else if (parent.getName().equals(FEED)) {
				if (rel == null || rel.equals(LINK_REL_ALTERNATE)) {
					state.getCurrentItem().setLink(href);
				}
			}
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleCharacters(HandlerState state, char[] ch, int start,
			int length) {

	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		// TODO Auto-generated method stub
	}

}
