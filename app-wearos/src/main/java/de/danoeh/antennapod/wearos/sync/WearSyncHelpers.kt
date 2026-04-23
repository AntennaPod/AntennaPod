package de.danoeh.antennapod.wearos.sync

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

fun ComponentActivity.requestDataFromPhone(path: String, tag: String) {
    lifecycleScope.launch(Dispatchers.IO) {
        val supported = WearConnectionUtils.isPhoneSupported(this@requestDataFromPhone)
        if (!supported) {
            return@launch
        }
        try {
            val nodes = Wearable.getNodeClient(this@requestDataFromPhone).connectedNodes.await()
            val node = nodes.firstOrNull() ?: run {
                Log.w(tag, "No connected nodes")
                return@launch
            }
            Wearable.getMessageClient(this@requestDataFromPhone).sendMessage(node.id, path, null).await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message to phone", e)
        }
    }
}

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
