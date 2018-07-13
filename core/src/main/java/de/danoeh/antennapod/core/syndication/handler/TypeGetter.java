package de.danoeh.antennapod.core.syndication.handler;

import android.util.Log;

import org.apache.commons.io.input.XmlStreamReader;
import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

import de.danoeh.antennapod.core.feed.Feed;

/** Gets the type of a specific feed by reading the root element. */
public class TypeGetter {
	private static final String TAG = "TypeGetter";

	public enum Type {
		RSS20, RSS091, ATOM, INVALID
	}

	private static final String ATOM_ROOT = "feed";
	private static final String RSS_ROOT = "rss";

	public Type getType(Feed feed) throws UnsupportedFeedtypeException {
		XmlPullParserFactory factory;
		if (feed.getFile_url() != null) {
			Reader reader = null;
			try {
				factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				reader = createReader(feed);
				xpp.setInput(reader);
				int eventType = xpp.getEventType();

				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						String tag = xpp.getName();
						switch (tag) {
							case ATOM_ROOT:
								feed.setType(Feed.TYPE_ATOM1);
								Log.d(TAG, "Recognized type Atom");

								String strLang = xpp.getAttributeValue("http://www.w3.org/XML/1998/namespace", "lang");
								if (strLang != null) {
									feed.setLanguage(strLang);
								}

								return Type.ATOM;
							case RSS_ROOT:
								String strVersion = xpp.getAttributeValue(null, "version");
								if (strVersion == null) {
									feed.setType(Feed.TYPE_RSS2);
									Log.d(TAG, "Assuming type RSS 2.0");
									return Type.RSS20;
								} else if (strVersion.equals("2.0")) {
									feed.setType(Feed.TYPE_RSS2);
									Log.d(TAG, "Recognized type RSS 2.0");
									return Type.RSS20;
								} else if (strVersion.equals("0.91") || strVersion.equals("0.92")) {
									Log.d(TAG, "Recognized type RSS 0.91/0.92");
									return Type.RSS091;
								}
								throw new UnsupportedFeedtypeException("Unsupported rss version");
							default:
								Log.d(TAG, "Type is invalid");
								throw new UnsupportedFeedtypeException(Type.INVALID, tag);
						}
					} else {
						eventType = xpp.next();
					}
				}
			} catch (XmlPullParserException e) {
				e.printStackTrace();
                // XML document might actually be a HTML document -> try to parse as HTML
                String rootElement = null;
                try {
                    if (Jsoup.parse(new File(feed.getFile_url()), null) != null) {
                        rootElement = "html";
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                throw new UnsupportedFeedtypeException(Type.INVALID, rootElement);

            } catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		Log.d(TAG, "Type is invalid");
		throw new UnsupportedFeedtypeException(Type.INVALID);
	}

	private Reader createReader(Feed feed) {
		Reader reader;
		try {
			reader = new XmlStreamReader(new File(feed.getFile_url()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return reader;
	}
}
