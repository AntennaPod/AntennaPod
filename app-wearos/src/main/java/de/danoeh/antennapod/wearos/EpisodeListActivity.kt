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
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.composable.ListScaffold

class EpisodeListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_PATH) ?: run {
            finish()
            return
        }
        val viewModel = ViewModelProvider(this, EpisodeListViewModel.factory(path))[EpisodeListViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            EpisodeListScreen(
                uiState = uiState,
                onOpenEpisodeDetail = { episode ->
                    val intent = Intent(this, EpisodeDetailActivity::class.java).apply {
                        putExtra(EpisodeDetailActivity.EXTRA_EPISODE, episode)
                    }
                    startActivity(intent)
                }
            )
        }
    }

    companion object {
        const val EXTRA_PATH = "path"
    }
}

@Composable
fun EpisodeListScreen(uiState: EpisodeListUiState, onOpenEpisodeDetail: (FeedItem) -> Unit) {
    val scrollState = rememberScalingLazyListState()
    val episodes: List<FeedItem>? = uiState.episodes

    ListScaffold(
        titleRes = uiState.titleRes,
        scrollState = scrollState,
        isPhoneSupported = uiState.isPhoneSupported,
        isTimedOut = uiState.isTimedOut,
        isLoading = episodes == null,
        isEmpty = episodes?.isEmpty() == true
    ) {
        items(episodes!!) { episode ->
            ListItem(
                text = episode.title ?: "",
                onClick = { onOpenEpisodeDetail(episode) }
            )
        }
    }
}
