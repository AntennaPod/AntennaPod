package de.danoeh.antennapod.net.sync.wearinterface;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public final class WearConnectionUtils {
    private static final String TAG = "WearConnectionUtils";

    private WearConnectionUtils() {
    }

    /**
     * Returns true if a reachable phone with the matching capability is connected.
     * The capability is declared in the phone's wear.xml and versioned, so both sides
     * can negotiate compatibility when updated independently.
     */
    public static boolean isPhoneSupported(@NonNull Context context) {
        try {
            String capability = context.getString(R.string.wear_capability_phone);
            Set<Node> nodes = Tasks.await(
                    Wearable.getCapabilityClient(context)
                            .getCapability(capability, CapabilityClient.FILTER_REACHABLE))
                    .getNodes();
            return !nodes.isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Failed to check phone capability", e);
            return false;
        } catch (ExecutionException e) {
            Log.w(TAG, "Failed to check phone capability", e);
            return false;
        }
    }

    @NonNull
    public static String getConnectedNodeName(@NonNull Context context) {
        try {
            var nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());
            if (nodes.isEmpty()) {
                return "";
            }
            return nodes.get(0).getDisplayName();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Failed to get connected node name", e);
            return "";
        } catch (ExecutionException e) {
            Log.w(TAG, "Failed to get connected node name", e);
            return "";
        }
    }
}
