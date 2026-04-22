package de.danoeh.antennapod.wearos.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.net.sync.wearinterface.WearSerializer

class WearDataListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        if (path == WearDataPaths.SUBSCRIPTIONS) {
            val feeds = WearSerializer.feedsFromBytes(event.data)
            Log.d(TAG, "Received $path: ${feeds.size} feeds")
            WearDataRepository.updateFeeds(path, feeds)
        } else if (path == WearDataPaths.NOW_PLAYING) {
            WearDataRepository.updateNowPlaying(WearSerializer.nowPlayingFromBytes(event.data))
        } else if (path == WearDataPaths.QUEUE || path == WearDataPaths.DOWNLOADS ||
            path == WearDataPaths.EPISODES || path.startsWith(WearDataPaths.FEED_EPISODES_PREFIX)
        ) {
            val items = WearSerializer.episodesFromBytes(event.data)
            Log.d(TAG, "Received $path: ${items.size} episodes")
            WearDataRepository.updateEpisodes(path, items)
        }
    }

    companion object {
        private const val TAG = "WearDataListenerService"
    }
}
