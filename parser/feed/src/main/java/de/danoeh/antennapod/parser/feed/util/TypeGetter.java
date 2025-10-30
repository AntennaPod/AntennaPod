package de.danoeh.antennapod.parser.feed.util;

import android.util.Log;

import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;
import org.apache.commons.io.input.XmlStreamReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import de.danoeh.antennapod.model.feed.Feed;

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
        if (feed.getLocalFileUrl() != null) {
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
                                Log.d(TAG, "Type is invalid: " + tag);
                                throwExceptionIfWebsite(feed);
                                throw new UnsupportedFeedtypeException(tag, null);
                        }
                    } else {
                        try {
                            eventType = xpp.next();
                        } catch (RuntimeException e) {
                            // Apparently this happens on some devices...
                            throw new UnsupportedFeedtypeException("Unable to get type");
                        }
                    }
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                throwExceptionIfWebsite(feed);
                throw new UnsupportedFeedtypeException(e.getMessage());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Log.d(TAG, "Type is invalid");
        throw new UnsupportedFeedtypeException("Unknown problem when trying to determine feed type");
    }

    private Reader createReader(Feed feed) {
        Reader reader;
        try {
            reader = new XmlStreamReader(new File(feed.getLocalFileUrl()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return reader;
    }

    private void throwExceptionIfWebsite(Feed feed) throws UnsupportedFeedtypeException {
        try {
            Document document = Jsoup.parse(new File(feed.getLocalFileUrl()));
            Element titleElement = document.head().getElementsByTag("title").first();
            if (titleElement != null) {
                throw new UnsupportedFeedtypeException("html", "Website title: \"" + titleElement.text() + "\"");
            }
            Element firstChild = document.children().first();
            throw new UnsupportedFeedtypeException(firstChild != null ? firstChild.tagName() : "?", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
