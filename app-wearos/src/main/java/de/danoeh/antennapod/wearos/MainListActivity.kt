package de.danoeh.antennapod.wearos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.Wearable
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.sync.WearDataPathUtils
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainListScreen(
                context = this,
                onRequestNowPlaying = { requestNowPlaying() }
            )
        }
    }

    private fun requestNowPlaying() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull() ?: return@addOnSuccessListener
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, WearDataPaths.NOW_PLAYING, null)
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to request now playing", e) }
            }
    }

    companion object {
        private const val TAG = "MainListActivity"
    }
}

@Composable
fun MainListScreen(context: MainListActivity, onRequestNowPlaying: () -> Unit) {
    val scrollState = rememberScalingLazyListState()
    val versionName = BuildConfig.VERSION_NAME
    var connectedPhone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        connectedPhone = withContext(Dispatchers.Default) {
            WearConnectionUtils.getConnectedNodeName(context)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            onRequestNowPlaying()
            delay(2000)
        }
    }

    val nowPlaying by WearDataRepository.nowPlaying.collectAsState()

    val menuItems = listOf(
        WearDataPaths.QUEUE,
        WearDataPaths.DOWNLOADS,
        WearDataPaths.EPISODES,
        WearDataPaths.SUBSCRIPTIONS
    )

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
                if (nowPlaying != null) {
                    ListItem(
                        text = nowPlaying!!.item.title ?: "",
                        onClick = {
                            val intent = Intent(context, EpisodeDetailActivity::class.java).apply {
                                putExtra(EpisodeDetailActivity.EXTRA_EPISODE, nowPlaying!!.item)
                            }
                            context.startActivity(intent)
                        }
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
                    text = stringResource(WearDataPathUtils.getTitleResForPath(path)),
                    iconRes = WearDataPathUtils.getIconResForPath(path),
                    onClick = {
                        val intent = if (path == WearDataPaths.SUBSCRIPTIONS) {
                            Intent(context, FeedListActivity::class.java)
                        } else {
                            Intent(context, EpisodeListActivity::class.java).apply {
                                putExtra(EpisodeListActivity.EXTRA_PATH, path)
                            }
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                val connectionText = if (connectedPhone.isNotEmpty()) {
                    stringResource(CommonR.string.wearos_connected_to, connectedPhone) + "\n"
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
