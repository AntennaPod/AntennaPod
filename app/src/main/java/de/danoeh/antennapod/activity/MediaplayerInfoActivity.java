package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.viewpagerindicator.CirclePageIndicator;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.ChaptersFragment;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Activity for playing files that do not require a video surface.
 */
public abstract class MediaplayerInfoActivity extends MediaplayerActivity implements NavDrawerActivity {

    private static final String TAG = "MediaplayerInfoActivity";

    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int POS_CHAPTERS = 2;
    private static final int NUM_CONTENT_FRAGMENTS = 3;

    private static final String PREFS = "AudioPlayerActivityPreferences";
    private static final String PREF_KEY_SELECTED_FRAGMENT_POSITION = "selectedFragmentPosition";

    private static final String[] NAV_DRAWER_TAGS = {
            QueueFragment.TAG,
            EpisodesFragment.TAG,
            SubscriptionFragment.TAG,
            DownloadsFragment.TAG,
            PlaybackHistoryFragment.TAG,
            AddFeedFragment.TAG,
            NavListAdapter.SUBSCRIPTION_LIST_TAG
    };

    ImageButton butPlaybackSpeed;
    TextView txtvPlaybackSpeed;
    ImageButton butCastDisconnect;
    private DrawerLayout drawerLayout;
    private NavListAdapter navAdapter;
    private ListView navList;
    private View navDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private int mPosition = -1;

