package de.danoeh.antennapod.wearos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import de.danoeh.antennapod.net.sync.wearinterface.WearDataPaths
import de.danoeh.antennapod.net.sync.wearinterface.WearNowPlaying
import de.danoeh.antennapod.wearos.sync.WearDataRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(val connectedPhone: String = "", val nowPlaying: WearNowPlaying? = null)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val name = WearConnectionUtils.getConnectedNodeName(getApplication())
            _uiState.update { it.copy(connectedPhone = name) }
        }

        viewModelScope.launch {
            while (true) {
                WearDataRepository.sendMessage(getApplication(), WearDataPaths.NOW_PLAYING)
                delay(5.seconds)
            }
        }

        viewModelScope.launch {
            WearDataRepository.nowPlaying.collect { nowPlaying ->
                _uiState.update { it.copy(nowPlaying = nowPlaying) }
            }
        }
    }
}
