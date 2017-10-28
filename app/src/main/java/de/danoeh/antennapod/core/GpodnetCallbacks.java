package de.danoeh.antennapod.core;

import android.app.PendingIntent;
import android.content.Context;

/**
 * Callbacks related to the gpodder.net integration of the core module
 */
public interface GpodnetCallbacks {


    /**
     * Returns if true if the gpodder.net integration should be activated,
     * false otherwise.
     */
    boolean gpodnetEnabled();

    /**
     * Returns a PendingIntent for the error notification of the GpodnetSyncService.
     * <p/>
     * What the PendingIntent does may be implementation-specific.
     *
     * @return A PendingIntent for the notification or null if gpodder.net integration
     * has been disabled (i.e. gpodnetEnabled() == false).
     */
    PendingIntent getGpodnetSyncServiceErrorNotificationPendingIntent(Context context);
}
