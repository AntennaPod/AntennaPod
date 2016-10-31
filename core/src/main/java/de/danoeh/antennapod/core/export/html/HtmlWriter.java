package de.danoeh.antennapod.core.export.html;

import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.feed.Feed;

/** Writes HTML documents. */
public class HtmlWriter implements ExportWriter {

    private static final String TAG = "HtmlWriter";
    private static final String ENCODING = "UTF-8";
    private static final String HTML_TITLE = "AntennaPod Subscriptions";

    /**
     * Takes a list of feeds and a writer and writes those into an HTML
     * document.
     *
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    @Override
    public void writeDocument(List<Feed> feeds, Writer writer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Log.d(TAG, "Starting to write document");
        XmlSerializer xs = Xml.newSerializer();
        xs.setFeature(HtmlSymbols.XML_FEATURE_INDENT_OUTPUT, true);
        xs.setOutput(writer);

        xs.startDocument(ENCODING, false);
        xs.startTag(null, HtmlSymbols.HTML);
        xs.startTag(null, HtmlSymbols.HEAD);
        xs.startTag(null, HtmlSymbols.TITLE);
        xs.text(HTML_TITLE);
        xs.endTag(null, HtmlSymbols.TITLE);
        xs.endTag(null, HtmlSymbols.HEAD);

        xs.startTag(null, HtmlSymbols.BODY);
        xs.startTag(null, HtmlSymbols.HEADING);
        xs.text(HTML_TITLE);
        xs.endTag(null, HtmlSymbols.HEADING);
        xs.startTag(null, HtmlSymbols.ORDERED_LIST);
        for (Feed feed : feeds) {
            xs.startTag(null, HtmlSymbols.LIST_ITEM);
            xs.text(feed.getTitle());
            if (!TextUtils.isEmpty(feed.getLink())) {
                xs.text(" [");
                xs.startTag(null, HtmlSymbols.LINK);
                xs.attribute(null, HtmlSymbols.LINK_DESTINATION, feed.getLink());
                xs.text("Website");
                xs.endTag(null, HtmlSymbols.LINK);
                xs.text("]");
            }
            xs.text(" [");
            xs.startTag(null, HtmlSymbols.LINK);
            xs.attribute(null, HtmlSymbols.LINK_DESTINATION, feed.getDownload_url());
            xs.text("Feed");
            xs.endTag(null, HtmlSymbols.LINK);
            xs.text("]");
            xs.endTag(null, HtmlSymbols.LIST_ITEM);
        }
        xs.endTag(null, HtmlSymbols.ORDERED_LIST);
        xs.endTag(null, HtmlSymbols.BODY);
        xs.endTag(null, HtmlSymbols.HTML);
        xs.endDocument();
        Log.d(TAG, "Finished writing document");
    }

    public String fileExtension() {
        return "html";
    }

}
