package de.danoeh.antennapod.wearos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
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

data class EpisodeListUiState(
    val episodes: List<FeedItem>? = null,
    val isPhoneSupported: Boolean = true,
    val isTimedOut: Boolean = false
)

class EpisodeListViewModel(application: Application, private val path: String) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(EpisodeListUiState())
    val uiState: StateFlow<EpisodeListUiState> = _uiState

    init {
        viewModelScope.launch {
            WearDataRepository.episodesByPath.collect { map ->
                _uiState.update { it.copy(episodes = map[path]) }
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
            WearDataRepository.sendMessage(getApplication(), path)
            val received = withTimeoutOrNull(10.seconds) {
                WearDataRepository.episodesByPath.first { it.containsKey(path) }
            }
            if (received == null) {
                _uiState.update { it.copy(isTimedOut = true) }
            }
        }
    }

    companion object {
        fun factory(path: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return EpisodeListViewModel(app, path) as T
            }
        }
    }
}
