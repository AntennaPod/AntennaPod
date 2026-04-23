package de.danoeh.antennapod.wearos.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun rememberPhoneStatus(path: String, awaitData: suspend () -> Any): Pair<Boolean, Boolean> {
    var isPhoneSupported by remember(path) { mutableStateOf(true) }
    var isTimedOut by remember(path) { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(path) {
        isPhoneSupported = withContext(Dispatchers.Default) {
            WearConnectionUtils.isPhoneSupported(context)
        }
        if (isPhoneSupported) {
            val received = withTimeoutOrNull(10.seconds) {
                awaitData()
            }
            if (received == null) {
                isTimedOut = true
            }
        }
    }
    return isPhoneSupported to isTimedOut
}
