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
    public boolean flattrEnabled();

    /**
     * Returns an intent that starts the activity that is responsible for
     * letting users log into their flattr account.
     *
     * @return The intent that starts the authentication activity or null
     * if flattr integration is disabled (i.e. flattrEnabled() == false).
     */
    public Intent getFlattrAuthenticationActivityIntent(Context context);

    public PendingIntent getFlattrFailedNotificationContentIntent(Context context);

    public String getFlattrAppKey();

    public String getFlattrAppSecret();

    public void handleFlattrAuthenticationSuccess(AccessToken token);
}
