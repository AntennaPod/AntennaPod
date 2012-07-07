package de.podfetcher.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Updates the state of the player widget */
public class PlayerWidgetService extends Service {
    public PlayerWidgetService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
