package de.danoeh.antennapod.core.syndication.namespace;

import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.syndication.util.SyndDateUtils;
import de.danoeh.antennapod.core.syndication.util.SyndTypeUtils;
import org.xml.sax.Attributes;

/**
 * SAX-Parser for reading RSS-Feeds
 * 
 * @author daniel
 * 
 */
public class NSRSS20 extends Namespace {
	private static final String TAG = "NSRSS20";
	public static final String NSTAG = "rss";
	public static final String NSURI = "";

	public final static String CHANNEL = "channel";
	public final static String ITEM = "item";
	public final static String GUID = "guid";
	public final static String TITLE = "title";
	public final static String LINK = "link";
	public final static String DESCR = "description";
	public final static String PUBDATE = "pubDate";
	public final static String ENCLOSURE = "enclosure";
	public final static String IMAGE = "image";
	public final static String URL = "url";
	public final static String LANGUAGE = "language";

	public final static String ENC_URL = "url";
	public final static String ENC_LEN = "length";
	public final static String ENC_TYPE = "type";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(new FeedItem());
			state.getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());

		} else if (localName.equals(ENCLOSURE)) {
			String type = attributes.getValue(ENC_TYPE);
			String url = attributes.getValue(ENC_URL);
			if (state.getCurrentItem().getMedia() == null
					&& (SyndTypeUtils.enclosureTypeValid(type) || ((type = SyndTypeUtils
							.getValidMimeTypeFromUrl(url)) != null))) {

				long size = 0;
				try {
					size = Long.parseLong(attributes.getValue(ENC_LEN));
				} catch (NumberFormatException e) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, "Length attribute could not be parsed.");
				}
				state.getCurrentItem().setMedia(
						new FeedMedia(state.getCurrentItem(), url, size, type));
			}

		} else if (localName.equals(IMAGE)) {
			if (state.getTagstack().size() >= 1) {
				String parent = state.getTagstack().peek().getName();
				if (parent.equals(CHANNEL)) {
					state.getFeed().setImage(new FeedImage());
                    state.getFeed().getImage().setOwner(state.getFeed());
				}
			}
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ITEM)) {
			if (state.getCurrentItem() != null) {
				// the title tag is optional in RSS 2.0. The description is used
				// as a
				// title if the item has no title-tag.
				if (state.getCurrentItem().getTitle() == null) {
					state.getCurrentItem().setTitle(
							state.getCurrentItem().getDescription());
				}

                if (state.getTempObjects().containsKey(NSITunes.DURATION)) {
                    if (state.getCurrentItem().hasMedia()) {
                        state.getCurrentItem().getMedia().setDuration((Integer) state.getTempObjects().get(NSITunes.DURATION));
                    }
                    state.getTempObjects().remove(NSITunes.DURATION);
                }
			}
			state.setCurrentItem(null);
		} else if (state.getTagstack().size() >= 2
				&& state.getContentBuf() != null) {
			String content = state.getContentBuf().toString();
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();
			String third = null;
			if (state.getTagstack().size() >= 3) {
				third = state.getThirdTag().getName();
			}

			if (top.equals(GUID) && second.equals(ITEM)) {
                // some feed creators include an empty or non-standard guid-element in their feed, which should be ignored
                if (!content.isEmpty()) {
				    state.getCurrentItem().setItemIdentifier(content);
                }
			} else if (top.equals(TITLE)) {
				if (second.equals(ITEM)) {
					state.getCurrentItem().setTitle(content);
				} else if (second.equals(CHANNEL)) {
					state.getFeed().setTitle(content);
				} else if (second.equals(IMAGE) && third != null
						&& third.equals(CHANNEL)) {
					state.getFeed().getImage().setTitle(content);
				}
			} else if (top.equals(LINK)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setLink(content);
				} else if (second.equals(ITEM)) {
					state.getCurrentItem().setLink(content);
				}
			} else if (top.equals(PUBDATE) && second.equals(ITEM)) {
				state.getCurrentItem().setPubDate(
						SyndDateUtils.parseRFC822Date(content));
			} else if (top.equals(URL) && second.equals(IMAGE) && third != null
					&& third.equals(CHANNEL)) {
				state.getFeed().getImage().setDownload_url(content);
			} else if (localName.equals(DESCR)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setDescription(content);
				} else if (second.equals(ITEM)) {
					state.getCurrentItem().setDescription(content);
				}

			} else if (localName.equals(LANGUAGE)) {
				state.getFeed().setLanguage(content.toLowerCase());
			}
		}
	}

}
