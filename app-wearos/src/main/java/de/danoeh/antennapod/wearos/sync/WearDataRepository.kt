package de.danoeh.antennapod.wearos.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import de.danoeh.antennapod.net.sync.wearinterface.WearNowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Singleton holding the last data received from the phone for each data path.
 * Written by WearDataListenerService (even when no activity is running) and
 * read by the screen composables.
 */
object WearDataRepository {
    private val _episodesByPath = MutableStateFlow<Map<String, List<FeedItem>>>(emptyMap())
    val episodesByPath: StateFlow<Map<String, List<FeedItem>>> = _episodesByPath

    private val _feedsByPath = MutableStateFlow<Map<String, List<Feed>>>(emptyMap())
    val feedsByPath: StateFlow<Map<String, List<Feed>>> = _feedsByPath

    private val _nowPlaying = MutableStateFlow<WearNowPlaying?>(null)
    val nowPlaying: StateFlow<WearNowPlaying?> = _nowPlaying

    fun updateNowPlaying(nowPlaying: WearNowPlaying?) {
        _nowPlaying.value = nowPlaying
    }

    fun updateEpisodes(path: String, items: List<FeedItem>) {
        _episodesByPath.value += (path to items)
    }

    fun updateFeeds(path: String, feeds: List<Feed>) {
        _feedsByPath.value += (path to feeds)
    }

    fun requestDataFromPhone(context: Context, scope: CoroutineScope, path: String, tag: String) {
        scope.launch(Dispatchers.IO) {
            val supported = WearConnectionUtils.isPhoneSupported(context)
            if (!supported) {
                return@launch
            }
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                val node = nodes.firstOrNull() ?: run {
                    Log.w(tag, "No connected nodes")
                    return@launch
                }
                Wearable.getMessageClient(context).sendMessage(node.id, path, null).await()
            } catch (e: Exception) {
                Log.e(tag, "Failed to send message to phone", e)
            }
        }
    }
}
