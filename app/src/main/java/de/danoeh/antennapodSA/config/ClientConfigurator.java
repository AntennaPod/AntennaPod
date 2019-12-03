package de.danoeh.antennapodSA.config;

import de.danoeh.antennapodSA.BuildConfig;
import de.danoeh.antennapodSA.config.CastCallbackImpl;
import de.danoeh.antennapodSA.core.ClientConfig;

/**
 * Configures the ClientConfig class of the core package.
 */
class ClientConfigurator {

    private ClientConfigurator(){}

    static {
        ClientConfig.USER_AGENT = "AntennaPod/" + BuildConfig.VERSION_NAME;
        ClientConfig.applicationCallbacks = new ApplicationCallbacksImpl();
        ClientConfig.downloadServiceCallbacks = new DownloadServiceCallbacksImpl();
        ClientConfig.gpodnetCallbacks = new GpodnetCallbacksImpl();
        ClientConfig.playbackServiceCallbacks = new PlaybackServiceCallbacksImpl();
        ClientConfig.dbTasksCallbacks = new DBTasksCallbacksImpl();
        ClientConfig.castCallbacks = new CastCallbackImpl();
    }
}
