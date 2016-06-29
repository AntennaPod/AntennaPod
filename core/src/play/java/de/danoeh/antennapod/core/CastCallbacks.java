package de.danoeh.antennapod.core;

import android.support.annotation.Nullable;
import android.support.v7.app.MediaRouteDialogFactory;

/**
 * Callbacks for Chromecast support on the core module
 */
public interface CastCallbacks {

    @Nullable MediaRouteDialogFactory getMediaRouterDialogFactory();
}
