package de.danoeh.antennapod.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.ui.common.Converter
import de.danoeh.antennapod.ui.common.DateFormatter
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.wearos.composable.ListItem

class EpisodeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val episode = intent.getSerializableExtra(EXTRA_EPISODE) as? FeedItem ?: return
        val viewModel = ViewModelProvider(this, EpisodeDetailViewModel.factory(episode))
            .get(EpisodeDetailViewModel::class.java)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            EpisodeDetailScreen(
                uiState = uiState,
                onPlay = { viewModel.play() },
                onPause = { viewModel.pause() },
                onOpenOnPhone = { viewModel.openOnPhone() }
            )
        }
    }

    companion object {
        const val EXTRA_EPISODE = "episode"
    }
}

@Composable
fun EpisodeDetailScreen(
    uiState: EpisodeDetailUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onOpenOnPhone: () -> Unit
) {
    val item = uiState.item ?: return
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

        if (uiState.duration > 0) {
            item {
                LinearProgressIndicator(
                    progress = { uiState.position.toFloat() / uiState.duration.toFloat() },
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
                        text = Converter.getDurationStringLong(uiState.position),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = Converter.getDurationStringLong(uiState.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            if (uiState.isCurrentlyPlaying) {
                ListItem(
                    text = stringResource(CommonR.string.pause_label),
                    iconRes = CommonR.drawable.ic_pause_black,
                    onClick = onPause
                )
            } else {
                ListItem(
                    text = stringResource(CommonR.string.wearos_play_on_phone),
                    iconRes = CommonR.drawable.ic_play_48dp_black,
                    onClick = onPlay
                )
            }
        }

        item {
            ListItem(
                text = stringResource(CommonR.string.wearos_open_on_phone),
                iconRes = CommonR.drawable.ic_phone_black,
                onClick = onOpenOnPhone
            )
        }
    }
}
