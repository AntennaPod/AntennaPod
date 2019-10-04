package de.danoeh.antennapod.core.cast;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteChooserDialogFragment;
import androidx.mediarouter.app.MediaRouteControllerDialogFragment;
import androidx.mediarouter.media.MediaRouter;
import android.util.Log;

/**
 * <p>Action Provider that extends {@link MediaRouteActionProvider} and allows the client to
 * disable completely the button by calling {@link #setEnabled(boolean)}.</p>
 *
 * <p>It is disabled by default, so if a client wants to initially have it enabled it must call
 * <code>setEnabled(true)</code>.</p>
 */
public class SwitchableMediaRouteActionProvider extends MediaRouteActionProvider {
    public static final String TAG = "SwitchblMediaRtActProv";

    private static final String CHOOSER_FRAGMENT_TAG =
            "android.support.v7.mediarouter:MediaRouteChooserDialogFragment";
    private static final String CONTROLLER_FRAGMENT_TAG =
            "android.support.v7.mediarouter:MediaRouteControllerDialogFragment";
    private boolean enabled;

    public SwitchableMediaRouteActionProvider(Context context) {
        super(context);
        enabled = false;
    }

    /**
     * <p>Sets whether the Media Router button should be allowed to become visible or not.</p>
     *
     * <p>It's invisible by default.</p>
     */
    public void setEnabled(boolean newVal) {
        enabled = newVal;
        refreshVisibility();
    }

    @Override
    public boolean isVisible() {
        return enabled && super.isVisible();
    }

    @Override
    public boolean onPerformDefaultAction() {
        if (!super.onPerformDefaultAction()) {
            // there is no button, but we should still show the dialog if it's the case.
            if (!isVisible()) {
                return false;
            }
            FragmentManager fm = getFragmentManager();
            if (fm == null) {
                return false;
            }
            MediaRouter.RouteInfo route = MediaRouter.getInstance(getContext()).getSelectedRoute();
            if (route.isDefault() || !route.matchesSelector(getRouteSelector())) {
                if (fm.findFragmentByTag(CHOOSER_FRAGMENT_TAG) != null) {
                    Log.w(TAG, "showDialog(): Route chooser dialog already showing!");
                    return false;
                }
                MediaRouteChooserDialogFragment f =
                        getDialogFactory().onCreateChooserDialogFragment();
                f.setRouteSelector(getRouteSelector());
                f.show(fm, CHOOSER_FRAGMENT_TAG);
            } else {
                if (fm.findFragmentByTag(CONTROLLER_FRAGMENT_TAG) != null) {
                    Log.w(TAG, "showDialog(): Route controller dialog already showing!");
                    return false;
                }
                MediaRouteControllerDialogFragment f =
                        getDialogFactory().onCreateControllerDialogFragment();
                f.show(fm, CONTROLLER_FRAGMENT_TAG);
            }
            return true;

        } else {
            return true;
        }
    }

    private FragmentManager getFragmentManager() {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity)activity).getSupportFragmentManager();
        }
        return null;
    }

    private Activity getActivity() {
        // Gross way of unwrapping the Activity so we can get the FragmentManager
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
}
