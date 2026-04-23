package de.danoeh.antennapod.wearos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class FeedListUiState(
    val feeds: List<Feed>? = null,
    val isPhoneSupported: Boolean = true,
    val isTimedOut: Boolean = false
)

class FeedListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FeedListUiState())
    val uiState: StateFlow<FeedListUiState> = _uiState

    init {
        viewModelScope.launch {
            WearDataRepository.feedsByPath.collect { map ->
                _uiState.update { it.copy(feeds = map[WearDataPaths.SUBSCRIPTIONS]) }
            }
        }

        viewModelScope.launch {
            val isSupported = withContext(Dispatchers.Default) {
                WearConnectionUtils.isPhoneSupported(getApplication())
            }
            if (!isSupported) {
                _uiState.update { it.copy(isPhoneSupported = false) }
                return@launch
            }
            WearDataRepository.sendMessage(getApplication(), WearDataPaths.SUBSCRIPTIONS)
            val received = withTimeoutOrNull(10.seconds) {
                WearDataRepository.feedsByPath.first { it.containsKey(WearDataPaths.SUBSCRIPTIONS) }
            }
            if (received == null) {
                _uiState.update { it.copy(isTimedOut = true) }
            }
        }
    }
}
