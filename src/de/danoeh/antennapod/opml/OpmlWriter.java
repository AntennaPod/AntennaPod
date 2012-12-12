package de.danoeh.antennapod.opml;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;

/** Writes OPML documents. */
public class OpmlWriter {
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
	public void writeDocument(List<Feed> feeds, Writer writer)
			throws IllegalArgumentException, IllegalStateException, IOException {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Starting to write document");
		XmlSerializer xs = Xml.newSerializer();
		xs.setOutput(writer);

		xs.startDocument(ENCODING, false);
		xs.startTag(null, OpmlSymbols.OPML);
		xs.attribute(null, OpmlSymbols.VERSION, OPML_VERSION);

		xs.startTag(null, OpmlSymbols.HEAD);
		xs.startTag(null, OpmlSymbols.TITLE);
		xs.text(OPML_TITLE);
		xs.endTag(null, OpmlSymbols.TITLE);
		xs.endTag(null, OpmlSymbols.HEAD);

		xs.startTag(null, OpmlSymbols.BODY);
		for (Feed feed : feeds) {
			xs.startTag(null, OpmlSymbols.OUTLINE);
			xs.attribute(null, OpmlSymbols.TEXT, feed.getTitle());
			if (feed.getType() != null) {
				xs.attribute(null, OpmlSymbols.TYPE, feed.getType());
			}
			xs.attribute(null, OpmlSymbols.XMLURL, feed.getDownload_url());
			if (feed.getLink() != null) {
				xs.attribute(null, OpmlSymbols.HTMLURL, feed.getLink());
			}
			xs.attribute(null, OpmlSymbols.PRIORITY, Integer.toString(feed.getPriority()));
			xs.endTag(null, OpmlSymbols.OUTLINE);
		}
		xs.endTag(null, OpmlSymbols.BODY);
		xs.endTag(null, OpmlSymbols.OPML);
		xs.endDocument();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Finished writing document");
	}
}
