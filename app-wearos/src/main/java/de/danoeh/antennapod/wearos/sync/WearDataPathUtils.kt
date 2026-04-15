package de.danoeh.antennapod.wearos.sync

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.ui.common.R as CommonR

object WearDataPathUtils {
    @StringRes
    fun getTitleResForPath(path: String): Int = when {
        path == WearDataPaths.QUEUE -> CommonR.string.queue_label
        path == WearDataPaths.DOWNLOADS -> CommonR.string.downloads_label
        path == WearDataPaths.EPISODES -> CommonR.string.episodes_label
        path == WearDataPaths.SUBSCRIPTIONS -> CommonR.string.subscriptions_label
        path.startsWith(WearDataPaths.FEED_EPISODES_PREFIX) -> CommonR.string.episodes_label
        else -> CommonR.string.app_name
    }

    @DrawableRes
    fun getIconResForPath(path: String): Int = when {
        path == WearDataPaths.QUEUE -> CommonR.drawable.ic_playlist_play_black
        path == WearDataPaths.DOWNLOADS -> CommonR.drawable.ic_download_black
        path == WearDataPaths.EPISODES -> CommonR.drawable.ic_feed_black
        path == WearDataPaths.SUBSCRIPTIONS -> CommonR.drawable.ic_subscriptions_black
        else -> CommonR.mipmap.ic_launcher
    }
}
