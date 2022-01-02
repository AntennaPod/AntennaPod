package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public enum FeedItemFilterGroup {
    PLAYED(new ItemProperties(R.string.hide_played_episodes_label, FeedItemFilter.PLAYED),
            new ItemProperties(R.string.not_played, FeedItemFilter.UNPLAYED)),
    PAUSED(new ItemProperties(R.string.hide_paused_episodes_label, FeedItemFilter.PAUSED),
            new ItemProperties(R.string.not_paused, FeedItemFilter.NOT_PAUSED)),
    FAVORITE(new ItemProperties(R.string.hide_is_favorite_label, FeedItemFilter.IS_FAVORITE),
            new ItemProperties(R.string.not_favorite, FeedItemFilter.NOT_FAVORITE)),
    MEDIA(new ItemProperties(R.string.has_media, FeedItemFilter.HAS_MEDIA),
            new ItemProperties(R.string.no_media, FeedItemFilter.NO_MEDIA)),
    QUEUED(new ItemProperties(R.string.queued_label, FeedItemFilter.QUEUED),
            new ItemProperties(R.string.not_queued_label, FeedItemFilter.NOT_QUEUED)),
    DOWNLOADED(new ItemProperties(R.string.hide_downloaded_episodes_label, FeedItemFilter.DOWNLOADED),
            new ItemProperties(R.string.hide_not_downloaded_episodes_label, FeedItemFilter.NOT_DOWNLOADED));

    public final ItemProperties[] values;

    FeedItemFilterGroup(ItemProperties... values) {
        this.values = values;
    }

    public static class ItemProperties {

        public final int displayName;
        public final String filterId;

        public ItemProperties(int displayName, String filterId) {
            this.displayName = displayName;
            this.filterId = filterId;
        }

    }
}
