package de.danoeh.antennapod.config;

import de.danoeh.antennapod.core.ClientConfig;

/**
 * Configures the ClientConfig class of the core package.
 */
public class ClientConfigurator {

    static {
        ClientConfig.USER_AGENT = "AntennaPod/0.9.9.5";
        ClientConfig.applicationCallbacks = new ApplicationCallbacksImpl();
        ClientConfig.downloadServiceCallbacks = new DownloadServiceCallbacksImpl();
        ClientConfig.gpodnetCallbacks = new GpodnetCallbacksImpl();
        ClientConfig.playbackServiceCallbacks = new PlaybackServiceCallbacksImpl();
        ClientConfig.storageCallbacks = new StorageCallbacksImpl();
        ClientConfig.flattrCallbacks = new FlattrCallbacksImpl();
    }
}
