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
import androidx.compose.runtime.LaunchedEffect
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
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.sync.WearDataPathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainListScreen(context = this)
        }
    }
}

@Composable
fun MainListScreen(context: MainListActivity) {
    val scrollState = rememberScalingLazyListState()
    val versionName = BuildConfig.VERSION_NAME
    var connectedPhone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        connectedPhone = withContext(Dispatchers.Default) {
            WearConnectionUtils.getConnectedNodeName(context)
        }
    }

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