    private ViewPager pager;
    private MediaplayerInfoPagerAdapter pagerAdapter;

    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        if (disposable != null) {
            disposable.dispose();
        }
        saveCurrentFragment();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // don't risk creating memory leaks
        drawerLayout = null;
        navAdapter = null;
        navList = null;
        navDrawer = null;
        drawerToggle = null;
        pager = null;
        pagerAdapter = null;
    }

    @Override
    protected void chooseTheme() {
        setTheme(UserPreferences.getNoTitleTheme());
    }

    void saveCurrentFragment() {
        if(pager == null) {
            return;
        }
        Log.d(TAG, "Saving preferences");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, pager.getCurrentItem())
                .apply();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    private void loadLastFragment() {
        Log.d(TAG, "Restoring instance state");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, -1);
        pager.setCurrentItem(lastPosition);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onAwaitingVideoSurface() {
        Log.d(TAG, "onAwaitingVideoSurface was called in audio player -> switching to video player");
        startActivity(new Intent(this, VideoplayerActivity.class));
    }

    @Override
    protected void postStatusMsg(int resId, boolean showToast) {
        if (resId == R.string.player_preparing_msg
                || resId == R.string.player_seeking_msg
                || resId == R.string.player_buffering_msg) {
            // TODO Show progress bar here
        }
        if (showToast) {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void clearStatusMsg() {
        // TODO Hide progress bar here
    }


    @Override
    protected void setupGUI() {
        super.setupGUI();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.shadow).setVisibility(View.GONE);
            AppBarLayout appBarLayout = findViewById(R.id.appBar);
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            appBarLayout.setElevation(px);
        }
        drawerLayout = findViewById(R.id.drawer_layout);
        navList = findViewById(R.id.nav_list);
        navDrawer = findViewById(R.id.nav_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerLayout.addDrawerListener(drawerToggle);

        navAdapter = new NavListAdapter(itemAccess, this);
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener((parent, view, position, id) -> {
            int viewType = parent.getAdapter().getItemViewType(position);
            if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER) {
                Intent intent = new Intent(MediaplayerInfoActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_NAV_TYPE, viewType);
                intent.putExtra(MainActivity.EXTRA_NAV_INDEX, position);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(navDrawer);
        });
        navList.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < navAdapter.getTags().size()) {
                showDrawerPreferencesDialog();
                return true;
            } else {
                mPosition = position;
                return false;
            }
        });
        registerForContextMenu(navList);
        drawerToggle.syncState();

        findViewById(R.id.nav_settings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(navDrawer);
            startActivity(new Intent(MediaplayerInfoActivity.this, PreferenceActivity.class));
        });

        butPlaybackSpeed = findViewById(R.id.butPlaybackSpeed);
        txtvPlaybackSpeed = findViewById(R.id.txtvPlaybackSpeed);
        butCastDisconnect = findViewById(R.id.butCastDisconnect);

        pager = findViewById(R.id.pager);
        pager.setOffscreenPageLimit(3);
        pagerAdapter = new MediaplayerInfoPagerAdapter(getSupportFragmentManager(), this);
        pager.setAdapter(pagerAdapter);
        CirclePageIndicator pageIndicator = findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(pager);
        loadLastFragment();
        pager.onSaveInstanceState();

        navList.post(this::supportStartPostponedEnterTransition);
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_VIDEO) {
            Log.d(TAG, "ReloadNotification received, switching to Videoplayer now");
            finish();
            startActivity(new Intent(this, VideoplayerActivity.class));

        }
    }

    @Override
    protected void onBufferStart() {
        postStatusMsg(R.string.player_buffering_msg, false);
    }

    @Override
    protected void onBufferEnd() {
        clearStatusMsg();
    }

    public PlaybackController getPlaybackController() {
        return controller;
    }

    @Override
    public boolean isDrawerOpen() {
        return drawerLayout != null && navDrawer != null && drawerLayout.isDrawerOpen(navDrawer);
    }

    @Override
    protected int getContentViewResourceId() {
        return R.layout.mediaplayerinfo_activity;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v.getId() != R.id.nav_list) {
            return;
        }
        AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = adapterInfo.position;
        if(position < navAdapter.getSubscriptionOffset()) {
            return;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_feed_context, menu);
        Feed feed = navDrawerData.feeds.get(position - navAdapter.getSubscriptionOffset());
        menu.setHeaderTitle(feed.getTitle());
        // episodes are not loaded, so we cannot check if the podcast has new or unplayed ones!
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int position = mPosition;
        mPosition = -1; // reset
        if(position < 0) {
            return false;
        }
        Feed feed = navDrawerData.feeds.get(position - navAdapter.getSubscriptionOffset());
        switch(item.getItemId()) {
            case R.id.remove_all_new_flags_item:
                DBWriter.removeFeedNewFlag(feed.getId());
                return true;
            case R.id.mark_all_read_item:
                DBWriter.markFeedRead(feed.getId());
                return true;
            case R.id.rename_item:
                new RenameFeedDialog(this, feed).show();
                return true;
            case R.id.remove_item:
                final FeedRemover remover = new FeedRemover(this, feed);
                ConfirmationDialog conDialog = new ConfirmationDialog(this,
                        R.string.remove_feed_label,
                        getString(R.string.feed_delete_confirmation_msg, feed.getTitle())) {
                    @Override
                    public void onConfirmButtonPressed(
                            DialogInterface dialog) {
                        dialog.dismiss();
                        if (controller != null) {
                            Playable playable = controller.getMedia();
                            if (playable != null && playable instanceof FeedMedia) {
                                FeedMedia media = (FeedMedia) playable;
                                if (media.getItem() != null && media.getItem().getFeed() != null &&
                                        media.getItem().getFeed().getId() == feed.getId()) {
                                    Log.d(TAG, "Currently playing episode is about to be deleted, skipping");
                                    remover.skipOnCompletion = true;
                                    if(controller.getStatus() == PlayerStatus.PLAYING) {
                                        IntentUtils.sendLocalBroadcast(MediaplayerInfoActivity.this, PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE);
                                    }
                                }
                            }
                        }
                        remover.executeAsync();
                    }
                };
                conDialog.createNewDialog().show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if(isDrawerOpen()) {
            drawerLayout.closeDrawer(navDrawer);
        } else if (pager == null || pager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            pager.setCurrentItem(pager.getCurrentItem() - 1);
        }
    }

    private void showDrawerPreferencesDialog() {
        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        String[] navLabels = new String[NAV_DRAWER_TAGS.length];
        final boolean[] checked = new boolean[NAV_DRAWER_TAGS.length];
        for (int i = 0; i < NAV_DRAWER_TAGS.length; i++) {
            String tag = NAV_DRAWER_TAGS[i];
            navLabels[i] = navAdapter.getLabel(tag);
            if (!hiddenDrawerItems.contains(tag)) {
                checked[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.drawer_preferences);
        builder.setMultiChoiceItems(navLabels, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                hiddenDrawerItems.remove(NAV_DRAWER_TAGS[which]);
            } else {
                hiddenDrawerItems.add(NAV_DRAWER_TAGS[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> UserPreferences.setHiddenDrawerItems(hiddenDrawerItems));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private DBReader.NavDrawerData navDrawerData;

    private void loadData() {
        disposable = Observable.fromCallable(DBReader::getNavDrawerData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    navDrawerData = result;
                    if (navAdapter != null) {
                        navAdapter.notifyDataSetChanged();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
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

    @Subscribe
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadData();
    }

    private final NavListAdapter.ItemAccess itemAccess = new NavListAdapter.ItemAccess() {
        @Override
        public int getCount() {
            if (navDrawerData != null) {
                return navDrawerData.feeds.size();
            } else {
                return 0;
            }
        }

        @Override
        public Feed getItem(int position) {
            if (navDrawerData != null && 0 <= position && position < navDrawerData.feeds.size()) {
                return navDrawerData.feeds.get(position);
            } else {
                return null;
            }
        }

        @Override
        public int getSelectedItemIndex() {
            return -1;
        }

        @Override
        public int getQueueSize() {
            return (navDrawerData != null) ? navDrawerData.queueSize : 0;
        }

        @Override
        public int getNumberOfNewItems() {
            return (navDrawerData != null) ? navDrawerData.numNewItems : 0;
        }

        @Override
        public int getNumberOfDownloadedItems() {
            return (navDrawerData != null) ? navDrawerData.numDownloadedItems : 0;
        }

        @Override
        public int getReclaimableItems() {
            return (navDrawerData != null) ? navDrawerData.reclaimableSpace : 0;
        }

        @Override
        public int getFeedCounter(long feedId) {
            return navDrawerData != null ? navDrawerData.feedCounters.get(feedId) : 0;
        }

        @Override
        public int getFeedCounterSum() {
            if(navDrawerData == null) {
                return 0;
            }
            int sum = 0;
            for(int counter : navDrawerData.feedCounters.values()) {
                sum += counter;
            }
            return sum;
        }
    };

    private static class MediaplayerInfoPagerAdapter extends FragmentStatePagerAdapter {
        private static final String TAG = "MPInfoPagerAdapter";
        private Activity activity;

        public MediaplayerInfoPagerAdapter(FragmentManager fm, Activity a) {
            super(fm);
            activity = a;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            switch (position) {
                case POS_COVER:
                    return new CoverFragment();
                case POS_DESCR:
                    return new ItemDescriptionFragment();
                case POS_CHAPTERS:
                    return new ChaptersFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            PlaybackController controller = new PlaybackController(this.activity, false);
            if (controller.getMedia() == null  ||
                    controller.getMedia().getChapters() == null  ||
                    controller.getMedia().getChapters().size() == 0) {
                return NUM_CONTENT_FRAGMENTS - 1;
            } else {
                return NUM_CONTENT_FRAGMENTS;
            }
        }
    }
}
