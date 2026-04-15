package de.danoeh.antennapod.wearos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.Wearable
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.ui.common.Converter
import de.danoeh.antennapod.ui.common.DateFormatter
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.wearos.composable.ListItem

class EpisodeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val episode = intent.getSerializableExtra(EXTRA_EPISODE) as? FeedItem ?: return

        setContent {
            EpisodeDetailScreen(
                item = episode,
                onPlay = { playEpisode(episode.id) }
            )
        }
    }

    private fun playEpisode(itemId: Long) {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull() ?: return@addOnSuccessListener
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, WearDataPaths.playPath(itemId), null)
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to send play request", e) }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to get connected nodes", e) }
    }

    companion object {
        const val EXTRA_EPISODE = "episode"
        private const val TAG = "EpisodeDetailActivity"
    }
}

@Composable
fun EpisodeDetailScreen(item: FeedItem, onPlay: () -> Unit) {
    val scrollState = rememberScalingLazyListState()
    var titleExpanded by remember { mutableStateOf(false) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            val dateStr = DateFormatter.formatAbbrev(LocalContext.current, item.getPubDate())
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { titleExpanded = !titleExpanded },
                style = MaterialTheme.typography.titleMedium.copy(
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Paragraph
                ),
                textAlign = TextAlign.Center,
                maxLines = if (titleExpanded) Int.MAX_VALUE else 3,
                overflow = if (titleExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )
        }

        if (item.media!!.duration > 0) {
            item {
                LinearProgressIndicator(
                    progress = { item.media!!.position.toFloat() / item.media!!.duration.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Converter.getDurationStringLong(item.media!!.position),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = Converter.getDurationStringLong(item.media!!.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            ListItem(
                text = stringResource(CommonR.string.wearos_play_on_phone),
                iconRes = CommonR.drawable.ic_play_48dp_black,
                onClick = onPlay
            )
        }
    }
}
