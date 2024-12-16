package de.danoeh.antennapod.ui.share

import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.common.queryString

/**
 *
 */
fun FeedItem.getShareLink(): String {
    val query = queryString(mapOf(
        "url" to (media?.downloadUrl ?: ""),
        "eTitle" to title,
        "eGuid" to id.toString(),
        "eDate" to pubDate.time.toString()
    ))
    return "https://antennapod.org/deeplink/episode/?$query"
}
