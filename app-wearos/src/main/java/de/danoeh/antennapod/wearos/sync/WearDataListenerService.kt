package de.danoeh.antennapod.wearos.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.net.sync.wearinterface.WearSerializer

class WearDataListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        when (path) {
            WearDataPaths.SUBSCRIPTIONS -> {
                val feeds = WearSerializer.feedsFromBytes(event.data)
                Log.d(TAG, "Received $path: ${feeds.size} feeds")
                WearDataRepository.updateFeeds(path, feeds)
            }
            WearDataPaths.NOW_PLAYING ->
                WearDataRepository.updateNowPlaying(WearSerializer.nowPlayingFromBytes(event.data))
            WearDataPaths.QUEUE, WearDataPaths.DOWNLOADS, WearDataPaths.EPISODES -> {
                val items = WearSerializer.episodesFromBytes(event.data)
                Log.d(TAG, "Received $path: ${items.size} episodes")
                WearDataRepository.updateEpisodes(path, items)
            }
            else -> if (path.startsWith(WearDataPaths.FEED_EPISODES_PREFIX)) {
                val items = WearSerializer.episodesFromBytes(event.data)
                Log.d(TAG, "Received $path: ${items.size} episodes")
                WearDataRepository.updateEpisodes(path, items)
            }
        }
    }

    companion object {
        private const val TAG = "WearDataListenerService"
    }
}
