package de.danoeh.antennapod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;

import java.util.Locale;

public final class ManagedConfiguration {
    private static final String TAG = "ManagedConfiguration";
    private static final String KEY_SERVER = "gpodder_server";
    private static final String KEY_USERNAME = "gpodder_username";
    private static final String KEY_PASSWORD = "gpodder_password";
    private static final String KEY_DEVICE = "gpodder_device";

    private ManagedConfiguration() {
    }

    public static void install(Context context) {
        apply(context);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                apply(c);
            }
        }, new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED));
    }

    public static void apply(Context context) {
        RestrictionsManager manager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        if (manager == null) {
            return;
        }
        Bundle restrictions = manager.getApplicationRestrictions();
        if (restrictions == null) {
            return;
        }
        String server = restrictions.getString(KEY_SERVER);
        String username = restrictions.getString(KEY_USERNAME);
        String password = restrictions.getString(KEY_PASSWORD);
        String device = restrictions.getString(KEY_DEVICE);
        if (TextUtils.isEmpty(server) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return;
        }
        if (TextUtils.isEmpty(device)) {
            device = (username + "_device").replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.US);
        }
        boolean unchanged = SynchronizationProvider.GPODDER_NET.getIdentifier()
                .equals(SynchronizationSettings.getSelectedSyncProviderKey())
                && server.equals(SynchronizationCredentials.getHosturl())
                && username.equals(SynchronizationCredentials.getUsername())
                && password.equals(SynchronizationCredentials.getPassword())
                && device.equals(SynchronizationCredentials.getDeviceId());
        if (unchanged) {
            return;
        }
        SynchronizationCredentials.clear();
        SynchronizationCredentials.setHosturl(server);
        SynchronizationCredentials.setUsername(username);
        SynchronizationCredentials.setPassword(password);
        SynchronizationCredentials.setDeviceId(device);
        SynchronizationSettings.setSelectedSyncProvider(SynchronizationProvider.GPODDER_NET.getIdentifier());
        Log.i(TAG, "Applied gpodder sync settings from managed configuration");
        SynchronizationQueue.getInstance().fullSync();
    }
}
