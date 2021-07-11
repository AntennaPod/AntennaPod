package de.danoeh.antennapod.core.syndication.namespace;

import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.core.syndication.util.SyndStringUtils;
import org.xml.sax.Attributes;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.syndication.util.SyndTypeUtils;
import de.danoeh.antennapod.core.util.DateUtils;

/**
 * SAX-Parser for reading RSS-Feeds.
 */
public class NSRSS20 extends Namespace {

    private static final String TAG = "NSRSS20";

    public static final String CHANNEL = "channel";
    public static final String ITEM = "item";
    private static final String GUID = "guid";
    private static final String TITLE = "title";
    private static final String LINK = "link";
    private static final String DESCR = "description";
    private static final String PUBDATE = "pubDate";
    private static final String ENCLOSURE = "enclosure";
    private static final String IMAGE = "image";
    private static final String URL = "url";
    private static final String LANGUAGE = "language";

    private static final String ENC_URL = "url";
    private static final String ENC_LEN = "length";
    private static final String ENC_TYPE = "type";

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

            boolean validType = SyndTypeUtils.enclosureTypeValid(type);
            if (!validType) {
                type = SyndTypeUtils.getMimeTypeFromUrl(url);
                validType = SyndTypeUtils.enclosureTypeValid(type);
            }

            boolean validUrl = !TextUtils.isEmpty(url);
            if (state.getCurrentItem() != null && state.getCurrentItem().getMedia() == null
                    && validType && validUrl) {
                long size = 0;
                try {
                    size = Long.parseLong(attributes.getValue(ENC_LEN));
                    if (size < 16384) {
                        // less than 16kb is suspicious, check manually
                        size = 0;
                    }
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Length attribute could not be parsed.");
                }
                FeedMedia media = new FeedMedia(state.getCurrentItem(), url, size, type);
                state.getCurrentItem().setMedia(media);
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
                // as a title if the item has no title-tag.
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
            String contentRaw = state.getContentBuf().toString();
            String content = SyndStringUtils.trimAllWhitespace(contentRaw);
            SyndElement topElement = state.getTagstack().peek();
            String top = topElement.getName();
            SyndElement secondElement = state.getSecondTag();
            String second = secondElement.getName();
            String third = null;
            if (state.getTagstack().size() >= 3) {
                third = state.getThirdTag().getName();
            }
            if (GUID.equals(top) && ITEM.equals(second)) {
                // some feed creators include an empty or non-standard guid-element in their feed,
                // which should be ignored
                if (!TextUtils.isEmpty(contentRaw) && state.getCurrentItem() != null) {
                    state.getCurrentItem().setItemIdentifier(contentRaw);
                }
            } else if (TITLE.equals(top)) {
                if (ITEM.equals(second) && state.getCurrentItem() != null) {
                    state.getCurrentItem().setTitle(content);
                } else if (CHANNEL.equals(second) && state.getFeed() != null) {
                    state.getFeed().setTitle(content);
                }
            } else if (LINK.equals(top)) {
                if (CHANNEL.equals(second) && state.getFeed() != null) {
                    state.getFeed().setLink(content);
                } else if (ITEM.equals(second) && state.getCurrentItem() != null) {
                    state.getCurrentItem().setLink(content);
                }
            } else if (PUBDATE.equals(top) && ITEM.equals(second) && state.getCurrentItem() != null) {
                state.getCurrentItem().setPubDate(DateUtils.parseOrNullIfFuture(content));
            } else if (URL.equals(top) && IMAGE.equals(second) && CHANNEL.equals(third)) {
                // prefer itunes:image
                if (state.getFeed() != null && state.getFeed().getImageUrl() == null) {
                    state.getFeed().setImageUrl(content);
                }
            } else if (DESCR.equals(localName)) {
                if (CHANNEL.equals(second) && state.getFeed() != null) {
                    state.getFeed().setDescription(content);
                } else if (ITEM.equals(second) && state.getCurrentItem() != null) {
                    state.getCurrentItem().setDescriptionIfLonger(content);
                }
            } else if (LANGUAGE.equals(localName) && state.getFeed() != null) {
                state.getFeed().setLanguage(content.toLowerCase());
            }
        }
    }

}
