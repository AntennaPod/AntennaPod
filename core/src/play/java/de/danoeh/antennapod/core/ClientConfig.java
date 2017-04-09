package de.danoeh.antennapod.core;

import android.content.Context;

import de.danoeh.antennapod.core.cast.CastManager;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * Stores callbacks for core classes like Services, DB classes etc. and other configuration variables.
 * Apps using the core module of AntennaPod should register implementations of all interfaces here.
 */
public class ClientConfig {

    /**
     * Should be used when setting User-Agent header for HTTP-requests.
     */
    public static String USER_AGENT;

    public static ApplicationCallbacks applicationCallbacks;

    public static DownloadServiceCallbacks downloadServiceCallbacks;

    public static PlaybackServiceCallbacks playbackServiceCallbacks;

    public static GpodnetCallbacks gpodnetCallbacks;

    public static FlattrCallbacks flattrCallbacks;

    public static DBTasksCallbacks dbTasksCallbacks;

    public static CastCallbacks castCallbacks;

    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if(initialized) {
            return;
        }
        PodDBAdapter.init(context);
        UserPreferences.init(context);
        UpdateManager.init(context);
        PlaybackPreferences.init(context);
        NetworkUtils.init(context);
        CastManager.init(context);
        SleepTimerPreferences.init(context);
        initialized = true;
    }

}
