package de.danoeh.antennapod.core.feed;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBReader;

public class FeedItemFilter {

    private final String[] filter;

    private boolean hideUnplayed = false;
    private boolean hidePaused = false;
    private boolean hidePlayed = false;
    private boolean hideQueued = false;
    private boolean hideNotQueued = false;
    private boolean hideDownloaded = false;
    private boolean hideNotDownloaded = false;

    public FeedItemFilter(String filter) {
        this(StringUtils.split(filter, ','));
    }

    public FeedItemFilter(String[] filter) {
        this.filter = filter;
        for(String f : filter) {
            // see R.arrays.feed_filter_values
            switch(f) {
                case "unplayed":
                    hideUnplayed = true;
                    break;
                case "paused":
                    hidePaused = true;
                    break;
                case "played":
                    hidePlayed = true;
                    break;
                case "queued":
                    hideQueued = true;
                    break;
                case "not_queued":
                    hideNotQueued = true;
                    break;
                case "downloaded":
                    hideDownloaded = true;
                    break;
                case "not_downloaded":
                    hideNotDownloaded = true;
                    break;
            }
        }
    }

    public List<FeedItem> filter(Context context, List<FeedItem> items) {
        if(filter.length == 0) {
            return items;
        }
        List<FeedItem> result = new ArrayList<FeedItem>();
        for(FeedItem item : items) {
            if(hideUnplayed && false == item.isRead()) continue;
            if(hidePaused && item.getState() == FeedItem.State.IN_PROGRESS) continue;
            if(hidePlayed && item.isRead()) continue;
            boolean isQueued = DBReader.getQueueIDList(context).contains(item.getId());
            if(hideQueued && isQueued) continue;
            if(hideNotQueued && false == isQueued) continue;
            boolean isDownloaded = item.getMedia() != null && item.getMedia().isDownloaded();
            if(hideDownloaded && isDownloaded) continue;
            if(hideNotDownloaded && false == isDownloaded) continue;
            result.add(item);
        }
        return result;
    }

    public String[] getValues() {
        return filter.clone();
    }

}
