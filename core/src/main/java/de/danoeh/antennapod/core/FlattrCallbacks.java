package de.danoeh.antennapod.core;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.shredzone.flattr4j.oauth.AccessToken;

/**
 * Callbacks for the flattr integration of the app.
 */
public interface FlattrCallbacks {

    /**
     * Returns if true if the flattr integration should be activated,
     * false otherwise.
     */
    boolean flattrEnabled();

    /**
     * Returns an intent that starts the activity that is responsible for
     * letting users log into their flattr account.
     *
     * @return The intent that starts the authentication activity or null
     * if flattr integration is disabled (i.e. flattrEnabled() == false).
     */
    Intent getFlattrAuthenticationActivityIntent(Context context);

    PendingIntent getFlattrFailedNotificationContentIntent(Context context);

    String getFlattrAppKey();

    String getFlattrAppSecret();

    void handleFlattrAuthenticationSuccess(AccessToken token);
}
