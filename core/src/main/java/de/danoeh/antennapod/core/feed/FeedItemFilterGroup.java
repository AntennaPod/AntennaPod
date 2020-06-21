package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.core.R;

public class FeedItemFilterGroup {

    private static boolean DEFAULT_SELECTED_STATE = false;

    private static final String NO_FILTERID = "";
    private static final int HASMEDIA_LABEL = R.string.has_media;
    private static final int NOTPAUSED_LABEL = R.string.not_paused;
    private static final int NOTFAVORITE_LABEL = R.string.not_favorite;

    private static final int UNPLAYED_LABEL = R.string.not_played;
    private static final String UNPLAYED_FILTERID = "unplayed";

    private static final int PLAYED_LABEL = R.string.hide_played_episodes_label;
    private static final String PLAYED_FILTERID = "played";

    private static final int PAUSED_LABEL = R.string.hide_paused_episodes_label;
    private static final String PAUSED_FILTERID = "paused";

    private static final int ISFAVORITE_LABEL = R.string.hide_is_favorite_label;
    private static final String ISFAVORITE_FILTERID = "is_favorite";

    private static final int NOMEDIA_LABEL = R.string.no_media;
    private static final String NOMEDIA_FILTERID = "no_media";

    private static final int QUEUED_LABEL = R.string.queue_label;
    private static final String QUEUED_FILTERID = "queued";

    private static final int NOTQUEUED_LABEL = R.string.not_queued_label;
    private static final String NOTQUEUED_FILTERID = "not_queued";

    private static final int NOTDOWNLOADED_LABEL = R.string.hide_downloaded_episodes_label;
    private static final String NOTDOWNLOADED_FILTERID = "not_downloaded";

    private static final int DOWNLOADED_LABEL = R.string.hide_downloaded_episodes_label;
    private static final String DOWNLOADED_FILTERID = "downloaded";

    public enum FeedItemEnum {

        PLAYED(new ItemProperties(DEFAULT_SELECTED_STATE, UNPLAYED_LABEL, UNPLAYED_FILTERID),
                new ItemProperties(DEFAULT_SELECTED_STATE, PLAYED_LABEL, PLAYED_FILTERID)),
        PAUSED(new ItemProperties(DEFAULT_SELECTED_STATE, NOTPAUSED_LABEL, NO_FILTERID),
                new ItemProperties(DEFAULT_SELECTED_STATE, PAUSED_LABEL, PAUSED_FILTERID)),
        FAVORITE(new ItemProperties(DEFAULT_SELECTED_STATE, NOTFAVORITE_LABEL, NO_FILTERID),
                new ItemProperties(DEFAULT_SELECTED_STATE, ISFAVORITE_LABEL, ISFAVORITE_FILTERID)),
        MEDIA(new ItemProperties(DEFAULT_SELECTED_STATE, NOMEDIA_LABEL, NOMEDIA_FILTERID),
                new ItemProperties(DEFAULT_SELECTED_STATE, HASMEDIA_LABEL, NO_FILTERID)),
        QUEUED(new ItemProperties(DEFAULT_SELECTED_STATE, NOTQUEUED_LABEL, NOTQUEUED_FILTERID),
                new ItemProperties(DEFAULT_SELECTED_STATE, QUEUED_LABEL, QUEUED_FILTERID)),
        DOWNLOADED(new ItemProperties(DEFAULT_SELECTED_STATE, NOTDOWNLOADED_LABEL, NOTDOWNLOADED_FILTERID),
                new ItemProperties(DEFAULT_SELECTED_STATE, DOWNLOADED_LABEL, DOWNLOADED_FILTERID));

        public final ItemProperties[] values;

        FeedItemEnum(ItemProperties... values) {
            this.values = values;
        }

        public static class ItemProperties {

            public final int displayName;
            public boolean selected;
            public final String filterId;

            public void setSelected(boolean value) {
                this.selected = value;
            }

            public boolean getSelected() {
                return this.selected;
            }

            public ItemProperties(boolean selected, int displayName, String filterId) {
                this.selected = selected;
                this.displayName = displayName;
                this.filterId = filterId;
            }

        }
    }
}
