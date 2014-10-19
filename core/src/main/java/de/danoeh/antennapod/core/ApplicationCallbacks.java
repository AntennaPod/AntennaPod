package de.danoeh.antennapod.core;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

/**
 * Callbacks related to the application in general
 */
public interface ApplicationCallbacks {

    /**
     * Returns a non-null instance of the application class
     */
    public Application getApplicationInstance();

    /**
     * Returns a non-null intent that starts the storage error
     * activity.
     */
    public Intent getStorageErrorActivity(Context context);

    public void setUpdateInterval(long updateInterval);
}
