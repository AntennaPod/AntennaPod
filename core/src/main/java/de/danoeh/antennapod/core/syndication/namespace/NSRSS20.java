package de.danoeh.antennapod.core.syndication.namespace;

import android.text.TextUtils;
import android.util.Log;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.syndication.util.SyndTypeUtils;
import de.danoeh.antennapod.core.util.DateUtils;

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

	public static final String CHANNEL = "channel";
	public static final String ITEM = "item";
	public static final String GUID = "guid";
	public static final String TITLE = "title";
	public static final String LINK = "link";
	public static final String DESCR = "description";
	public static final String PUBDATE = "pubDate";
	public static final String ENCLOSURE = "enclosure";
	public static final String IMAGE = "image";
	public static final String URL = "url";
	public static final String LANGUAGE = "language";

	public static final String ENC_URL = "url";
	public static final String ENC_LEN = "length";
	public static final String ENC_TYPE = "type";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (ITEM.equals(localName)) {
			state.setCurrentItem(new FeedItem());
			state.getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());

		} else if (ENCLOSURE.equals(localName)) {
			String type = attributes.getValue(ENC_TYPE);
			String url = attributes.getValue(ENC_URL);
			boolean validType;
			if(SyndTypeUtils.enclosureTypeValid(type)) {
				validType = true;
			} else {
				type = type = SyndTypeUtils.getValidMimeTypeFromUrl(url);
				validType = type != null;
			}
			if (state.getCurrentItem() != null && state.getCurrentItem().getMedia() == null &&
				validType) {
				long size = 0;
				try {
					size = Long.parseLong(attributes.getValue(ENC_LEN));
					if(size < 16384) {
						// less than 16kb is suspicious, check manually
						size = 0;
					}
				} catch (NumberFormatException e) {
					Log.d(TAG, "Length attribute could not be parsed.");
				}
				state.getCurrentItem().setMedia(
						new FeedMedia(state.getCurrentItem(), url, size, type));
			}

		} else if (IMAGE.equals(localName)) {
			if (state.getTagstack().size() >= 1) {
				String parent = state.getTagstack().peek().getName();
				if (CHANNEL.equals(parent)) {
					Feed feed = state.getFeed();
					if(feed != null && feed.getImage() == null) {
						feed.setImage(new FeedImage());
						feed.getImage().setOwner(state.getFeed());
					}
				}
			}
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (ITEM.equals(localName)) {
			if (state.getCurrentItem() != null) {
				FeedItem currentItem = state.getCurrentItem();
				// the title tag is optional in RSS 2.0. The description is used
				// as a
				// title if the item has no title-tag.
				if (currentItem.getTitle() == null) {
					currentItem.setTitle(currentItem.getDescription());
				}

                if (state.getTempObjects().containsKey(NSITunes.DURATION)) {
                    if (currentItem.hasMedia()) {
						Integer duration = (Integer) state.getTempObjects().get(NSITunes.DURATION);
						currentItem.getMedia().setDuration(duration);
                    }
                    state.getTempObjects().remove(NSITunes.DURATION);
                }
			}
			state.setCurrentItem(null);
		} else if (state.getTagstack().size() >= 2 && state.getContentBuf() != null) {
			String content = state.getContentBuf().toString();
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();
			String third = null;
			if (state.getTagstack().size() >= 3) {
				third = state.getThirdTag().getName();
			}
			if (GUID.equals(top) && ITEM.equals(second)) {
                // some feed creators include an empty or non-standard guid-element in their feed, which should be ignored
                if (!TextUtils.isEmpty(content) && state.getCurrentItem() != null) {
				    state.getCurrentItem().setItemIdentifier(content);
                }
			} else if (TITLE.equals(top)) {
				String title = content.trim();
				if (ITEM.equals(second) && state.getCurrentItem() != null) {
					state.getCurrentItem().setTitle(title);
				} else if (CHANNEL.equals(second) && state.getFeed() != null) {
					state.getFeed().setTitle(title);
				} else if (IMAGE.equals(second) && CHANNEL.equals(third)) {
					if(state.getFeed() != null && state.getFeed().getImage() != null &&
						state.getFeed().getImage().getTitle() == null) {
						state.getFeed().getImage().setTitle(title);
					}
				}
			} else if (LINK.equals(top)) {
				if (CHANNEL.equals(second) && state.getFeed() != null) {
					state.getFeed().setLink(content);
				} else if (ITEM.equals(second) && state.getCurrentItem() != null) {
					state.getCurrentItem().setLink(content);
				}
			} else if (PUBDATE.equals(top) && ITEM.equals(second) && state.getCurrentItem() != null) {
				state.getCurrentItem().setPubDate(DateUtils.parse(content));
			} else if (URL.equals(top) && IMAGE.equals(second) && CHANNEL.equals(third)) {
				// prefer itunes:image
				if(state.getFeed() != null && state.getFeed().getImage() != null &&
					state.getFeed().getImage().getDownload_url() == null) {
					state.getFeed().getImage().setDownload_url(content);
				}
			} else if (DESCR.equals(localName)) {
				if (CHANNEL.equals(second) && state.getFeed() != null) {
					state.getFeed().setDescription(content);
				} else if (ITEM.equals(second) && state.getCurrentItem() != null) {
					state.getCurrentItem().setDescription(content);
				}
			} else if (LANGUAGE.equals(localName) && state.getFeed() != null) {
				state.getFeed().setLanguage(content.toLowerCase());
			}
		}
	}

}
