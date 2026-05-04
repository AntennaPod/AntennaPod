package de.danoeh.antennapod.wearos

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.ui.common.R as CommonR

object NavigationNames {
    @StringRes
    fun getTitleResForPath(path: String): Int = when (path) {
        WearDataPaths.QUEUE -> CommonR.string.queue_label
        WearDataPaths.DOWNLOADS -> CommonR.string.downloads_label
        WearDataPaths.EPISODES -> CommonR.string.episodes_label
        WearDataPaths.SUBSCRIPTIONS -> CommonR.string.subscriptions_label
        else -> if (path.startsWith(WearDataPaths.FEED_EPISODES_PREFIX)) {
            CommonR.string.episodes_label
        } else {
            CommonR.string.app_name
        }
    }

    @DrawableRes
    fun getIconResForPath(path: String): Int = when (path) {
        WearDataPaths.QUEUE -> CommonR.drawable.ic_playlist_play_black
        WearDataPaths.DOWNLOADS -> CommonR.drawable.ic_download_black
        WearDataPaths.EPISODES -> CommonR.drawable.ic_feed_black
        WearDataPaths.SUBSCRIPTIONS -> CommonR.drawable.ic_subscriptions_black
        else -> CommonR.mipmap.ic_launcher
    }
}
