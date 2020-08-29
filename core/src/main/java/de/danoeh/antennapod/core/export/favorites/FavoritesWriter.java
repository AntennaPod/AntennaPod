package de.danoeh.antennapod.core.export.favorites;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    private static final String FAVORITE_TEMPLATE = "html-export-favorites-item-template.html";
    private static final String FEED_TEMPLATE = "html-export-feed-template.html";

    @Override
    public void writeDocument(List<Feed> feeds, Writer writer, Context context)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Log.d(TAG, "Starting to write document");

        InputStream templateStream = context.getAssets().open("html-export-template.html");
        String template;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            template = IOUtils.toString(templateStream, StandardCharsets.UTF_8);
        } else {
            template = IOUtils.toString(templateStream, Charset.forName("UTF-8"));
        }
        template = template.replaceAll("\\{TITLE\\}", "Favorites");
        String[] templateParts = template.split("\\{FEEDS\\}");

        InputStream favTemplateStream = context.getAssets().open(FAVORITE_TEMPLATE);
        String favTemplate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            favTemplate = IOUtils.toString(favTemplateStream, StandardCharsets.UTF_8);
        } else {
            favTemplate = IOUtils.toString(favTemplateStream, Charset.forName("UTF-8"));
        }

        InputStream feedTemplateStream = context.getAssets().open(FEED_TEMPLATE);
        String feedTemplate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            feedTemplate = IOUtils.toString(feedTemplateStream, StandardCharsets.UTF_8);
        } else {
            feedTemplate = IOUtils.toString(feedTemplateStream, Charset.forName("UTF-8"));
        }

        Map<Long, List<FeedItem>> favoriteByFeed = getFeedMap(getFavorites());

        writer.append(templateParts[0]);

        for (Long feedId : favoriteByFeed.keySet()) {
            List<FeedItem> favorites = favoriteByFeed.get(feedId);
            writer.append("<li><div>\n");
            writeFeed(writer, favorites.get(0).getFeed(), feedTemplate);

            writer.append("<ul>\n");
            for (FeedItem item : favorites) {
                writeFavoriteItem(writer, item, favTemplate);
            }
            writer.append("</ul></div></li>\n");
        }

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

    private void writeFeed(Writer writer, Feed feed, String feedTemplate) throws IOException {
        String feedInfo = feedTemplate
                .replace("{FEED_IMG}", feed.getImageUrl())
                .replace("{FEED_TITLE}", feed.getTitle())
                .replace("{FEED_LINK}", feed.getLink())
                .replace("{FEED_WEBSITE}", feed.getDownload_url());

        writer.append(feedInfo);
    }

    private void writeFavoriteItem(Writer writer, FeedItem item, String favoriteTemplate) throws IOException {
        String favItem = favoriteTemplate
                .replace("{FAV_TITLE}", item.getTitle().trim())
                .replace("{FAV_WEBSITE}", item.getLink())
                .replace("{FAV_MEDIA}", item.getMedia().getDownload_url());

        writer.append(favItem);
    }

    @Override
    public String fileExtension() {
        return "html";
    }
}
