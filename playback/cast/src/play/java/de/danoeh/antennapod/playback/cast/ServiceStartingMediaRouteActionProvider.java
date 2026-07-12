package de.danoeh.antennapod.playback.cast;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteButton;

/**
 * Shows the currently playing episode directly after connecting to the cast device
 * by starting the playback service.
 */
public class ServiceStartingMediaRouteActionProvider extends MediaRouteActionProvider {
    private static final String TAG = "SrvStartMediaRouteBtn";

    public ServiceStartingMediaRouteActionProvider(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public MediaRouteButton onCreateMediaRouteButton() {
        return new ServiceStartingMediaRouteButton(getContext());
    }

    private static class ServiceStartingMediaRouteButton extends MediaRouteButton {
        ServiceStartingMediaRouteButton(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean performClick() {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(getContext(),
                    "de.danoeh.antennapod.playback.service.Media3PlaybackService"));
            try {
                getContext().startService(intent);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unable to start playback service", e);
            }
            return super.performClick();
        }
    }
}
