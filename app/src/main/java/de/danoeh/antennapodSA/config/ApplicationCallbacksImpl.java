package de.danoeh.antennapodSA.config;


import android.app.Application;
import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapodSA.core.ApplicationCallbacks;
import de.danoeh.antennapodSA.PodcastApp;
import de.danoeh.antennapodSA.activity.StorageErrorActivity;

public class ApplicationCallbacksImpl implements ApplicationCallbacks {

    @Override
    public Application getApplicationInstance() {
        return PodcastApp.getInstance();
    }

    @Override
    public Intent getStorageErrorActivity(Context context) {
        return new Intent(context, StorageErrorActivity.class);
    }

}
