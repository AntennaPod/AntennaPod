package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.cast.CastEnabledActivity;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.importexport.AutomaticDatabaseExportWorker;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.TransitionEffect;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.common.ThemeSwitcher;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.discovery.DiscoveryFragment;
import de.danoeh.antennapod.ui.screen.AddFeedFragment;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.InboxFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.screen.download.DownloadLogFragment;
import de.danoeh.antennapod.ui.screen.drawer.BottomNavigationMoreAdapter;
import de.danoeh.antennapod.ui.screen.drawer.DrawerPreferencesDialog;
import de.danoeh.antennapod.ui.screen.drawer.NavDrawerFragment;
import de.danoeh.antennapod.ui.screen.drawer.NavListAdapter;
import de.danoeh.antennapod.ui.screen.drawer.NavigationNames;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.screen.home.HomeFragment;
import de.danoeh.antennapod.ui.screen.playback.audio.AudioPlayerFragment;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;
import de.danoeh.antennapod.ui.screen.rating.RatingDialogManager;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionFragment;
import de.danoeh.antennapod.ui.view.LockableBottomSheetBehavior;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The activity that is shown when the user launches the app.
 */
public class MainActivity extends CastEnabledActivity {

    private static final String TAG = "MainActivity";
    public static final String MAIN_FRAGMENT_TAG = "main";

    public static final String PREF_NAME = "MainActivityPrefs";
    public static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";

    public static final String EXTRA_FEED_ID = "fragment_feed_id";
    public static final String EXTRA_REFRESH_ON_START = "refresh_on_start";
    public static final String EXTRA_ADD_TO_BACK_STACK = "add_to_back_stack";
    public static final String KEY_GENERATED_VIEW_ID = "generated_view_id";

    private @Nullable DrawerLayout drawerLayout;
    private @Nullable ActionBarDrawerToggle drawerToggle;
    private BottomNavigationView bottomNavigationView;
    private Disposable bottomNavigationBadgeLoader = null;
    private View navDrawer;
    private LockableBottomSheetBehavior sheetBehavior;
    private RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
    private int lastTheme = 0;
    private Insets systemBarInsets = Insets.NONE;

    @NonNull
    public static Intent getIntentToOpenFeed(@NonNull Context context, long feedId) {
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FEED_ID, feedId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        lastTheme = ThemeSwitcher.getNoTitleTheme(this);
        setTheme(lastTheme);
        if (savedInstanceState != null) {
            ensureGeneratedViewIdGreaterThan(savedInstanceState.getInt(KEY_GENERATED_VIEW_ID, 0));
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        recycledViewPool.setMaxRecycledViews(R.id.view_type_episode_item, 25);

        drawerLayout = findViewById(R.id.drawer_layout);
        navDrawer = findViewById(R.id.navDrawerFragment);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (UserPreferences.isBottomNavigationEnabled()) {
            buildBottomNavigationMenu();
            if (drawerLayout == null) { // Tablet mode
                navDrawer.setVisibility(View.GONE);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
            drawerLayout = null;
        } else {
            bottomNavigationView.setVisibility(View.GONE);
            bottomNavigationView = null;
            setNavDrawerSize();
        }

        // Consume navigation bar insets - we apply them in setPlayerVisible()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view), (v, insets) -> {
            systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            updateInsets();
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.NONE)
                    .build();
        });

