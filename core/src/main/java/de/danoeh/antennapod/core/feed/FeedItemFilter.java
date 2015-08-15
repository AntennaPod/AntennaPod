package de.danoeh.antennapod.core.feed;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBReader;

public class FeedItemFilter {

    private final String[] properties;

    private boolean hideUnplayed = false;
    private boolean hidePaused = false;
    private boolean hidePlayed = false;
    private boolean hideQueued = false;
    private boolean hideNotQueued = false;
    private boolean hideDownloaded = false;
    private boolean hideNotDownloaded = false;

    public FeedItemFilter(String properties) {
        this(StringUtils.split(properties, ','));
    }

    public FeedItemFilter(String[] properties) {
        this.properties = properties;
        for(String property : properties) {
            // see R.arrays.feed_filter_values
            switch(property) {
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
        if(properties.length == 0) {
            return items;
        }
        List<FeedItem> result = new ArrayList<FeedItem>();
        for(FeedItem item : items) {
            if(hideUnplayed && false == item.isPlayed()) continue;
            if(hidePaused && item.getState() == FeedItem.State.IN_PROGRESS) continue;
            if(hidePlayed && item.isPlayed()) continue;
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
        return properties.clone();
    }

}
