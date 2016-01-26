package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.viewpagerindicator.CirclePageIndicator;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.ChaptersFragment;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;
import de.danoeh.antennapod.preferences.PreferenceController;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Activity for playing audio files.
 */
public class AudioplayerActivity extends MediaplayerActivity implements NavDrawerActivity {

    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int POS_CHAPTERS = 2;
    private static final int NUM_CONTENT_FRAGMENTS = 3;

    final String TAG = "AudioplayerActivity";
    private static final String PREFS = "AudioPlayerActivityPreferences";
    private static final String PREF_KEY_SELECTED_FRAGMENT_POSITION = "selectedFragmentPosition";

    public static final String[] NAV_DRAWER_TAGS = {
            QueueFragment.TAG,
            EpisodesFragment.TAG,
            DownloadsFragment.TAG,
            PlaybackHistoryFragment.TAG,
            AddFeedFragment.TAG
    };

    private AtomicBoolean isSetup = new AtomicBoolean(false);

    private DrawerLayout drawerLayout;
    private NavListAdapter navAdapter;
    private ListView navList;
    private View navDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private int mPosition = -1;

    private Playable media;
    private ViewPager mPager;
    private AudioplayerPagerAdapter mPagerAdapter;

    private Subscription subscription;

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        if(subscription != null) {
            subscription.unsubscribe();
        }
        EventDistributor.getInstance().unregister(contentUpdate);
        saveCurrentFragment();
    }

    @Override
    protected void chooseTheme() {
        setTheme(UserPreferences.getNoTitleTheme());
    }

    private void saveCurrentFragment() {
        if(mPager == null) {
            return;
        }

        Log.d(TAG, "Saving preferences");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, mPager.getCurrentItem())
                .commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void loadLastFragment() {
        Log.d(TAG, "Restoring instance state");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, -1);
        mPager.setCurrentItem(lastPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            Intent intent = getIntent();
            Log.d(TAG, "Received VIEW intent: " + intent.getData().getPath());
            ExternalMedia media = new ExternalMedia(intent.getData().getPath(),
                    MediaType.AUDIO);
            Intent launchIntent = new Intent(this, PlaybackService.class);
            launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
            launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
                    true);
            launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM, false);
            launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY,
                    true);
            startService(launchIntent);
        }
        if(mPagerAdapter != null && controller != null && controller.getMedia() != media) {
            media = controller.getMedia();
            mPagerAdapter.notifyDataSetChanged();
        }


        EventDistributor.getInstance().register(contentUpdate);
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
    protected void postStatusMsg(int resId) {
        if (resId == R.string.player_preparing_msg
                || resId == R.string.player_seeking_msg
                || resId == R.string.player_buffering_msg) {
            // TODO Show progress bar here
        }
    }

    @Override
    protected void clearStatusMsg() {
        // TODO Hide progress bar here
    }


    @Override
    protected void setupGUI() {
        if(isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.shadow).setVisibility(View.GONE);
            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appBar);
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            appBarLayout.setElevation(px);
        }
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navList = (ListView) findViewById(R.id.nav_list);
        navDrawer = findViewById(R.id.nav_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerLayout.setDrawerListener(drawerToggle);

        navAdapter = new NavListAdapter(itemAccess, this);
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener((parent, view, position, id) -> {
            int viewType = parent.getAdapter().getItemViewType(position);
            if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER) {
                Intent intent = new Intent(AudioplayerActivity.this, MainActivity.class);
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
            startActivity(new Intent(AudioplayerActivity.this, PreferenceController.getPreferenceActivity()));
        });

        mPager = (ViewPager) findViewById(R.id.pager);
        if(mPager.getAdapter() == null) {
            mPagerAdapter = new AudioplayerPagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            CirclePageIndicator pageIndicator = (CirclePageIndicator) findViewById(R.id.page_indicator);
            pageIndicator.setViewPager(mPager);
            loadLastFragment();
        }
        mPager.onSaveInstanceState();
    }

    @Override
    protected void onPositionObserverUpdate() {
        super.onPositionObserverUpdate();
        notifyMediaPositionChanged();
    }

    @Override
    protected boolean loadMediaInfo() {
        if (!super.loadMediaInfo()) {
            return false;
        }
        if(controller.getMedia() != media) {
            media = controller.getMedia();
            mPagerAdapter.notifyDataSetChanged();
        }
        return true;
    }

    public void notifyMediaPositionChanged() {
        ListFragment chapterFragment = (ListFragment) mPagerAdapter.getItem(POS_CHAPTERS);
        ChaptersListAdapter adapter = (ChaptersListAdapter) chapterFragment.getListAdapter();
        if(adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
        postStatusMsg(R.string.player_buffering_msg);
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
        return R.layout.audioplayer_activity;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
            case R.id.mark_all_seen_item:
                DBWriter.markFeedSeen(feed.getId());
                return true;
            case R.id.mark_all_read_item:
                DBWriter.markFeedRead(feed.getId());
                return true;
            case R.id.remove_item:
                final FeedRemover remover = new FeedRemover(this, feed) {
                    @Override
                    protected void onPostExecute(Void result) {
                        super.onPostExecute(result);
                    }
                };
                ConfirmationDialog conDialog = new ConfirmationDialog(this,
                        R.string.remove_feed_label,
                        R.string.feed_delete_confirmation_msg) {
                    @Override
                    public void onConfirmButtonPressed(
                            DialogInterface dialog) {
                        dialog.dismiss();
                        if (controller != null) {
                            Playable playable = controller.getMedia();
                            if (playable != null && playable instanceof FeedMedia) {
                                FeedMedia media = (FeedMedia) playable;
                                if (media.getItem().getFeed().getId() == feed.getId()) {
                                    Log.d(TAG, "Currently playing episode is about to be deleted, skipping");
                                    remover.skipOnCompletion = true;
                                    if(controller.getStatus() == PlayerStatus.PLAYING) {
                                        sendBroadcast(new Intent(
                                                PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE));
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
        } else if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    public void showDrawerPreferencesDialog() {
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
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            UserPreferences.setHiddenDrawerItems(hiddenDrawerItems);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private DBReader.NavDrawerData navDrawerData;

    private void loadData() {
        subscription = Observable.fromCallable(() -> DBReader.getNavDrawerData())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    navDrawerData = result;
                    if (navAdapter != null) {
                        navAdapter.notifyDataSetChanged();
                    }
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                });
    }



    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EventDistributor.FEED_LIST_UPDATE & arg) != 0) {
                Log.d(TAG, "Received contentUpdate Intent.");
                loadData();
            }
        }
    };

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
        public int getFeedCounter(long feedId) {
            return navDrawerData != null ? navDrawerData.feedCounters.get(feedId) : 0;
        }
    };

    public interface AudioplayerContentFragment {
        void onDataSetChanged(Playable media);
    }

    private class AudioplayerPagerAdapter extends FragmentStatePagerAdapter {

        public AudioplayerPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            switch (position) {
                case POS_COVER:
                    return CoverFragment.newInstance(media);
                case POS_DESCR:
                    return ItemDescriptionFragment.newInstance(media, true, true);
                case POS_CHAPTERS:
                    return ChaptersFragment.newInstance(media, controller);
                default:
                    return null;
            }
        }


        @Override
        public int getCount() {
            return NUM_CONTENT_FRAGMENTS;
        }
    }

}
