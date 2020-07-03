package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.core.R;

public enum FeedItemFilterGroup {
    PLAYED(new ItemProperties(R.string.hide_played_episodes_label, "played"),
            new ItemProperties(R.string.not_played, "unplayed")),
    PAUSED(new ItemProperties(R.string.hide_paused_episodes_label, "paused"),
            new ItemProperties(R.string.not_paused, "not_paused")),
    FAVORITE(new ItemProperties(R.string.hide_is_favorite_label, "is_favorite"),
            new ItemProperties(R.string.not_favorite, "not_favorite")),
    MEDIA(new ItemProperties(R.string.has_media, "has_media"),
            new ItemProperties(R.string.no_media, "no_media")),
    QUEUED(new ItemProperties(R.string.queued_label, "queued"),
            new ItemProperties(R.string.not_queued_label, "not_queued")),
    DOWNLOADED(new ItemProperties(R.string.hide_downloaded_episodes_label, "downloaded"),
            new ItemProperties(R.string.hide_not_downloaded_episodes_label, "not_downloaded"));

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
