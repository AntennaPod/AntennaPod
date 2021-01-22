package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.LongList;

import static de.danoeh.antennapod.core.feed.FeedItem.TAG_FAVORITE;

public class FeedItemFilter {

    private final String[] mProperties;
    private final String mQuery;

    private boolean showPlayed = false;
    private boolean showUnplayed = false;
    private boolean showPaused = false;
    private boolean showNotPaused = false;
    private boolean showQueued = false;
    private boolean showNotQueued = false;
    private boolean showDownloaded = false;
    private boolean showNotDownloaded = false;
    private boolean showHasMedia = false;
    private boolean showNoMedia = false;
    private boolean showIsFavorite = false;
    private boolean showNotFavorite = false;

    public FeedItemFilter(String properties) {
        this(TextUtils.split(properties, ","));
    }

    public FeedItemFilter(String[] properties) {
        this.mProperties = properties;
        for (String property : properties) {
            // see R.arrays.feed_filter_values
            switch (property) {
                case "unplayed":
                    showUnplayed = true;
                    break;
                case "paused":
                    showPaused = true;
                    break;
                case "not_paused":
                    showNotPaused = true;
                    break;
                case "played":
                    showPlayed = true;
                    break;
                case "queued":
                    showQueued = true;
                    break;
                case "not_queued":
                    showNotQueued = true;
                    break;
                case "downloaded":
                    showDownloaded = true;
                    break;
                case "not_downloaded":
                    showNotDownloaded = true;
                    break;
                case "has_media":
                    showHasMedia = true;
                    break;
                case "no_media":
                    showNoMedia = true;
                    break;
                case "is_favorite":
                    showIsFavorite = true;
                    break;
                case "not_favorite":
                    showNotFavorite = true;
                    break;
                default:
                    break;
            }
        }

        mQuery = makeQuery();
    }

    private String makeQuery() {
        // The keys used within this method, but explicitly combined with their table
        String keyRead = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_READ;
        String keyPosition = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_POSITION;
        String keyDownloaded = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DOWNLOADED;
        String keyMediaId = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_ID;
        String keyItemId = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_ID;
        String keyFeedItem = PodDBAdapter.KEY_FEEDITEM;
        String tableQueue = PodDBAdapter.TABLE_NAME_QUEUE;
        String tableFavorites = PodDBAdapter.TABLE_NAME_FAVORITES;

        List<String> statements = new ArrayList<>();
        if (showPlayed)        statements.add(keyRead + " = 1 ");
        if (showUnplayed)      statements.add(" NOT " + keyRead + " = 1 "); // Match "New" items (read = -1) as well
        if (showPaused)        statements.add(" (" + keyPosition + " NOT NULL AND " + keyPosition + " > 0 " + ") ");
        if (showNotPaused)     statements.add(" (" + keyPosition + " IS NULL OR " + keyPosition + " = 0 " + ") ");
        if (showQueued)        statements.add(keyItemId + " IN (SELECT " + keyFeedItem + " FROM " + tableQueue + ") ");
        if (showNotQueued)     statements.add(keyItemId + " NOT IN (SELECT " + keyFeedItem + " FROM " + tableQueue + ") ");
        if (showDownloaded)    statements.add(keyDownloaded + " = 1 ");
        if (showNotDownloaded) statements.add(keyDownloaded + " = 0 ");
        if (showHasMedia)      statements.add(keyMediaId + " NOT NULL ");
        if (showNoMedia)       statements.add(keyMediaId + " IS NULL ");
        if (showIsFavorite)    statements.add(keyItemId + " IN (SELECT " + keyFeedItem + " FROM " + tableFavorites + ") ");
        if (showNotFavorite)   statements.add(keyItemId + " NOT IN (SELECT " + keyFeedItem + " FROM " + tableFavorites + ") ");

        if (statements.isEmpty()) {
            return "";
        }
        StringBuilder query = new StringBuilder(" (" + statements.get(0));
        for (String r : statements.subList(1, statements.size())) {
            query.append(" AND ");
            query.append(r);
        }
        query.append(") ");
        return query.toString();
    }

    /**
     * Run a list of feed items through the filter.
     */
    public List<FeedItem> filter(List<FeedItem> items) {
        if(mProperties.length == 0) return items;

        List<FeedItem> result = new ArrayList<>();

        // Check for filter combinations that will always return an empty list
        // (e.g. requiring played and unplayed at the same time)
        if (showPlayed && showUnplayed) return result;
        if (showQueued && showNotQueued) return result;
        if (showDownloaded && showNotDownloaded) return result;

        final LongList queuedIds = DBReader.getQueueIDList();
        for (FeedItem item : items) {
            // If the item does not meet a requirement, skip it.

            if (showPlayed && !item.isPlayed()) continue;
            if (showUnplayed && item.isPlayed()) continue;

            if (showPaused && item.getState() != FeedItem.State.IN_PROGRESS) continue;
            if (showNotPaused && item.getState() == FeedItem.State.IN_PROGRESS) continue;

            boolean queued = queuedIds.contains(item.getId());
            if (showQueued && !queued) continue;
            if (showNotQueued && queued) continue;

            boolean downloaded = item.getMedia() != null && item.getMedia().isDownloaded();
            if (showDownloaded && !downloaded) continue;
            if (showNotDownloaded && downloaded) continue;

            if (showHasMedia && !item.hasMedia()) continue;
            if (showNoMedia && item.hasMedia()) continue;

            if (showIsFavorite && !item.isTagged(TAG_FAVORITE)) continue;
            if (showNotFavorite && item.isTagged(TAG_FAVORITE)) continue;

            // If the item reaches here, it meets all criteria
            result.add(item);
        }

        return result;
    }

    /**
     * Express this filter using an SQL boolean statement that can be inserted into an SQL WHERE clause
     * to yield output filtered according to the rules of this filter.
     *
     * @return An SQL boolean statement that matches the desired items,
     *         empty string if there is nothing to filter
     */
    public String getQuery() {
        return mQuery;
    }

    public String[] getValues() {
        return mProperties.clone();
    }

    public boolean isShowDownloaded() {
        return showDownloaded;
    }

}
