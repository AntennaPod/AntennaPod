package de.test.antennapod.util.syndication.feedgenerator;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.DateUtils;

/**
 * Creates RSS 2.0 feeds. See FeedGenerator for more information.
 */
public class Rss2Generator implements FeedGenerator {

    public static final long FEATURE_WRITE_GUID = 1;

    @Override
    public void writeFeed(Feed feed, OutputStream outputStream, String encoding, long flags) throws IOException {
        if (feed == null) {
            throw new IllegalArgumentException("feed = null");
        } else if (outputStream == null) {
            throw new IllegalArgumentException("outputStream = null");
        } else if (encoding == null) {
            throw new IllegalArgumentException("encoding = null");
        }

        XmlSerializer xml = Xml.newSerializer();
        xml.setOutput(outputStream, encoding);
        xml.startDocument(encoding, null);

        xml.setPrefix("atom", "http://www.w3.org/2005/Atom");
        xml.startTag(null, "rss");
        xml.attribute(null, "version", "2.0");
        xml.startTag(null, "channel");

        // Write Feed data
        if (feed.getTitle() != null) {
            xml.startTag(null, "title");
            xml.text(feed.getTitle());
            xml.endTag(null, "title");
        }
        if (feed.getDescription() != null) {
            xml.startTag(null, "description");
            xml.text(feed.getDescription());
            xml.endTag(null, "description");
        }
        if (feed.getLink() != null) {
            xml.startTag(null, "link");
            xml.text(feed.getLink());
            xml.endTag(null, "link");
        }
        if (feed.getLanguage() != null) {
            xml.startTag(null, "language");
            xml.text(feed.getLanguage());
            xml.endTag(null, "language");
        }
        if (feed.getImageUrl() != null) {
            xml.startTag(null, "image");
            xml.startTag(null, "url");
            xml.text(feed.getImageUrl());
            xml.endTag(null, "url");
            xml.endTag(null, "image");
        }

        if (feed.getPaymentLink() != null) {
            GeneratorUtil.addPaymentLink(xml, feed.getPaymentLink(), true);
        }

        // Write FeedItem data
        if (feed.getItems() != null) {
            for (FeedItem item : feed.getItems()) {
                xml.startTag(null, "item");

                if (item.getTitle() != null) {
                    xml.startTag(null, "title");
                    xml.text(item.getTitle());
                    xml.endTag(null, "title");
                }
                if (item.getDescription() != null) {
                    xml.startTag(null, "description");
                    xml.text(item.getDescription());
                    xml.endTag(null, "description");
                }
                if (item.getLink() != null) {
                    xml.startTag(null, "link");
                    xml.text(item.getLink());
                    xml.endTag(null, "link");
                }
                if (item.getPubDate() != null) {
                    xml.startTag(null, "pubDate");
                    xml.text(DateUtils.formatRFC822Date(item.getPubDate()));
                    xml.endTag(null, "pubDate");
                }
                if ((flags & FEATURE_WRITE_GUID) != 0) {
                    xml.startTag(null, "guid");
                    xml.text(item.getItemIdentifier());
                    xml.endTag(null, "guid");
                }
                if (item.getMedia() != null) {
                    xml.startTag(null, "enclosure");
                    xml.attribute(null, "url", item.getMedia().getDownload_url());
                    xml.attribute(null, "length", String.valueOf(item.getMedia().getSize()));
                    xml.attribute(null, "type", item.getMedia().getMime_type());
                    xml.endTag(null, "enclosure");
                }
                if (item.getPaymentLink() != null) {
                    GeneratorUtil.addPaymentLink(xml, item.getPaymentLink(), true);
                }

                xml.endTag(null, "item");
            }
        }

        writeAdditionalAttributes(xml);

        xml.endTag(null, "channel");
        xml.endTag(null, "rss");

        xml.endDocument();
    }

    protected void writeAdditionalAttributes(XmlSerializer xml) throws IOException {

    }
}
