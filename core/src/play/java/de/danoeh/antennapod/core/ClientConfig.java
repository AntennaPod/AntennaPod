package de.danoeh.antennapodSA.core;

import android.content.Context;
import android.util.Log;

import de.danoeh.antennapodSA.core.cast.CastManager;
import de.danoeh.antennapodSA.core.preferences.PlaybackPreferences;
import de.danoeh.antennapodSA.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.storage.PodDBAdapter;
import de.danoeh.antennapodSA.core.util.NetworkUtils;
import de.danoeh.antennapodSA.core.util.exception.RxJavaErrorHandlerSetup;

/**
 * Stores callbacks for core classes like Services, DB classes etc. and other configuration variables.
 * Apps using the core module of AntennaPod should register implementations of all interfaces here.
 */
public class ClientConfig {
    private static final String TAG = "ClientConfig";

    private ClientConfig(){}

    /**
     * Should be used when setting User-Agent header for HTTP-requests.
     */
    public static String USER_AGENT;

    public static ApplicationCallbacks applicationCallbacks;

    public static DownloadServiceCallbacks downloadServiceCallbacks;

    public static PlaybackServiceCallbacks playbackServiceCallbacks;

    public static GpodnetCallbacks gpodnetCallbacks;

    public static DBTasksCallbacks dbTasksCallbacks;

    public static CastCallbacks castCallbacks;

    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }
        PodDBAdapter.init(context);
        UserPreferences.init(context);
        PlaybackPreferences.init(context);
        NetworkUtils.init(context);
        // Don't initialize Cast-related logic unless it is enabled, to avoid the unnecessary
        // Google Play Service usage.
        // Down side: when the user decides to enable casting, AntennaPod needs to be restarted
        // for it to take effect.
        if (UserPreferences.isCastEnabled()) {
            CastManager.init(context);
        } else {
            Log.v(TAG, "Cast is disabled. All Cast-related initialization will be skipped.");
        }
        SleepTimerPreferences.init(context);
        RxJavaErrorHandlerSetup.setupRxJavaErrorHandler();
        initialized = true;
    }

}
