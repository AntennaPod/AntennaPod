package de.danoeh.antennapod.wearos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.wearos.composable.ListItem

class MainListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MainListScreen(
                uiState = uiState,
                onOpenEpisodeDetail = { episode ->
                    val intent = Intent(this, EpisodeDetailActivity::class.java).apply {
                        putExtra(EpisodeDetailActivity.EXTRA_EPISODE, episode)
                    }
                    startActivity(intent)
                },
                onOpenFeedList = {
                    startActivity(Intent(this, FeedListActivity::class.java))
                },
                onOpenEpisodeList = { path ->
                    val intent = Intent(this, EpisodeListActivity::class.java).apply {
                        putExtra(EpisodeListActivity.EXTRA_PATH, path)
                    }
                    startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun MainListScreen(
    uiState: MainUiState,
    onOpenEpisodeDetail: (FeedItem) -> Unit,
    onOpenFeedList: () -> Unit,
    onOpenEpisodeList: (String) -> Unit
) {
    val scrollState = rememberScalingLazyListState()
    val versionName = BuildConfig.VERSION_NAME

    val menuItems = remember {
        listOf(
            WearDataPaths.QUEUE,
            WearDataPaths.DOWNLOADS,
            WearDataPaths.EPISODES,
            WearDataPaths.SUBSCRIPTIONS
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = scrollState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.app_name))
                }
            }

            item {
                if (uiState.nowPlaying != null) {
                    ListItem(
                        text = uiState.nowPlaying.item.title ?: "",
                        onClick = { onOpenEpisodeDetail(uiState.nowPlaying.item) }
                    )
                } else {
                    ListItem(
                        text = stringResource(CommonR.string.no_media_playing_label),
                        onClick = {}
                    )
                }
            }

            item {
                Text(
                    text = stringResource(CommonR.string.wearos_browse_phone),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(menuItems) { path ->
                ListItem(
                    text = stringResource(NavigationNames.getTitleResForPath(path)),
                    iconRes = NavigationNames.getIconResForPath(path),
                    onClick = {
                        if (path == WearDataPaths.SUBSCRIPTIONS) {
                            onOpenFeedList()
                        } else {
                            onOpenEpisodeList(path)
                        }
                    }
                )
            }

            item {
                val connectionText = if (uiState.connectedPhone.isNotEmpty()) {
                    stringResource(CommonR.string.wearos_connected_to, uiState.connectedPhone) + "\n"
                } else {
                    ""
                }
                Text(
                    text = connectionText + stringResource(CommonR.string.wearos_version, versionName),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        ScrollIndicator(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
