package de.danoeh.antennapod.wearos.sync

import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.sync.wearinterface.WearNowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
}