        final FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(MAIN_FRAGMENT_TAG) == null) {
            if (!UserPreferences.DEFAULT_PAGE_REMEMBER.equals(UserPreferences.getDefaultPage())) {
                loadFragment(UserPreferences.getDefaultPage(), null);
            } else {
                String lastFragment = NavDrawerFragment.getLastNavFragment(this);
                if (ArrayUtils.contains(getResources().getStringArray(R.array.nav_drawer_section_tags), lastFragment)) {
                    loadFragment(lastFragment, null);
                } else {
                    try {
                        loadFeedFragmentById(Integer.parseInt(lastFragment), null);
                    } catch (NumberFormatException e) {
                        // it's not a number, this happens if we removed
                        // a label from the NAV_DRAWER_TAGS
                        // give them a nice default...
                        loadFragment(HomeFragment.TAG, null);
                    }
                }
            }
        }

        FragmentTransaction transaction = fm.beginTransaction();
        NavDrawerFragment navDrawerFragment = new NavDrawerFragment();
        transaction.replace(R.id.navDrawerFragment, navDrawerFragment, NavDrawerFragment.TAG);
        AudioPlayerFragment audioPlayerFragment = new AudioPlayerFragment();
        transaction.replace(R.id.audioplayerFragment, audioPlayerFragment, AudioPlayerFragment.TAG);
        transaction.commit();

        checkFirstLaunch();
        View bottomSheet = findViewById(R.id.audioplayerFragment);
        sheetBehavior = (LockableBottomSheetBehavior) BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(false);
        sheetBehavior.setBottomSheetCallback(bottomSheetCallback);

        FeedUpdateManager.getInstance().restartUpdateAlarm(this, false);
        SynchronizationQueue.getInstance().syncIfNotSyncedRecently();
        AutomaticDatabaseExportWorker.enqueueIfNeeded(this, false);

        WorkManager.getInstance(this)
                .getWorkInfosByTagLiveData(FeedUpdateManagerImpl.WORK_TAG_FEED_UPDATE)
                .observe(this, workInfos -> {
                    boolean isRefreshingFeeds = false;
                    for (WorkInfo workInfo : workInfos) {
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            isRefreshingFeeds = true;
                        } else if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
                            isRefreshingFeeds = true;
                        }
                    }
                    EventBus.getDefault().postSticky(new FeedUpdateRunningEvent(isRefreshingFeeds));
                });
        WorkManager.getInstance(this)
                .getWorkInfosByTagLiveData(DownloadServiceInterface.WORK_TAG)
                .observe(this, workInfos -> {
                    Map<String, DownloadStatus> updatedEpisodes = new HashMap<>();
                    for (WorkInfo workInfo : workInfos) {
                        String downloadUrl = null;
                        for (String tag : workInfo.getTags()) {
                            if (tag.startsWith(DownloadServiceInterface.WORK_TAG_EPISODE_URL)) {
                                downloadUrl = tag.substring(DownloadServiceInterface.WORK_TAG_EPISODE_URL.length());
                            }
                        }
                        if (downloadUrl == null) {
                            continue;
                        }
                        int status;
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            status = DownloadStatus.STATE_RUNNING;
                        } else if (workInfo.getState() == WorkInfo.State.ENQUEUED
                                || workInfo.getState() == WorkInfo.State.BLOCKED) {
                            status = DownloadStatus.STATE_QUEUED;
                        } else {
                            status = DownloadStatus.STATE_COMPLETED;
                        }
                        int progress = workInfo.getProgress().getInt(DownloadServiceInterface.WORK_DATA_PROGRESS, -1);
                        if (progress == -1 && status != DownloadStatus.STATE_COMPLETED) {
                            status = DownloadStatus.STATE_QUEUED;
                            progress = 0;
                        }
                        if (updatedEpisodes.containsKey(downloadUrl) && status == DownloadStatus.STATE_COMPLETED) {
                            continue; // In case of a duplicate, prefer running/queued over completed
                        }
                        updatedEpisodes.put(downloadUrl, new DownloadStatus(status, progress));
                    }
                    DownloadServiceInterface.get().setCurrentDownloads(updatedEpisodes);
                    EventBus.getDefault().postSticky(new EpisodeDownloadEvent(updatedEpisodes));
                });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateInsets();
    }

    /**
     * View.generateViewId stores the current ID in a static variable.
     * When the process is killed, the variable gets reset.
     * This makes sure that we do not get ID collisions
     * and therefore errors when trying to restore state from another view.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void ensureGeneratedViewIdGreaterThan(int minimum) {
        while (View.generateViewId() <= minimum) {
            // Generate new IDs
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_GENERATED_VIEW_ID, View.generateViewId());
    }

    private final BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new AntennaPodBottomSheetCallback();

    private class AntennaPodBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        @Override
        public void onStateChanged(@NonNull View view, int state) {
            if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                onSlide(view, 0.0f);
            } else if (state == BottomSheetBehavior.STATE_EXPANDED) {
                onSlide(view, 1.0f);
            } else if (state == BottomSheetBehavior.STATE_HIDDEN) {
                IntentUtils.sendLocalBroadcast(MainActivity.this,
                        PlaybackServiceInterface.ACTION_SHUTDOWN_PLAYBACK_SERVICE);
                PlaybackPreferences.writeNoMediaPlaying();
                setPlayerVisible(false);
            }
        }

        @Override
        public void onSlide(@NonNull View view, float slideOffset) {
            AudioPlayerFragment audioPlayer = (AudioPlayerFragment) getSupportFragmentManager()
                    .findFragmentByTag(AudioPlayerFragment.TAG);
            if (audioPlayer == null) {
                return;
            }

            if (slideOffset == 0.0f) { //STATE_COLLAPSED
                audioPlayer.scrollToPage(AudioPlayerFragment.POS_COVER);
            }

            audioPlayer.fadePlayerToToolbar(slideOffset);
        }
    }

    public void setupToolbarToggle(@NonNull MaterialToolbar toolbar, boolean displayUpArrow) {
        if (drawerLayout != null) { // Tablet layout does not have a drawer
            if (drawerToggle != null) {
                drawerLayout.removeDrawerListener(drawerToggle);
            }
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.drawer_open, R.string.drawer_close);
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
            drawerToggle.setDrawerIndicatorEnabled(!displayUpArrow);
            drawerToggle.setToolbarNavigationClickListener(v -> getSupportFragmentManager().popBackStack());
        } else if (!displayUpArrow) {
            toolbar.setNavigationIcon(null);
        } else {
            toolbar.setNavigationIcon(ThemeUtils.getDrawableFromAttr(this, R.attr.homeAsUpIndicator));
            toolbar.setNavigationOnClickListener(v -> getSupportFragmentManager().popBackStack());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawerLayout != null && drawerToggle != null) {
            drawerLayout.removeDrawerListener(drawerToggle);
        }
        if (bottomNavigationBadgeLoader != null) {
            bottomNavigationBadgeLoader.dispose();
            bottomNavigationBadgeLoader = null;
        }
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_IS_FIRST_LAUNCH, true)) {
            FeedUpdateManager.getInstance().restartUpdateAlarm(this, true);

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

    private void updateInsets() {
        setPlayerVisible(findViewById(R.id.audioplayerFragment).getVisibility() == View.VISIBLE);
    }

    public void setPlayerVisible(boolean visible) {
        getBottomSheet().setLocked(!visible);
        findViewById(R.id.audioplayerFragment).setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            bottomSheetCallback.onStateChanged(null, getBottomSheet().getState()); // Update toolbar visibility
        } else {
            getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        View bottomPaddingView = findViewById(R.id.bottom_padding);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomPaddingView.getLayoutParams();
        params.height = systemBarInsets.bottom;
        bottomPaddingView.setLayoutParams(params);

        int externalPlayerHeight = (int) getResources().getDimension(R.dimen.external_player_height);
        FragmentContainerView mainView = findViewById(R.id.main_content_view);
        params = (ViewGroup.MarginLayoutParams) mainView.getLayoutParams();
        params.setMargins(systemBarInsets.left, 0, systemBarInsets.right, (visible ? externalPlayerHeight : 0));
        mainView.setLayoutParams(params);
        sheetBehavior.setPeekHeight(externalPlayerHeight);
        sheetBehavior.setHideable(true);
        sheetBehavior.setGestureInsetBottomIgnored(true);

        FragmentContainerView playerView = findViewById(R.id.playerFragment);
        ViewGroup.MarginLayoutParams playerParams = (ViewGroup.MarginLayoutParams) playerView.getLayoutParams();
        playerParams.setMargins(systemBarInsets.left, 0, systemBarInsets.right, 0);
        playerView.setLayoutParams(playerParams);
        RelativeLayout playerContent = findViewById(R.id.playerContent);
        playerContent.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0);
    }

    public RecyclerView.RecycledViewPool getRecycledViewPool() {
        return recycledViewPool;
    }

    public Fragment createFragmentInstance(String tag, Bundle args) {
        Log.d(TAG, "loadFragment(tag: " + tag + ", args: " + args + ")");
        Fragment fragment;
        switch (tag) {
            case HomeFragment.TAG:
                fragment = new HomeFragment();
                break;
            case QueueFragment.TAG:
                fragment = new QueueFragment();
                break;
            case InboxFragment.TAG:
                fragment = new InboxFragment();
                break;
            case AllEpisodesFragment.TAG:
                fragment = new AllEpisodesFragment();
                break;
            case CompletedDownloadsFragment.TAG:
                fragment = new CompletedDownloadsFragment();
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
            case DiscoveryFragment.TAG:
                fragment = new DiscoveryFragment();
                break;
            default:
                // default to home screen
                fragment = new HomeFragment();
                args = null;
                break;
        }
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    public void loadFragment(String tag, Bundle args) {
        NavDrawerFragment.saveLastNavFragment(this, tag);
        if (bottomNavigationView != null) {
            int bottomSelectedItem = NavigationNames.getBottomNavigationItemId(tag);
            if (bottomNavigationView.getMenu().findItem(bottomSelectedItem) == null) {
                bottomSelectedItem = R.id.bottom_navigation_more;
            }
            bottomNavigationView.setOnItemSelectedListener(null);
            bottomNavigationView.setSelectedItemId(bottomSelectedItem);
            bottomNavigationView.setOnItemSelectedListener(bottomItemSelectedListener);
        }
        loadFragment(createFragmentInstance(tag, args));
    }

    public void loadFeedFragmentById(long feedId, Bundle args) {
        Fragment fragment = FeedItemlistFragment.newInstance(feedId);
        if (args != null) {
            fragment.setArguments(args);
        }
        NavDrawerFragment.saveLastNavFragment(this, String.valueOf(feedId));
        loadFragment(fragment);
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // clear back stack
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }
        FragmentTransaction t = fragmentManager.beginTransaction();
        t.replace(R.id.main_content_view, fragment, MAIN_FRAGMENT_TAG);
        fragmentManager.popBackStack();
        // TODO: we have to allow state loss here
        // since this function can get called from an AsyncTask which
        // could be finishing after our app has already committed state
        // and is about to get shutdown.  What we *should* do is
        // not commit anything in an AsyncTask, but that's a bigger
        // change than we want now.
        t.commitAllowingStateLoss();

        if (drawerLayout != null) { // Tablet layout does not have a drawer
            drawerLayout.closeDrawer(navDrawer);
        }
    }

    public void loadChildFragment(Fragment fragment, TransitionEffect transition) {
        Validate.notNull(fragment);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (transition == TransitionEffect.FADE) {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        } else if (transition == TransitionEffect.SLIDE) {
            transaction.setCustomAnimations(
                    R.anim.slide_right_in,
                    R.anim.slide_left_out,
                    R.anim.slide_left_in,
                    R.anim.slide_right_out);
        }

        transaction
                .hide(getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG))
                .add(R.id.main_content_view, fragment, MAIN_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    public void loadChildFragment(Fragment fragment) {
        loadChildFragment(fragment, TransitionEffect.NONE);
    }

    private void buildBottomNavigationMenu() {
        List<String> drawerItems = UserPreferences.getVisibleDrawerItemOrder();
        drawerItems.remove(NavListAdapter.SUBSCRIPTION_LIST_TAG);

        Menu menu = bottomNavigationView.getMenu();
        menu.clear();
        int maxItems = Math.min(5, bottomNavigationView.getMaxItemCount());
        for (int i = 0; i < drawerItems.size() && i < maxItems - 1; i++) {
            String tag = drawerItems.get(i);
            MenuItem item = menu.add(0, NavigationNames.getBottomNavigationItemId(tag),
                    0, getString(NavigationNames.getShortLabel(tag)));
            item.setIcon(NavigationNames.getDrawable(tag));
        }
        MenuItem moreItem = menu.add(0, R.id.bottom_navigation_more, 0, getString(R.string.overflow_more));
        moreItem.setIcon(R.drawable.dots_vertical);
        bottomNavigationView.setOnItemSelectedListener(bottomItemSelectedListener);
        updateBottomNavigationBadgeIfNeeded();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        updateBottomNavigationBadgeIfNeeded();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        updateBottomNavigationBadgeIfNeeded();
    }

    private void updateBottomNavigationBadgeIfNeeded() {
        if (bottomNavigationView == null) {
            return;
        } else if (bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_inbox) == null) {
            return;
        }
        if (bottomNavigationBadgeLoader != null) {
            bottomNavigationBadgeLoader.dispose();
        }
        bottomNavigationBadgeLoader = Observable.fromCallable(
                        () -> DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.NEW)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.bottom_navigation_inbox);
                    badge.setVisible(result > 0);
                    badge.setNumber(result);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private final NavigationBarView.OnItemSelectedListener bottomItemSelectedListener = item -> {
        if (item.getItemId() == R.id.bottom_navigation_more) {
            showBottomNavigationMorePopup();
            return false;
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            loadFragment(NavigationNames.getBottomNavigationFragmentTag(item.getItemId()), null);
            return true;
        }
    };

    private void showBottomNavigationMorePopup() {
        List<String> drawerItems = UserPreferences.getVisibleDrawerItemOrder();
        drawerItems.remove(NavListAdapter.SUBSCRIPTION_LIST_TAG);

        final List<MenuItem> popupMenuItems = new ArrayList<>();
        for (int i = bottomNavigationView.getMaxItemCount() - 1; i < drawerItems.size(); i++) {
            String tag = drawerItems.get(i);
            MenuItem item = new MenuBuilder(this).add(0, NavigationNames.getBottomNavigationItemId(tag),
                    0, getString(NavigationNames.getLabel(tag)));
            item.setIcon(NavigationNames.getDrawable(tag));
            popupMenuItems.add(item);
        }
        MenuItem customizeItem = new MenuBuilder(this).add(0, R.id.bottom_navigation_settings,
                0, getString(R.string.pref_nav_drawer_items_title));
        customizeItem.setIcon(R.drawable.ic_pencil);
        popupMenuItems.add(customizeItem);

        MenuItem settingsItem = new MenuBuilder(this).add(0, R.id.bottom_navigation_settings,
                0, getString(R.string.settings_label));
        settingsItem.setIcon(R.drawable.ic_settings);
        popupMenuItems.add(settingsItem);

        final ListPopupWindow listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setWidth((int) (250 * getResources().getDisplayMetrics().density));
        listPopupWindow.setAnchorView(bottomNavigationView);
        listPopupWindow.setAdapter(new BottomNavigationMoreAdapter(this, popupMenuItems));
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (position == popupMenuItems.size() - 1) {
                startActivity(new Intent(this, PreferenceActivity.class));
            } else if (position == popupMenuItems.size() - 2) {
                new DrawerPreferencesDialog(this, this::buildBottomNavigationMenu).show();
            } else {
                loadFragment(NavigationNames.getBottomNavigationFragmentTag(
                        popupMenuItems.get(position).getItemId()), null);
            }
            listPopupWindow.dismiss();
        });
        listPopupWindow.setDropDownGravity(Gravity.END | Gravity.BOTTOM);
        listPopupWindow.setModal(true);
        listPopupWindow.show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) { // Tablet layout does not have a drawer
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) { // Tablet layout does not have a drawer
            drawerToggle.onConfigurationChanged(newConfig);
        }
        setNavDrawerSize();
    }

    private void setNavDrawerSize() {
        if (drawerToggle == null) { // Tablet layout does not have a drawer
            return;
        }
        float screenPercent = getResources().getInteger(R.integer.nav_drawer_screen_size_percent) * 0.01f;
        int width = (int) (getScreenWidth() * screenPercent);
        int maxWidth = (int) getResources().getDimension(R.dimen.nav_drawer_max_screen_size);

        navDrawer.getLayoutParams().width = Math.min(width, maxWidth);
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (getBottomSheet().getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetCallback.onSlide(null, 1.0f);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        new RatingDialogManager(this).showIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleNavIntent();

        boolean hasBottomNavigation = bottomNavigationView != null;
        if (lastTheme != ThemeSwitcher.getNoTitleTheme(this)
                || hasBottomNavigation != UserPreferences.isBottomNavigationEnabled()) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }
        if (UserPreferences.getHiddenDrawerItems().contains(NavDrawerFragment.getLastNavFragment(this))) {
            loadFragment(UserPreferences.getDefaultPage(), null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        lastTheme = ThemeSwitcher.getNoTitleTheme(this); // Don't recreate activity when a result is pending
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

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
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) { // Tablet layout does not have a drawer
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
        if (isDrawerOpen() && drawerLayout != null) {
            drawerLayout.closeDrawer(navDrawer);
        } else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            super.onBackPressed();
        } else {
            String toPage = UserPreferences.getDefaultPage();
            if (NavDrawerFragment.getLastNavFragment(this).equals(toPage)
                    || UserPreferences.DEFAULT_PAGE_REMEMBER.equals(toPage)) {
                if (UserPreferences.backButtonOpensDrawer() && drawerLayout != null && bottomNavigationView == null) {
                    drawerLayout.openDrawer(navDrawer);
                } else {
                    super.onBackPressed();
                }
            } else {
                loadFragment(toPage, null);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MessageEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");

        Snackbar snackbar = showSnackbarAbovePlayer(event.message, Snackbar.LENGTH_LONG);
        if (event.action != null) {
            snackbar.setAction(event.actionText, v -> event.action.accept(this));
        }
    }

    private void handleNavIntent() {
        Log.d(TAG, "handleNavIntent()");
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_FEED_ID)) {
            long feedId = intent.getLongExtra(EXTRA_FEED_ID, 0);
            Bundle args = intent.getBundleExtra(MainActivityStarter.EXTRA_FRAGMENT_ARGS);
            if (feedId > 0) {
                boolean addToBackStack = intent.getBooleanExtra(EXTRA_ADD_TO_BACK_STACK, false);
                if (addToBackStack) {
                    loadChildFragment(FeedItemlistFragment.newInstance(feedId));
                } else {
                    loadFeedFragmentById(feedId, args);
                }
            }
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (intent.hasExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG)) {
            String tag = intent.getStringExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG);
            Bundle args = intent.getBundleExtra(MainActivityStarter.EXTRA_FRAGMENT_ARGS);
            if (tag != null) {
                Fragment fragment = createFragmentInstance(tag, args);
                if (intent.getBooleanExtra(MainActivityStarter.EXTRA_ADD_TO_BACK_STACK, false)) {
                    loadChildFragment(fragment);
                } else {
                    loadFragment(fragment);
                }
            }
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_PLAYER, false)) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetCallback.onSlide(null, 1.0f);
        } else {
            handleDeeplink(intent.getData());
        }

        if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_DRAWER, false) && drawerLayout != null) {
            drawerLayout.open();
        }
        if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_DOWNLOAD_LOGS, false)) {
            new DownloadLogFragment().show(getSupportFragmentManager(), null);
        }
        if (intent.getBooleanExtra(EXTRA_REFRESH_ON_START, false)) {
            FeedUpdateManager.getInstance().runOnceOrAsk(this);
        }
        // to avoid handling the intent twice when the configuration changes
        setIntent(new Intent(MainActivity.this, MainActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavIntent();
    }

    public Snackbar showSnackbarAbovePlayer(CharSequence text, int duration) {
        Snackbar s;
        if (getBottomSheet().getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            s = Snackbar.make(findViewById(R.id.main_content_view), text, duration);
            if (findViewById(R.id.audioplayerFragment).getVisibility() == View.VISIBLE) {
                s.setAnchorView(findViewById(R.id.audioplayerFragment));
            }
        } else {
            s = Snackbar.make(findViewById(android.R.id.content), text, duration);
        }
        s.show();
        return s;
    }

    public Snackbar showSnackbarAbovePlayer(int text, int duration) {
        return showSnackbarAbovePlayer(getResources().getText(text), duration);
    }

    /**
     * Handles the deep link incoming via App Actions.
     * Performs an in-app search or opens the relevant feature of the app
     * depending on the query.
     *
     * @param uri incoming deep link
     */
    private void handleDeeplink(Uri uri) {
        if (uri == null || uri.getPath() == null) {
            return;
        }
        Log.d(TAG, "Handling deeplink: " + uri.toString());
        switch (uri.getPath()) {
            case "/deeplink/search":
                String query = uri.getQueryParameter("query");
                if (query == null) {
                    return;
                }

                this.loadChildFragment(SearchFragment.newInstance(query));
                break;
            case "/deeplink/main":
                String feature = uri.getQueryParameter("page");
                if (feature == null) {
                    return;
                }
                switch (feature) {
                    case "DOWNLOADS":
                        loadFragment(CompletedDownloadsFragment.TAG, null);
                        break;
                    case "HISTORY":
                        loadFragment(PlaybackHistoryFragment.TAG, null);
                        break;
                    case "EPISODES":
                        loadFragment(AllEpisodesFragment.TAG, null);
                        break;
                    case "QUEUE":
                        loadFragment(QueueFragment.TAG, null);
                        break;
                    case "SUBSCRIPTIONS":
                        loadFragment(SubscriptionFragment.TAG, null);
                        break;
                    default:
                        showSnackbarAbovePlayer(getString(R.string.app_action_not_found, feature),
                                Snackbar.LENGTH_LONG);
                        return;
                }
                break;
            default:
                break;
        }
    }
  
    //Hardware keyboard support
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            return super.onKeyUp(keyCode, event);
        }

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        Integer customKeyCode = null;
        EventBus.getDefault().post(event);

        switch (keyCode) {
            case KeyEvent.KEYCODE_P:
                customKeyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case KeyEvent.KEYCODE_J: //Fallthrough
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_COMMA:
                customKeyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
                break;
            case KeyEvent.KEYCODE_K: //Fallthrough
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_PERIOD:
                customKeyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                break;
            case KeyEvent.KEYCODE_PLUS: //Fallthrough
            case KeyEvent.KEYCODE_W:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_MINUS: //Fallthrough
            case KeyEvent.KEYCODE_S:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_M:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                break;
            default:
                break;
        }

        if (customKeyCode != null) {
            sendBroadcast(MediaButtonStarter.createIntent(this, customKeyCode));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
