package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.feed.FeedItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ForYouBlender {

    static List<FeedItem> blend(List<FeedItem> inProgress, List<FeedItem> fresh) {
        List<FeedItem> episodes = new ArrayList<>(inProgress);
        Set<Long> seen = new HashSet<>();
        for (FeedItem item : episodes) {
            seen.add(item.getId());
        }
        for (FeedItem item : fresh) {
            if (seen.add(item.getId())) {
                episodes.add(item);
            }
        }
        return episodes;
    }
}
