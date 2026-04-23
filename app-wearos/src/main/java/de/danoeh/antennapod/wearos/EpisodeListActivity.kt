package de.danoeh.antennapod.wearos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.composable.ListScaffold
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import de.danoeh.antennapod.wearos.sync.rememberPhoneStatus
import kotlinx.coroutines.flow.first

class EpisodeListActivity : ComponentActivity() {
    private var path: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = intent.getStringExtra(EXTRA_PATH) ?: run {
            finish()
            return
        }
        setContent {
            EpisodeListScreen(path = path, onOpenEpisodeDetail = { episode -> openEpisodeDetail(episode) })
        }
    }

    override fun onResume() {
        super.onResume()
        WearDataRepository.requestDataFromPhone(this, lifecycleScope, path, TAG)
    }

    private fun openEpisodeDetail(episode: FeedItem) {
        val intent = Intent(this, EpisodeDetailActivity::class.java).apply {
            putExtra(EpisodeDetailActivity.EXTRA_EPISODE, episode)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_PATH = "path"
        private const val TAG = "EpisodeListActivity"
    }
}

@Composable
fun EpisodeListScreen(path: String, onOpenEpisodeDetail: (FeedItem) -> Unit) {
    val scrollState = rememberScalingLazyListState()
    val titleRes = NavigationNames.getTitleResForPath(path)
    val episodes: List<FeedItem>? = WearDataRepository.episodesByPath.collectAsState().value[path]
    val (isPhoneSupported, isTimedOut) = rememberPhoneStatus(path) {
        WearDataRepository.episodesByPath.first { it.containsKey(path) }
    }

    ListScaffold(
        titleRes = titleRes,
        scrollState = scrollState,
        isPhoneSupported = isPhoneSupported,
        isTimedOut = isTimedOut,
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
