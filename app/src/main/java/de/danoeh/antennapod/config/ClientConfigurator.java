package de.danoeh.antennapod.config;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;

/**
 * Configures the ClientConfig class of the core package.
 */
class ClientConfigurator {

    static {
        ClientConfig.USER_AGENT = "AntennaPod/" + BuildConfig.VERSION_NAME;
        ClientConfig.applicationCallbacks = new ApplicationCallbacksImpl();
        ClientConfig.downloadServiceCallbacks = new DownloadServiceCallbacksImpl();
        ClientConfig.gpodnetCallbacks = new GpodnetCallbacksImpl();
        ClientConfig.playbackServiceCallbacks = new PlaybackServiceCallbacksImpl();
        ClientConfig.flattrCallbacks = new FlattrCallbacksImpl();
        ClientConfig.dbTasksCallbacks = new DBTasksCallbacksImpl();
        ClientConfig.castCallbacks = new CastCallbackImpl();
    }
}
