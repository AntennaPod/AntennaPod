package de.test.antennapod.util.syndication.feedgenerator;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.util.DateUtils;

/**
 * Creates Atom feeds. See FeedGenerator for more information.
 */
public class AtomGenerator implements FeedGenerator{

    private static final String NS_ATOM = "http://www.w3.org/2005/Atom";

    private static final long FEATURE_USE_RFC3339LOCAL = 1;

    @Override
    public void writeFeed(Feed feed, OutputStream outputStream, String encoding, long flags) throws IOException {
        if (feed == null) throw new IllegalArgumentException("feed = null");
        if (outputStream == null) throw new IllegalArgumentException("outputStream = null");
        if (encoding == null) throw new IllegalArgumentException("encoding = null");

        XmlSerializer xml = Xml.newSerializer();
        xml.setOutput(outputStream, encoding);
        xml.startDocument(encoding, null);

        xml.startTag(null, "feed");
        xml.attribute(null, "xmlns", NS_ATOM);

        // Write Feed data
        if (feed.getIdentifyingValue() != null) {
            xml.startTag(null, "id");
            xml.text(feed.getIdentifyingValue());
            xml.endTag(null, "id");
        }
        if (feed.getTitle() != null) {
            xml.startTag(null, "title");
            xml.text(feed.getTitle());
            xml.endTag(null, "title");
        }
        if (feed.getLink() != null) {
            xml.startTag(null, "link");
            xml.attribute(null, "rel", "alternate");
            xml.attribute(null, "href", feed.getLink());
            xml.endTag(null, "link");
        }
        if (feed.getDescription() != null) {
            xml.startTag(null, "subtitle");
            xml.text(feed.getDescription());
            xml.endTag(null, "subtitle");
        }

        if (feed.getPaymentLink() != null) {
            GeneratorUtil.addPaymentLink(xml, feed.getPaymentLink(), false);
        }

        // Write FeedItem data
        if (feed.getItems() != null) {
            for (FeedItem item : feed.getItems()) {
                xml.startTag(null, "entry");

                if (item.getIdentifyingValue() != null) {
                    xml.startTag(null, "id");
                    xml.text(item.getIdentifyingValue());
                    xml.endTag(null, "id");
                }
                if (item.getTitle() != null) {
                    xml.startTag(null, "title");
                    xml.text(item.getTitle());
                    xml.endTag(null, "title");
                }
                if (item.getLink() != null) {
                    xml.startTag(null, "link");
                    xml.attribute(null, "rel", "alternate");
                    xml.attribute(null, "href", item.getLink());
                    xml.endTag(null, "link");
                }
                if (item.getPubDate() != null) {
                    xml.startTag(null, "published");
                    if ((flags & FEATURE_USE_RFC3339LOCAL) != 0) {
                        xml.text(DateUtils.formatRFC3339Local(item.getPubDate()));
                    } else {
                        xml.text(DateUtils.formatRFC3339UTC(item.getPubDate()));
                    }
                    xml.endTag(null, "published");
                }
                if (item.getDescription() != null) {
                    xml.startTag(null, "content");
                    xml.text(item.getDescription());
                    xml.endTag(null, "content");
                }
                if (item.getMedia() != null) {
                    FeedMedia media = item.getMedia();
                    xml.startTag(null, "link");
                    xml.attribute(null, "rel", "enclosure");
                    xml.attribute(null, "href", media.getDownload_url());
                    xml.attribute(null, "type", media.getMime_type());
                    xml.attribute(null, "length", String.valueOf(media.getSize()));
                    xml.endTag(null, "link");
                }

                if (item.getPaymentLink() != null) {
                    GeneratorUtil.addPaymentLink(xml, item.getPaymentLink(), false);
                }

                xml.endTag(null, "entry");
            }
        }

        xml.endTag(null, "feed");
        xml.endDocument();
    }
}
