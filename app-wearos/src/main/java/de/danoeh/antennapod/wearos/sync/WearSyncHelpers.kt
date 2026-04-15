package de.danoeh.antennapod.wearos.sync

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

fun ComponentActivity.requestDataFromPhone(path: String, tag: String) {
    lifecycleScope.launch(Dispatchers.IO) {
        val supported = WearConnectionUtils.isPhoneSupported(this@requestDataFromPhone)
        if (!supported) {
            return@launch
        }
        Wearable.getNodeClient(this@requestDataFromPhone).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull() ?: run {
                    Log.w(tag, "No connected nodes")
                    return@addOnSuccessListener
                }
                Wearable.getMessageClient(this@requestDataFromPhone).sendMessage(node.id, path, null)
                    .addOnFailureListener { e -> Log.e(tag, "Failed to send message", e) }
            }
            .addOnFailureListener { e -> Log.e(tag, "Failed to get connected nodes", e) }
    }
}

@Composable
fun rememberPhoneStatus(
    activity: ComponentActivity,
    path: String,
    waitForData: suspend () -> Any
): Pair<Boolean, Boolean> {
    var isPhoneSupported by remember(path) { mutableStateOf(true) }
    var isTimedOut by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        isPhoneSupported = withContext(Dispatchers.Default) {
            WearConnectionUtils.isPhoneSupported(activity)
        }
        if (isPhoneSupported) {
            val received = withTimeoutOrNull(10.seconds) {
                waitForData()
            }
            if (received == null) {
                isTimedOut = true
            }
        }
    }
    return isPhoneSupported to isTimedOut
}
