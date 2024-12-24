package de.danoeh.antennapod.ui.share

import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.common.queryString

/**
 * Builds a shareable link for this feed item, including media URL, title, GUID, and publication date.
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
