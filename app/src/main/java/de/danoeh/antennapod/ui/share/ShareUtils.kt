@file:JvmName("ShareUtilsKt")

package de.danoeh.antennapod.ui.share

import android.content.Context
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.common.UriUtil
import de.danoeh.antennapod.net.common.queryString
import de.danoeh.antennapod.net.common.urlEncode

/**
 * Shares an individual feed item. To share a feed, use [ShareUtils.shareFeedLink].
 */
fun shareFeedItemLink(context: Context, item: FeedItem) {
    val itemUrl = urlEncode(item.link)
    val query = queryString(mapOf(
        "url" to itemUrl,
        "title" to item.title,
    ))
    val text = "https://antennapod.org/deeplink/episode?$query"
    ShareUtils.shareLink(context, text)
}
