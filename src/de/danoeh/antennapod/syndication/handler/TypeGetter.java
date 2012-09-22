package de.danoeh.antennapod.syndication.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;

/** Gets the type of a specific feed by reading the root element. */
public class TypeGetter {
	private static final String TAG = "TypeGetter";

	enum Type {
		RSS20, ATOM, INVALID
	}

	private static final String ATOM_ROOT = "feed";
	private static final String RSS_ROOT = "rss";

	public Type getType(Feed feed) throws UnsupportedFeedtypeException {
		XmlPullParserFactory factory;
		if (feed.getFile_url() != null) {
			try {
				factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				xpp.setInput(createReader(feed));
				int eventType = xpp.getEventType();

				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						String tag = xpp.getName();
						if (tag.equals(ATOM_ROOT)) {
							feed.setType(Feed.TYPE_ATOM1);
							if (AppConfig.DEBUG)
								Log.d(TAG, "Recognized type Atom");
							return Type.ATOM;
						} else if (tag.equals(RSS_ROOT)
								&& (xpp.getAttributeValue(null, "version")
										.equals("2.0"))) {
							feed.setType(Feed.TYPE_RSS2);
							if (AppConfig.DEBUG)
								Log.d(TAG, "Recognized type RSS 2.0");
							return Type.RSS20;
						} else {
							if (AppConfig.DEBUG)
								Log.d(TAG, "Type is invalid");
							throw new UnsupportedFeedtypeException(Type.INVALID);
						}
					} else {
						eventType = xpp.next();
					}
				}

			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Type is invalid");
		throw new UnsupportedFeedtypeException(Type.INVALID);
	}

	private Reader createReader(Feed feed) {
		FileReader reader;
		try {
			reader = new FileReader(new File(feed.getFile_url()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return reader;
	}
}
