package de.danoeh.antennapodSA.config;

import androidx.annotation.NonNull;
import androidx.mediarouter.app.MediaRouteControllerDialogFragment;
import androidx.mediarouter.app.MediaRouteDialogFactory;

import de.danoeh.antennapodSA.core.CastCallbacks;
import de.danoeh.antennapodSA.fragment.CustomMRControllerDialogFragment;

public class CastCallbackImpl implements CastCallbacks {
    @Override
    public MediaRouteDialogFactory getMediaRouterDialogFactory() {
        return new MediaRouteDialogFactory() {
            @NonNull
            @Override
            public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
                return new CustomMRControllerDialogFragment();
            }
        };
    }
}
