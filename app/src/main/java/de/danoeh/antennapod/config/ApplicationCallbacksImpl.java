package de.danoeh.antennapod.config;


import android.app.Application;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.core.ApplicationCallbacks;

public class ApplicationCallbacksImpl implements ApplicationCallbacks {

    @Override
    public Application getApplicationInstance() {
        return PodcastApp.getInstance();
    }
}
