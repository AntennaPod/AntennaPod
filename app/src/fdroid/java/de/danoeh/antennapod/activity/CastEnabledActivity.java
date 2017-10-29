package de.danoeh.antennapod.activity;

import android.support.v7.app.AppCompatActivity;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends AppCompatActivity {
//        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "CastEnabledActivity";

//    protected CastManager castManager;
//    protected SwitchableMediaRouteActionProvider mediaRouteActionProvider;
//    private final CastButtonVisibilityManager castButtonVisibilityManager = new CastButtonVisibilityManager();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
//                registerOnSharedPreferenceChangeListener(this);
//
//        castManager = CastManager.getInstance();
//        castManager.addCastConsumer(castConsumer);
//        castButtonVisibilityManager.setPrefEnabled(UserPreferences.isCastEnabled());
//        onCastConnectionChanged(castManager.isConnected());
//    }
//
//    @Override
//    protected void onDestroy() {
//        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
//                .unregisterOnSharedPreferenceChangeListener(this);
//        castManager.removeCastConsumer(castConsumer);
//        super.onDestroy();
//    }
//
//    @Override
//    @CallSuper
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.cast_enabled, menu);
//        castButtonVisibilityManager.setMenu(menu);
//        return true;
//    }
//
//    @Override
//    @CallSuper
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        super.onPrepareOptionsMenu(menu);
//        mediaRouteActionProvider = castManager
//                .addMediaRouterButton(menu.findItem(R.id.media_route_menu_item));
//        mediaRouteActionProvider.setEnabled(castButtonVisibilityManager.shouldEnable());
//        return true;
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        castButtonVisibilityManager.setResumed(true);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        castButtonVisibilityManager.setResumed(false);
//    }
//
//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        if (UserPreferences.PREF_CAST_ENABLED.equals(key)) {
//            boolean newValue = UserPreferences.isCastEnabled();
//            Log.d(TAG, "onSharedPreferenceChanged(), isCastEnabled set to " + newValue);
//            castButtonVisibilityManager.setPrefEnabled(newValue);
//            // PlaybackService has its own listener, so if it's active we don't have to take action here.
//            if (!newValue && !PlaybackService.isRunning) {
//                CastManager.getInstance().disconnect();
//            }
//        }
//    }
//
//    CastConsumer castConsumer = new DefaultCastConsumer() {
//        @Override
//        public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
//            onCastConnectionChanged(true);
//        }
//
//        @Override
//        public void onDisconnected() {
//            onCastConnectionChanged(false);
//        }
//    };
//
//    private void onCastConnectionChanged(boolean connected) {
//        if (connected) {
//            castButtonVisibilityManager.onConnected();
//            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
//        } else {
//            castButtonVisibilityManager.onDisconnected();
//            setVolumeControlStream(AudioManager.STREAM_MUSIC);
//        }
//    }
//
//    /**
//     * Should be called by any activity or fragment for which the cast button should be shown.
//     *
//     * @param showAsAction refer to {@link MenuItem#setShowAsAction(int)}
//     */
    public final void requestCastButton(int showAsAction) {
        // no-op
    }

//    private class CastButtonVisibilityManager {
//        private volatile boolean prefEnabled = false;
//        private volatile boolean viewRequested = false;
//        private volatile boolean resumed = false;
//        private volatile boolean connected = false;
//        private volatile int showAsAction = MenuItem.SHOW_AS_ACTION_IF_ROOM;
//        private Menu menu;
//
//        public synchronized void setPrefEnabled(boolean newValue) {
//            if (prefEnabled != newValue && resumed && (viewRequested || connected)) {
//                if (newValue) {
//                    castManager.incrementUiCounter();
//                } else {
//                    castManager.decrementUiCounter();
//                }
//            }
//            prefEnabled = newValue;
//            if (mediaRouteActionProvider != null) {
//                mediaRouteActionProvider.setEnabled(prefEnabled && (viewRequested || connected));
//            }
//        }
//
//        public synchronized void setResumed(boolean newValue) {
//            if (resumed == newValue) {
//                Log.e(TAG, "resumed should never change to the same value");
//                return;
//            }
//            resumed = newValue;
//            if (prefEnabled && (viewRequested || connected)) {
//                if (resumed) {
//                    castManager.incrementUiCounter();
//                } else {
//                    castManager.decrementUiCounter();
//                }
//            }
//        }
//
//        public synchronized void setViewRequested(boolean newValue) {
//            if (viewRequested != newValue && resumed && prefEnabled && !connected) {
//                if (newValue) {
//                    castManager.incrementUiCounter();
//                } else {
//                    castManager.decrementUiCounter();
//                }
//            }
//            viewRequested = newValue;
//            if (mediaRouteActionProvider != null) {
//                mediaRouteActionProvider.setEnabled(prefEnabled && (viewRequested || connected));
//            }
//        }
//
//        public synchronized void setConnected(boolean newValue) {
//            if (connected != newValue && resumed && prefEnabled && !prefEnabled) {
//                if (newValue) {
//                    castManager.incrementUiCounter();
//                } else {
//                    castManager.decrementUiCounter();
//                }
//            }
//            connected = newValue;
//            if (mediaRouteActionProvider != null) {
//                mediaRouteActionProvider.setEnabled(prefEnabled && (viewRequested || connected));
//            }
//        }
//
//        public synchronized boolean shouldEnable() {
//            return prefEnabled && viewRequested;
//        }
//
//        public void setMenu(Menu menu) {
//            setViewRequested(false);
//            showAsAction = MenuItem.SHOW_AS_ACTION_IF_ROOM;
//            this.menu = menu;
//            setShowAsAction();
//        }
//
//        public void requestCastButton(int showAsAction) {
//            setViewRequested(true);
//            this.showAsAction = showAsAction;
//            setShowAsAction();
//        }
//
//        public void onConnected() {
//            setConnected(true);
//            setShowAsAction();
//        }
//
//        public void onDisconnected() {
//            setConnected(false);
//            setShowAsAction();
//        }
//
//        private void setShowAsAction() {
//            if (menu == null) {
//                Log.d(TAG, "setShowAsAction() without a menu");
//                return;
//            }
//            MenuItem item = menu.findItem(R.id.media_route_menu_item);
//            if (item == null) {
//                Log.e(TAG, "setShowAsAction(), but cast button not inflated");
//                return;
//            }
//            MenuItemCompat.setShowAsAction(item, connected? MenuItem.SHOW_AS_ACTION_ALWAYS : showAsAction);
//        }
//    }
}
