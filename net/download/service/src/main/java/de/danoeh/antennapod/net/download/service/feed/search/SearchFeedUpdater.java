package de.danoeh.antennapod.net.download.service.feed.search;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.discovery.EpisodeSearchResult;
import de.danoeh.antennapod.net.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.ui.i18n.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchFeedUpdater {
    private static final String TAG = "SearchFeedUpdater";

    public static Feed updateFeed(Feed feed, Context context) {
        try {
            String query = getQuery(feed);
            if (TextUtils.isEmpty(query)) {
                throw new IOException("Search feed has empty query");
            }

            ItunesPodcastSearcher searcher = new ItunesPodcastSearcher();
            List<EpisodeSearchResult> episodeResults = searcher.searchEpisodes(query).blockingGet();
            if (episodeResults == null) {
                episodeResults = new ArrayList<>();
            }

            Feed updatedFeed = tryUpdateFeed(feed, context, query, episodeResults);

            List<DownloadResult> downloadResults = DBReader.getFeedDownloadLog(feed.getId(), 1);
            if (downloadResults.isEmpty() || !downloadResults.get(0).isSuccessful()) {
                reportSuccess(feed);
            }
            return updatedFeed;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update search feed", e);
            reportError(feed, e.getMessage());
        }
        return null;
    }

    public static String getQuery(Feed feed) {
        if (feed == null || feed.getDownloadUrl() == null) {
            return "";
        }
        return feed.getDownloadUrl().replace(Feed.PREFIX_SEARCH_FEED, "");
    }

    @VisibleForTesting
    public static Feed tryUpdateFeed(Feed feed, Context context, String query,
                                     List<EpisodeSearchResult> episodeResults) {
        if (feed.getItems() == null) {
            feed.setItems(new ArrayList<>());
        }

        Feed newFeed = new Feed(feed.getDownloadUrl(), null,
                context.getString(R.string.search_feed_title, query));
        newFeed.setId(feed.getId());
        newFeed.setState(feed.getState());
        newFeed.setDescription(context.getString(R.string.search_feed_description, query));
        newFeed.setAuthor("AntennaPod Search");

        newFeed.setImageUrl(Feed.PREFIX_SEARCH_COVER + query);

        List<FeedItem> items = new ArrayList<>();
        for (EpisodeSearchResult episode : episodeResults) {
            FeedItem item = createFeedItem(newFeed, episode);
            items.add(item);
        }
        newFeed.setItems(items);

        return FeedDatabaseWriter.updateFeed(context, newFeed, false);
    }

    @NonNull
    public static FeedItem createFeedItem(Feed feed, EpisodeSearchResult episode) {
        String identifier = episode.episodeGuid != null ? episode.episodeGuid : episode.episodeUrl;
        FeedItem item = new FeedItem(0, episode.title, identifier,
                episode.episodeUrl, episode.pubDate, FeedItem.NEW, feed);
        if (episode.description != null) {
            item.setDescriptionIfLonger(episode.description);
        }
        if (episode.imageUrl != null) {
            item.setImageUrl(episode.imageUrl);
        }
        FeedMedia media = new FeedMedia(item, episode.episodeUrl, 0, "audio/*");
        media.setDuration((int) episode.durationMs);
        item.setMedia(media);
        return item;
    }

    private static void reportError(Feed feed, String reasonDetailed) {
        DownloadResult status = new DownloadResult(feed.getTitle(), feed.getId(),
                Feed.FEEDFILETYPE_FEED, false, DownloadError.ERROR_IO_ERROR, reasonDetailed);
        DBWriter.addDownloadStatus(status);
        DBWriter.setFeedLastUpdateFailed(feed.getId(), true);
    }

    private static void reportSuccess(Feed feed) {
        DownloadResult status = new DownloadResult(feed.getTitle(), feed.getId(),
                Feed.FEEDFILETYPE_FEED, true, DownloadError.SUCCESS, null);
        DBWriter.addDownloadStatus(status);
        DBWriter.setFeedLastUpdateFailed(feed.getId(), false);
    }
}
