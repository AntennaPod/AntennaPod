package de.danoeh.antennapod.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.ui.common.Converter
import de.danoeh.antennapod.ui.common.DateFormatter
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.ui.notifications.R as NotificationsR
import de.danoeh.antennapod.wearos.composable.ListItem

class EpisodeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val episode = IntentCompat.getSerializableExtra(intent, EXTRA_EPISODE, FeedItem::class.java) ?: run {
            finish()
            return
        }
        val viewModel = ViewModelProvider(this, EpisodeDetailViewModel.factory(episode))
            .get(EpisodeDetailViewModel::class.java)

        setContent {
            MaterialTheme(colorScheme = dynamicColorScheme(this) ?: MaterialTheme.colorScheme) {
                val uiState by viewModel.uiState.collectAsState()
                EpisodeDetailScreen(
                    uiState = uiState,
                    onPlay = { viewModel.play() },
                    onPause = { viewModel.pause() },
                    onSkipForward = { viewModel.skipForward() },
                    onSkipBackward = { viewModel.skipBackward() },
                    onOpenOnPhone = { viewModel.openOnPhone() }
                )
            }
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
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onOpenOnPhone: () -> Unit
) {
    val item = uiState.item
    val scrollState = rememberScalingLazyListState()
    var titleExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = scrollState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
            autoCentering = null
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
                    maxLines = if (titleExpanded) Int.MAX_VALUE else 2,
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
                            .padding(
                                start = 8.dp,
                                top = 2.dp,
                                end = 8.dp,
                                bottom = if (uiState.hasStartedPlaying) 8.dp else 16.dp
                            ),
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
                if (uiState.hasStartedPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        FilledTonalIconButton(onClick = onSkipBackward, modifier = Modifier.size(48.dp)) {
                            Icon(
                                painter = painterResource(NotificationsR.drawable.ic_notification_fast_rewind),
                                contentDescription = stringResource(CommonR.string.rewind_label),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        val playIcon = if (uiState.isCurrentlyPlaying) {
                            CommonR.drawable.ic_pause_black
                        } else {
                            CommonR.drawable.ic_play_48dp_black
                        }
                        val playLabel = if (uiState.isCurrentlyPlaying) {
                            CommonR.string.pause_label
                        } else {
                            CommonR.string.play_label
                        }
                        FilledIconButton(
                            onClick = if (uiState.isCurrentlyPlaying) onPause else onPlay,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(playIcon),
                                contentDescription = stringResource(playLabel),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        FilledTonalIconButton(onClick = onSkipForward, modifier = Modifier.size(48.dp)) {
                            Icon(
                                painter = painterResource(NotificationsR.drawable.ic_notification_fast_forward),
                                contentDescription = stringResource(CommonR.string.fast_forward_label),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
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

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        ScrollIndicator(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
