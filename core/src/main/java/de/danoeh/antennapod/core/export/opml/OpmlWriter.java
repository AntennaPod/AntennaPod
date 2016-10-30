package de.danoeh.antennapod.core.export.opml;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.util.DateUtils;

/** Writes OPML documents. */
public class OpmlWriter implements ExportWriter {

	private static final String TAG = "OpmlWriter";
	private static final String ENCODING = "UTF-8";
	private static final String OPML_VERSION = "2.0";
	private static final String OPML_TITLE = "AntennaPod Subscriptions";

	/**
	 * Takes a list of feeds and a writer and writes those into an OPML
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
		xs.setOutput(writer);

		xs.startDocument(ENCODING, false);
		xs.text("\n");
		xs.startTag(null, OpmlSymbols.OPML);
		xs.attribute(null, OpmlSymbols.VERSION, OPML_VERSION);
		xs.text("\n");

		xs.text("  ");
		xs.startTag(null, OpmlSymbols.HEAD);
		xs.text("\n");
		xs.text("    ");
		xs.startTag(null, OpmlSymbols.TITLE);
		xs.text(OPML_TITLE);
		xs.endTag(null, OpmlSymbols.TITLE);
		xs.text("\n");
		xs.text("    ");
		xs.startTag(null, OpmlSymbols.DATE_CREATED);
		xs.text(DateUtils.formatRFC822Date(new Date()));
		xs.endTag(null, OpmlSymbols.DATE_CREATED);
		xs.text("\n");
		xs.text("  ");
		xs.endTag(null, OpmlSymbols.HEAD);
		xs.text("\n");

		xs.text("  ");
		xs.startTag(null, OpmlSymbols.BODY);
		xs.text("\n");
		for (Feed feed : feeds) {
			xs.text("    ");
			xs.startTag(null, OpmlSymbols.OUTLINE);
			xs.attribute(null, OpmlSymbols.TEXT, feed.getTitle());
			xs.attribute(null, OpmlSymbols.TITLE, feed.getTitle());
			if (feed.getType() != null) {
				xs.attribute(null, OpmlSymbols.TYPE, feed.getType());
			}
			xs.attribute(null, OpmlSymbols.XMLURL, feed.getDownload_url());
			if (feed.getLink() != null) {
				xs.attribute(null, OpmlSymbols.HTMLURL, feed.getLink());
			}
			xs.endTag(null, OpmlSymbols.OUTLINE);
			xs.text("\n");
		}
		xs.text("  ");
		xs.endTag(null, OpmlSymbols.BODY);
		xs.text("\n");
		xs.endTag(null, OpmlSymbols.OPML);
		xs.text("\n");
		xs.endDocument();
		Log.d(TAG, "Finished writing document");
	}

	public String fileExtension() {
		return "opml";
	}

}
