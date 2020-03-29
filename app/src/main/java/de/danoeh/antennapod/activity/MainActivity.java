package de.danoeh.antennapod.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.dialog.RatingDialog;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.AudioPlayerFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.NavDrawerFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.fragment.TransitionEffect;
import de.danoeh.antennapod.preferences.PreferenceUpgrader;
import de.danoeh.antennapod.view.LockableBottomSheetBehavior;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * The activity that is shown when the user launches the app.
 */
public class MainActivity extends CastEnabledActivity {

    private static final String TAG = "MainActivity";
    public static final String MAIN_FRAGMENT_TAG = "main";

    public static final String PREF_NAME = "MainActivityPrefs";
    public static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";

    public static final String EXTRA_FRAGMENT_TAG = "fragment_tag";
    public static final String EXTRA_FRAGMENT_ARGS = "fragment_args";
    public static final String EXTRA_FEED_ID = "fragment_feed_id";
    public static final String EXTRA_OPEN_PLAYER = "open_player";

    private static final String SAVE_BACKSTACK_COUNT = "backstackCount";

    private DrawerLayout drawerLayout;
    private View navDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private LockableBottomSheetBehavior sheetBehavior;
    private long lastBackButtonPressTime = 0;

    @NonNull
    public static Intent getIntentToOpenFeed(@NonNull Context context, long feedId) {
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FEED_ID, feedId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getNoTitleTheme());
        super.onCreate(savedInstanceState);
        StorageUtils.checkStorageAvailability(this);
        setContentView(R.layout.main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navDrawer = findViewById(R.id.navDrawerFragment);

        final FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(() ->
                drawerToggle.setDrawerIndicatorEnabled(fm.getBackStackEntryCount() == 0));

        FragmentTransaction transaction = fm.beginTransaction();

        Fragment mainFragment = fm.findFragmentByTag(MAIN_FRAGMENT_TAG);
        if (mainFragment != null) {
            transaction.replace(R.id.main_view, mainFragment);
        } else {
            String lastFragment = NavDrawerFragment.getLastNavFragment(this);
            if (ArrayUtils.contains(NavDrawerFragment.NAV_DRAWER_TAGS, lastFragment)) {
                loadFragment(lastFragment, null);
            } else {
                try {
                    loadFeedFragmentById(Integer.parseInt(lastFragment), null);
                } catch (NumberFormatException e) {
                    // it's not a number, this happens if we removed
                    // a label from the NAV_DRAWER_TAGS
                    // give them a nice default...
                    loadFragment(QueueFragment.TAG, null);
                }
            }
        }
        NavDrawerFragment navDrawerFragment = new NavDrawerFragment();
        transaction.replace(R.id.navDrawerFragment, navDrawerFragment, NavDrawerFragment.TAG);
        AudioPlayerFragment audioPlayerFragment = new AudioPlayerFragment();
        transaction.replace(R.id.audioplayerFragment, audioPlayerFragment, AudioPlayerFragment.TAG);
        transaction.commit();

        checkFirstLaunch();
        PreferenceUpgrader.checkUpgrades(this);
        View bottomSheet = findViewById(R.id.audioplayerFragment);
        sheetBehavior = (LockableBottomSheetBehavior) BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setPeekHeight((int) getResources().getDimension(R.dimen.external_player_height));
        sheetBehavior.setHideable(false);
        sheetBehavior.setBottomSheetCallback(bottomSheetCallback);
    }

    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View view, int state) {
        }

        @Override
        public void onSlide(@NonNull View view, float slideOffset) {
            AudioPlayerFragment audioPlayer = (AudioPlayerFragment) getSupportFragmentManager()
                    .findFragmentByTag(AudioPlayerFragment.TAG);
            float condensedSlideOffset = Math.max(0.0f, Math.min(0.2f, slideOffset - 0.2f)) / 0.2f;
            audioPlayer.getExternalPlayerHolder().setAlpha(1 - condensedSlideOffset);
            audioPlayer.getExternalPlayerHolder().setVisibility(
                    condensedSlideOffset > 0.99f ? View.GONE : View.VISIBLE);
        }
    };

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        drawerLayout.removeDrawerListener(drawerToggle);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        super.setSupportActionBar(toolbar);
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_IS_FIRST_LAUNCH, true)) {
            loadFragment(AddFeedFragment.TAG, null);
            new Handler().postDelayed(() -> drawerLayout.openDrawer(navDrawer), 1500);

            // for backward compatibility, we only change defaults for fresh installs
            UserPreferences.setUpdateInterval(12);

            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(PREF_IS_FIRST_LAUNCH, false);
            edit.apply();
        }
    }

    public boolean isDrawerOpen() {
        return drawerLayout != null && navDrawer != null && drawerLayout.isDrawerOpen(navDrawer);
    }

    public LockableBottomSheetBehavior getBottomSheet() {
        return sheetBehavior;
    }

    public void setPlayerVisible(boolean visible) {
        getBottomSheet().setLocked(!visible);
        FrameLayout mainView = findViewById(R.id.main_view);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainView.getLayoutParams();
        params.setMargins(0, 0, 0, visible ? (int) getResources().getDimension(R.dimen.external_player_height) : 0);
        mainView.setLayoutParams(params);
        findViewById(R.id.audioplayerFragment).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void loadFragment(String tag, Bundle args) {
        Log.d(TAG, "loadFragment(tag: " + tag + ", args: " + args + ")");
        Fragment fragment;
        switch (tag) {
            case QueueFragment.TAG:
                fragment = new QueueFragment();
                break;
            case EpisodesFragment.TAG:
                fragment = new EpisodesFragment();
                break;
            case DownloadsFragment.TAG:
                fragment = new DownloadsFragment();
                break;
            case PlaybackHistoryFragment.TAG:
                fragment = new PlaybackHistoryFragment();
                break;
            case AddFeedFragment.TAG:
                fragment = new AddFeedFragment();
                break;
            case SubscriptionFragment.TAG:
                fragment = new SubscriptionFragment();
                break;
            default:
                // default to the queue
                fragment = new QueueFragment();
                tag = QueueFragment.TAG;
                args = null;
                break;
        }

        if (args != null) {
            fragment.setArguments(args);
        }
        NavDrawerFragment.saveLastNavFragment(this, tag);
        loadFragment(fragment);
    }

    public void loadFeedFragmentById(long feedId, Bundle args) {
        Fragment fragment = FeedItemlistFragment.newInstance(feedId);
        if (args != null) {
            fragment.setArguments(args);
        }
        NavDrawerFragment.saveLastNavFragment(this, String.valueOf(feedId));
        loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // clear back stack
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }
        FragmentTransaction t = fragmentManager.beginTransaction();
        t.replace(R.id.main_view, fragment, MAIN_FRAGMENT_TAG);
        fragmentManager.popBackStack();
        // TODO: we have to allow state loss here
        // since this function can get called from an AsyncTask which
        // could be finishing after our app has already committed state
        // and is about to get shutdown.  What we *should* do is
        // not commit anything in an AsyncTask, but that's a bigger
        // change than we want now.
        t.commitAllowingStateLoss();
        drawerLayout.closeDrawer(navDrawer);
    }

    public void loadChildFragment(Fragment fragment, TransitionEffect transition) {
        Validate.notNull(fragment);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (transition) {
            case FADE:
                transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
                break;
            case SLIDE:
                transaction.setCustomAnimations(
                        R.anim.slide_right_in,
                        R.anim.slide_left_out,
                        R.anim.slide_left_in,
                        R.anim.slide_right_out);
                break;
        }

        transaction
                .hide(getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG))
                .add(R.id.main_view, fragment, MAIN_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    public void loadChildFragment(Fragment fragment) {
        loadChildFragment(fragment, TransitionEffect.NONE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_BACKSTACK_COUNT, getSupportFragmentManager().getBackStackEntryCount());
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        RatingDialog.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
        handleNavIntent();
        RatingDialog.check();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            drawerLayout.closeDrawer(navDrawer);
        } else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            super.onBackPressed();
        } else {
            switch (UserPreferences.getBackButtonBehavior()) {
                case OPEN_DRAWER:
                    drawerLayout.openDrawer(navDrawer);
                    break;
                case SHOW_PROMPT:
                    new AlertDialog.Builder(this)
                        .setMessage(R.string.close_prompt)
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> MainActivity.super.onBackPressed())
                        .setNegativeButton(R.string.no, null)
                        .setCancelable(false)
                        .show();
                    break;
                case DOUBLE_TAP:
                    if (lastBackButtonPressTime < System.currentTimeMillis() - 2000) {
                        Toast.makeText(this, R.string.double_tap_toast, Toast.LENGTH_SHORT).show();
                        lastBackButtonPressTime = System.currentTimeMillis();
                    } else {
                        super.onBackPressed();
                    }
                    break;
                case GO_TO_PAGE:
                    if (NavDrawerFragment.getLastNavFragment(this).equals(UserPreferences.getBackButtonGoToPage())) {
                        super.onBackPressed();
                    } else {
                        loadFragment(UserPreferences.getBackButtonGoToPage(), null);
                    }
                    break;
                default: super.onBackPressed();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MessageEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        View parentLayout = findViewById(R.id.drawer_layout);
        Snackbar snackbar = Snackbar.make(parentLayout, event.message, Snackbar.LENGTH_SHORT);
        if (event.action != null) {
            snackbar.setAction(getString(R.string.undo), v -> event.action.run());
        }
        snackbar.show();
    }

    private void handleNavIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_FEED_ID) || intent.hasExtra(EXTRA_FRAGMENT_TAG)) {
            Log.d(TAG, "handleNavIntent()");
            String tag = intent.getStringExtra(EXTRA_FRAGMENT_TAG);
            Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
            long feedId = intent.getLongExtra(EXTRA_FEED_ID, 0);
            if (tag != null) {
                loadFragment(tag, args);
            } else if (feedId > 0) {
                loadFeedFragmentById(feedId, args);
            }
        } else if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetCallback.onSlide(null, 1.0f);
        }
        // to avoid handling the intent twice when the configuration changes
        setIntent(new Intent(MainActivity.this, MainActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
