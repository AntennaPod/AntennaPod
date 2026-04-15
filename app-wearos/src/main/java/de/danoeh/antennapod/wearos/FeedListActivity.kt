package de.danoeh.antennapod.wearos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.composable.ListScaffold
import de.danoeh.antennapod.wearos.sync.WearDataPathUtils
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import de.danoeh.antennapod.wearos.sync.rememberPhoneStatus
import de.danoeh.antennapod.wearos.sync.requestDataFromPhone
import kotlinx.coroutines.flow.first

class FeedListActivity : ComponentActivity() {
    private val path = WearDataPaths.SUBSCRIPTIONS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeedListScreen(activity = this)
        }
    }

    override fun onResume() {
        super.onResume()
        requestDataFromPhone(path, TAG)
    }

    fun openFeedEpisodes(feedId: Long) {
        val intent = Intent(this, EpisodeListActivity::class.java).apply {
            putExtra(EpisodeListActivity.EXTRA_PATH, WearDataPaths.feedEpisodesPath(feedId))
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "FeedListActivity"
    }
}

@Composable
fun FeedListScreen(activity: FeedListActivity) {
    val scrollState = rememberScalingLazyListState()
    val feeds: List<Feed>? = WearDataRepository.feedsByPath.collectAsState().value[WearDataPaths.SUBSCRIPTIONS]
    val (isPhoneSupported, isTimedOut) = rememberPhoneStatus(activity, WearDataPaths.SUBSCRIPTIONS) {
        WearDataRepository.feedsByPath.first { it.containsKey(WearDataPaths.SUBSCRIPTIONS) }
    }

    ListScaffold(
        titleRes = WearDataPathUtils.getTitleResForPath(WearDataPaths.SUBSCRIPTIONS),
        scrollState = scrollState,
        isPhoneSupported = isPhoneSupported,
        isTimedOut = isTimedOut,
        isLoading = feeds == null,
        isEmpty = feeds?.isEmpty() == true
    ) {
        items(feeds!!) { feed ->
            ListItem(
                text = feed.title ?: "",
                onClick = { activity.openFeedEpisodes(feed.id) }
            )
        }
    }
}
