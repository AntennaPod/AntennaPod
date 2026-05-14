package de.danoeh.antennapod.wearos.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

object WearMessageSender {
    private const val TAG = "WearMessageSender"

    suspend fun send(context: Context, path: String) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val node = nodes.firstOrNull() ?: run {
                Log.w(TAG, "No connected nodes")
                return
            }
            Wearable.getMessageClient(context).sendMessage(node.id, path, null).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to phone", e)
        }
    }
}
