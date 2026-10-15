package de.danoeh.antennapod.net.discovery;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.i18n.R;
import java.util.ArrayList;
import java.util.List;

public class SearchFeedBuilder {

    public static Feed buildFeed(Context context, String query, List<EpisodeSearchResult> episodeResults) {
        Feed feed = new Feed(Feed.PREFIX_SEARCH_FEED + query, null,
                context.getString(R.string.search_feed_title, query));
        feed.setState(Feed.STATE_SUBSCRIBED);
        feed.setDescription(context.getString(R.string.search_feed_description, query));
        feed.setAuthor("AntennaPod Search");
        feed.setImageUrl(Feed.PREFIX_SEARCH_COVER + query);

        List<FeedItem> items = new ArrayList<>();
        if (episodeResults != null) {
            for (EpisodeSearchResult episode : episodeResults) {
                items.add(createFeedItem(feed, episode));
            }
        }
        feed.setItems(items);
        return feed;
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
}
