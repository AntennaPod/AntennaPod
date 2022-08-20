package de.danoeh.antennapod.core;

import android.app.Application;

/**
 * Callbacks related to the application in general
 */
public interface ApplicationCallbacks {

    /**
     * Returns a non-null instance of the application class
     */
    Application getApplicationInstance();
}
