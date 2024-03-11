package de.danoeh.antennapod.storage.importexport;

import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.storage.preferences.BuildConfig;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Reads OPML documents.
 */
public class OpmlReader {
    private static final String TAG = "OpmlReader";

    private boolean isInOpml = false;

    /**
     * Reads an Opml document and returns a list of all OPML elements it can
     * find
     */
    public ArrayList<OpmlElement> readDocument(Reader reader)
            throws XmlPullParserException, IOException {
        ArrayList<OpmlElement> elementList = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(reader);
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Reached beginning of document");
                    }
                    break;
                case XmlPullParser.START_TAG:
                    if (xpp.getName().equals(OpmlSymbols.OPML)) {
                        isInOpml = true;
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Reached beginning of OPML tree.");
                        }
                    } else if (isInOpml && xpp.getName().equals(OpmlSymbols.OUTLINE)) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Found new Opml element");
                        }
                        OpmlElement element = new OpmlElement();

                        final String title = xpp.getAttributeValue(null, OpmlSymbols.TITLE);
                        if (!TextUtils.isEmpty(title)) {
                            Log.i(TAG, "Using title: " + title);
                            element.setText(title);
                        } else {
                            Log.i(TAG, "Title not found, using text");
                            element.setText(xpp.getAttributeValue(null, OpmlSymbols.TEXT));
                        }
                        element.setXmlUrl(xpp.getAttributeValue(null, OpmlSymbols.XMLURL));
                        element.setHtmlUrl(xpp.getAttributeValue(null, OpmlSymbols.HTMLURL));
                        element.setType(xpp.getAttributeValue(null, OpmlSymbols.TYPE));
                        if (!TextUtils.isEmpty(element.getXmlUrl())) {
                            if (TextUtils.isEmpty(element.getText())) {
                                Log.i(TAG, "Opml element has no text attribute.");
                                element.setText(element.getXmlUrl());
                            }
                            elementList.add(element);
                        } else {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Skipping element because of missing xml url");
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            eventType = xpp.next();
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Parsing finished.");
        }

        return elementList;
    }

}
