package de.danoeh.antennapod.core.syndication.handler;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.io.input.XmlStreamReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;

public class FeedHandler {
    private static final String TAG = "FeedHandler";

    public FeedHandlerResult parseFeed(Feed feed) throws SAXException, IOException,
            ParserConfigurationException, UnsupportedFeedtypeException {
        TypeGetter tg = new TypeGetter();
        TypeGetter.Type type = tg.getType(feed);
        SyndHandler handler = new SyndHandler(feed, type);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        File file = new File(feed.getFile_url());
        Reader inputStreamReader = new XmlStreamReader(file);
        InputSource inputSource = new InputSource(inputStreamReader);

        saxParser.parse(inputSource, handler);
        inputStreamReader.close();
        feed.setItems(dedupItems(feed.getItems()));
        return new FeedHandlerResult(handler.state.feed, handler.state.alternateUrls);
    }

    public static List<FeedItem> dedupItems(List<FeedItem> items) {
        List<FeedItem> list = items;
        if (list == null) {
            return null;
        }
        ArrayList<String> seen = new ArrayList<>();
        Iterator<FeedItem> it = list.iterator();
        while (it.hasNext()) {
            FeedItem item = it.next();
            if (seen.indexOf(item.getItemIdentifier()) == -1) {
                if (item.getMedia() == null || TextUtils.isEmpty(item.getMedia().getStreamUrl())) {
                    continue;
                }
                if (seen.indexOf(item.getMedia().getStreamUrl()) == -1) {
                    seen.add(item.getMedia().getDownload_url());
                    if (TextUtils.isEmpty(item.getTitle()) || TextUtils.isEmpty(item.getPubDate().toString())) {
                        continue;
                    }
                    if (seen.indexOf(item.getTitle() + item.getPubDate().toString()) == -1) {
                        seen.add(item.getTitle() + item.getPubDate().toString());
                    } else {
                        Log.d(TAG, "Removing duplicate episode title and pubDate "
                                + item.getTitle()
                                + " " + item.getPubDate());
                        it.remove();
                    }
                } else {
                    Log.d(TAG, "Removing duplicate episode stream url " + item.getMedia().getStreamUrl());
                    it.remove();
                }

                seen.add(item.getItemIdentifier());
            } else {
                Log.d(TAG, "Removing duplicate episode guid " + item.getItemIdentifier());
                it.remove();
            }
        }
        return list;
    }
}
