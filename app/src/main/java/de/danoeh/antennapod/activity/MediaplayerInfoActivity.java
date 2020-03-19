package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.fragment.ChaptersFragment;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.fragment.NavDrawerFragment;
import de.danoeh.antennapod.view.PagerIndicatorView;
import de.danoeh.antennapod.view.PlaybackSpeedIndicatorView;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * Activity for playing files that do not require a video surface.
 */
public abstract class MediaplayerInfoActivity extends MediaplayerActivity {

    private static final String TAG = "MediaplayerInfoActivity";

    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int POS_CHAPTERS = 2;
    private static final int NUM_CONTENT_FRAGMENTS = 3;

    private static final String PREFS = "AudioPlayerActivityPreferences";
    private static final String PREF_KEY_SELECTED_FRAGMENT_POSITION = "selectedFragmentPosition";

    PlaybackSpeedIndicatorView butPlaybackSpeed;
    TextView txtvPlaybackSpeed;
    private DrawerLayout drawerLayout;
    private View navDrawer;
    private ActionBarDrawerToggle drawerToggle;
    private ViewPager pager;
    private PagerIndicatorView pageIndicator;
    private MediaplayerInfoPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportPostponeEnterTransition();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        saveCurrentFragment();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // don't risk creating memory leaks
        drawerLayout = null;
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
        if (pager == null) {
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
        if (drawerToggle != null) {
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
        drawerLayout = findViewById(R.id.drawer_layout);
        navDrawer = findViewById(R.id.navDrawerFragment);
        butPlaybackSpeed = findViewById(R.id.butPlaybackSpeed);
        txtvPlaybackSpeed = findViewById(R.id.txtvPlaybackSpeed);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.navDrawerFragment, new NavDrawerFragment(), NavDrawerFragment.TAG)
                .commit();

        pager = findViewById(R.id.pager);
        pager.setOffscreenPageLimit(3);
        pagerAdapter = new MediaplayerInfoPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pageIndicator = findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(pager);
        pageIndicator.setOnClickListener(v
                -> pager.setCurrentItem((pager.getCurrentItem() + 1) % pager.getChildCount()));
        loadLastFragment();
        pager.onSaveInstanceState();

        pager.post(this::supportStartPostponedEnterTransition);
    }

    @Override
    boolean loadMediaInfo() {
        if (controller != null && controller.getMedia() != null) {
            List<Chapter> chapters = controller.getMedia().getChapters();
            boolean hasChapters = chapters != null && !chapters.isEmpty();
            pageIndicator.setDisabledPage(hasChapters ? -1 : 2);
        }
        return super.loadMediaInfo();
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
    public void onBackPressed() {
        if (isDrawerOpen()) {
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

    private static class MediaplayerInfoPagerAdapter extends FragmentStatePagerAdapter {
        private static final String TAG = "MPInfoPagerAdapter";

        public MediaplayerInfoPagerAdapter(FragmentManager fm) {
            super(fm);
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
            return NUM_CONTENT_FRAGMENTS;
        }
    }
}
