package de.danoeh.antennapod.playback.service.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;

/**
 * Pauses playback when audio is about to switch away from the current route
 * (e.g., Bluetooth disconnect or headphones unplugged). This helps with
 * A2DP/AVRCP scenarios where the car disconnects and we should stop playback
 * cleanly.
 */
public class BecomingNoisyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!UserPreferences.isPauseOnHeadsetDisconnect()) {
            return;
        }
        // Dispatch a media pause command; MediaSession/AVRCP will propagate to car head units.
        Intent pauseIntent = MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_PAUSE);
        context.sendBroadcast(pauseIntent);
    }
}