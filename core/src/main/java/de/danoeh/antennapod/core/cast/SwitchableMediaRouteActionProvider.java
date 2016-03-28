package de.danoeh.antennapod.core.cast;

import android.content.Context;
import android.support.v7.app.MediaRouteActionProvider;

/**
 * <p>Action Provider that extends {@link MediaRouteActionProvider} and allows the client to
 * disable completely the button by calling {@link #setEnabled(boolean)}.</p>
 *
 * <p>It is disabled by default, so if a client wants to initially have it enabled it must call
 * <code>setEnabled(true)</code>.</p>
 */
public class SwitchableMediaRouteActionProvider extends MediaRouteActionProvider {

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
}
