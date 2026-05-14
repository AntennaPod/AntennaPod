package de.danoeh.antennapod.wearos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.composable.ListScaffold

class FeedListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[FeedListViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            FeedListScreen(uiState = uiState, onOpenFeedEpisodes = { feedId ->
                val intent = Intent(this, EpisodeListActivity::class.java).apply {
                    putExtra(EpisodeListActivity.EXTRA_PATH, WearDataPaths.feedEpisodesPath(feedId))
                }
                startActivity(intent)
            })
        }
    }
}

@Composable
fun FeedListScreen(uiState: FeedListUiState, onOpenFeedEpisodes: (Long) -> Unit) {
    val scrollState = rememberScalingLazyListState()
    val feeds: List<Feed>? = uiState.feeds

    ListScaffold(
        titleRes = NavigationNames.getTitleResForPath(WearDataPaths.SUBSCRIPTIONS),
        scrollState = scrollState,
        isPhoneSupported = uiState.isPhoneSupported,
        isTimedOut = uiState.isTimedOut,
        isLoading = feeds == null,
        isEmpty = feeds?.isEmpty() == true
    ) {
        items(feeds!!) { feed ->
            ListItem(
                text = feed.title ?: "",
                onClick = { onOpenFeedEpisodes(feed.id) }
            )
        }
    }
}
