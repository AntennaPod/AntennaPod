package de.danoeh.antennapod.core.cast;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import de.danoeh.antennapod.core.R;

public class CastButtonVisibilityManager {
    private static final String TAG = "CastBtnVisibilityMgr";
    private final CastManager castManager;
    private volatile boolean prefEnabled = false;
    private volatile boolean viewRequested = false;
    private volatile boolean resumed = false;
    private volatile boolean connected = false;
    private volatile int showAsAction = MenuItem.SHOW_AS_ACTION_IF_ROOM;
    private Menu menu;
    public SwitchableMediaRouteActionProvider mediaRouteActionProvider;

    public CastButtonVisibilityManager(CastManager castManager) {
        this.castManager = castManager;
    }

    public synchronized void setPrefEnabled(boolean newValue) {
        if (prefEnabled != newValue && resumed && (viewRequested || connected)) {
            if (newValue) {
                castManager.incrementUiCounter();
            } else {
                castManager.decrementUiCounter();
            }
        }
        prefEnabled = newValue;
        if (mediaRouteActionProvider != null) {
            mediaRouteActionProvider.setEnabled(prefEnabled && (viewRequested || connected));
        }
    }

    public synchronized void setResumed(boolean newValue) {
        if (resumed == newValue) {
            Log.e(TAG, "resumed should never change to the same value");
            return;
        }
        resumed = newValue;
        if (prefEnabled && (viewRequested || connected)) {
            if (resumed) {
                castManager.incrementUiCounter();
            } else {
                castManager.decrementUiCounter();
            }
        }
    }

    public synchronized void setViewRequested(boolean newValue) {
        if (viewRequested != newValue && resumed && prefEnabled && !connected) {
            if (newValue) {
                castManager.incrementUiCounter();
            } else {
                castManager.decrementUiCounter();
            }
        }
        viewRequested = newValue;
        if (mediaRouteActionProvider != null) {
            mediaRouteActionProvider.setEnabled(prefEnabled && (viewRequested || connected));
        }
    }

    public synchronized void setConnected(boolean newValue) {
        if (connected != newValue && resumed && prefEnabled && !prefEnabled) {
            if (newValue) {
                castManager.incrementUiCounter();
            } else {
                castManager.decrementUiCounter();
            }
        }
        connected = newValue;
        if (mediaRouteActionProvider != null) {
            mediaRouteActionProvider.setEnabled(prefEnabled && (viewRequested || connected));
        }
    }

    public synchronized boolean shouldEnable() {
        return prefEnabled && viewRequested;
    }

    public void setMenu(Menu menu) {
        setViewRequested(false);
        showAsAction = MenuItem.SHOW_AS_ACTION_IF_ROOM;
        this.menu = menu;
        setShowAsAction();
    }

    public void requestCastButton(int showAsAction) {
        setViewRequested(true);
        this.showAsAction = showAsAction;
        setShowAsAction();
    }

    public void onConnected() {
        setConnected(true);
        setShowAsAction();
    }

    public void onDisconnected() {
        setConnected(false);
        setShowAsAction();
    }

    private void setShowAsAction() {
        if (menu == null) {
            Log.d(TAG, "setShowAsAction() without a menu");
            return;
        }
        MenuItem item = menu.findItem(R.id.media_route_menu_item);
        if (item == null) {
            Log.e(TAG, "setShowAsAction(), but cast button not inflated");
            return;
        }
        item.setShowAsAction(connected ? MenuItem.SHOW_AS_ACTION_ALWAYS : showAsAction);
    }
}
