package de.danoeh.antennapod.config;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.app.MediaRouteControllerDialogFragment;
import android.support.v7.app.MediaRouteDialogFactory;

import de.danoeh.antennapod.cast.CustomMRControllerDialog;
import de.danoeh.antennapod.core.CastCallbacks;

public class CastCallbackImpl implements CastCallbacks {
    @Override
    public MediaRouteDialogFactory getMediaRouterDialogFactory() {
        return new MediaRouteDialogFactory() {
            @NonNull
            @Override
            public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
                return new MediaRouteControllerDialogFragment() {
                    @Override
                    public MediaRouteControllerDialog onCreateControllerDialog(Context context, Bundle savedInstanceState) {
                        return new CustomMRControllerDialog(context);
                    }
                };
            }
        };
    }
}
