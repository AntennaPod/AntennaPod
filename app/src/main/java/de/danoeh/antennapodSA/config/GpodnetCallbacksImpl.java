package de.danoeh.antennapodSA.config;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapodSA.core.GpodnetCallbacks;
import de.danoeh.antennapodSA.activity.MainActivity;


public class GpodnetCallbacksImpl implements GpodnetCallbacks {
    @Override
    public boolean gpodnetEnabled() {
        return true;
    }

    @Override
    public PendingIntent getGpodnetSyncServiceErrorNotificationPendingIntent(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
