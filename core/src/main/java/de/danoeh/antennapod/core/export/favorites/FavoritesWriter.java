package de.danoeh.antennapod.core.export.favorites;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;

/** Writes saved favorites to file. */
public class FavoritesWriter implements ExportWriter {
    private static final String TAG = "FavoritesWriter";

    private static final int PAGE_LIMIT = 100;

    @Override
    public void writeDocument(List<Feed> feeds, Writer writer, Context context)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Log.d(TAG, "Starting to write document");

        InputStream templateStream = context.getAssets().open("html-export-template.html");
        String template = IOUtils.toString(templateStream, "UTF-8");
        template = template.replaceAll("\\{TITLE\\}", "Favorites");
        String[] templateParts = template.split("\\{FEEDS\\}");

        Map<Long, List<FeedItem>> favoriteByFeed = getFeedMap(getFavorites());

        writer.append(templateParts[0]);
        writer.append("<ul>");

        for (Long feedId : favoriteByFeed.keySet()) {
            List<FeedItem> favorites = favoriteByFeed.get(feedId);

            writer.append("<li><div>");
            writeFeed(writer, favorites.get(0).getFeed());

            writer.append("<ul style=\"text-align:left\">");
            for (FeedItem item : favorites) {
                writeFavoriteItem(writer, item);
            }
            writer.append("</ul></div></li>\n");
        }

        writer.append("</ul>");

        writer.append(templateParts[1]);

        Log.d(TAG, "Finished writing document");
    }

    private List<FeedItem> getFavorites() {
        int page = 0;

        List<FeedItem> favoritesList = new ArrayList<>();
        List<FeedItem> favoritesPage;
        do {
            favoritesPage = DBReader.getFavoriteItemsList(page * PAGE_LIMIT, PAGE_LIMIT);
            favoritesList.addAll(favoritesPage);
            ++page;
        } while (!favoritesPage.isEmpty() && favoritesPage.size() == PAGE_LIMIT);

        // sort in descending order
        Collections.sort(favoritesList, (lhs, rhs) -> rhs.getPubDate().compareTo(lhs.getPubDate()));

        return favoritesList;
    }

    /**
     * Group favorite episodes by feed, sorting them by publishing date in descending order.
     *
     * @param favoritesList {@code List} of all favorite episodes.
     * @return A {@code Map} favorite episodes, keyed by feed ID.
     */
    private Map<Long, List<FeedItem>> getFeedMap(List<FeedItem> favoritesList) {
        Map<Long, List<FeedItem>> feedMap = new TreeMap<>();

        for (FeedItem item : favoritesList) {
            List<FeedItem> feedEpisodes = feedMap.get(item.getFeedId());

            if (feedEpisodes == null) {
                feedEpisodes = new ArrayList<>();
                feedMap.put(item.getFeedId(), feedEpisodes);
            }

            feedEpisodes.add(item);
        }

        return feedMap;
    }

    private void writeFeed(Writer writer, Feed feed) throws IOException {
        writer.append("<img src=\"");
        writer.append(feed.getImageUrl());
        writer.append("\" /><p>");
        writer.append(feed.getTitle());
        writer.append(" <span><a href=\"");
        writer.append(feed.getLink());
        writer.append("\">Website</a> • <a href=\"");
        writer.append(feed.getDownload_url());
        writer.append("\">Feed</a></span></p>");
    }

    private void writeFavoriteItem(Writer writer, FeedItem item) throws IOException {
        writer.append("<li><span>");
        writer.append(item.getTitle().trim());
        writer.append("<br>\n[<a href=\"");
        writer.append(item.getLink());
        writer.append("\">Website</a>] • [<a href=\"");
        writer.append(item.getMedia().getDownload_url());
        writer.append("\">Media</a>]</span></li>\n");
    }

    @Override
    public String fileExtension() {
        return "html";
    }
}
