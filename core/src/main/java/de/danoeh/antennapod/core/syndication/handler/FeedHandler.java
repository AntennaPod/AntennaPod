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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    /**
     * For updating items that are stored in the database, see also: DBTasks.searchFeedItemByIdentifyingValue
     */
    public static List<FeedItem> dedupItems(List<FeedItem> items) {
        if (items == null) {
            return null;
        }
        List<FeedItem> list = new ArrayList<>(items);
        Set<String> seen = new HashSet<>();
        Iterator<FeedItem> it = list.iterator();
        while (it.hasNext()) {
            FeedItem item = it.next();
            if (seen.contains(item.getItemIdentifier())) {
                Log.d(TAG, "Removing duplicate episode guid " + item.getItemIdentifier());
                it.remove();
                continue;
            }

            if (item.getMedia() == null || TextUtils.isEmpty(item.getMedia().getStreamUrl())) {
                continue;
            }
            if (seen.contains(item.getMedia().getStreamUrl())) {
                Log.d(TAG, "Removing duplicate episode stream url " + item.getMedia().getStreamUrl());
                it.remove();
            } else {
                seen.add(item.getMedia().getStreamUrl());
                if (TextUtils.isEmpty(item.getTitle()) || TextUtils.isEmpty(item.getPubDate().toString())) {
                    continue;
                }
                if (!seen.contains(item.getTitle() + item.getPubDate().toString())) {
                    seen.add(item.getTitle() + item.getPubDate().toString());
                } else {
                    Log.d(TAG, "Removing duplicate episode title and pubDate "
                            + item.getTitle()
                            + " " + item.getPubDate());
                    it.remove();
                }
            }
            seen.add(item.getItemIdentifier());
        }
        return list;
    }
}
