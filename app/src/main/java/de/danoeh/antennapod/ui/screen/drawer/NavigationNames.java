package de.danoeh.antennapod.ui.screen.drawer;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.screen.AddFeedFragment;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.InboxFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.screen.home.HomeFragment;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionFragment;

public abstract class NavigationNames {
    public static @DrawableRes int getDrawable(String tag) {
        switch (tag) {
            case QueueFragment.TAG:
                return R.drawable.ic_playlist_play;
            case InboxFragment.TAG:
                return R.drawable.ic_inbox;
            case AllEpisodesFragment.TAG:
                return R.drawable.ic_feed;
            case CompletedDownloadsFragment.TAG:
                return R.drawable.ic_download;
            case PlaybackHistoryFragment.TAG:
                return R.drawable.ic_history;
            case SubscriptionFragment.TAG:
                return R.drawable.ic_subscriptions;
            case AddFeedFragment.TAG:
                return R.drawable.ic_add;
            case HomeFragment.TAG:
            default:
                return R.drawable.ic_home;
        }
    }

    public static @StringRes int getLabel(String tag) {
        switch (tag) {
            case QueueFragment.TAG:
                return R.string.queue_label;
            case InboxFragment.TAG:
                return R.string.inbox_label;
            case AllEpisodesFragment.TAG:
                return R.string.episodes_label;
            case CompletedDownloadsFragment.TAG:
                return R.string.downloads_label;
            case PlaybackHistoryFragment.TAG:
                return R.string.playback_history_label;
            case SubscriptionFragment.TAG:
                return R.string.subscriptions_label;
            case AddFeedFragment.TAG:
                return R.string.add_feed_label;
            case HomeFragment.TAG:
            default:
                return R.string.home_label;
        }
    }
}
