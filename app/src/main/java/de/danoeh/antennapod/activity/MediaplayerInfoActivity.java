package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.viewpagerindicator.CirclePageIndicator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.fragment.ChaptersFragment;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;
import de.greenrobot.event.EventBus;
import rx.Subscription;

/**
 * Activity for playing files that do not require a video surface.
 */
public abstract class MediaplayerInfoActivity extends MediaplayerActivity implements NavDrawerActivity {

    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int POS_CHAPTERS = 2;
    private static final int NUM_CONTENT_FRAGMENTS = 3;

    final String TAG = "MediaplayerInfoActivity";
    private static final String PREFS = "AudioPlayerActivityPreferences";
    private static final String PREF_KEY_SELECTED_FRAGMENT_POSITION = "selectedFragmentPosition";

    protected Button butPlaybackSpeed;
    protected ImageButton butCastDisconnect;
    private int mPosition = -1;

    private Playable media;
    private ViewPager pager;
    private MediaplayerInfoPagerAdapter pagerAdapter;

    private Subscription subscription;

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        if(pagerAdapter != null) {
            pagerAdapter.setController(null);
        }
        if(subscription != null) {
            subscription.unsubscribe();
        }
        saveCurrentFragment();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // don't risk creating memory leaks
        pager = null;
        pagerAdapter = null;
    }

    @Override
    protected void chooseTheme() {
        getActivity().setTheme(UserPreferences.getNoTitleTheme());
    }

    protected void saveCurrentFragment() {
        if(pager == null) {
            return;
        }
        Log.d(TAG, "Saving preferences");
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, pager.getCurrentItem())
                .commit();
    }

    private void loadLastFragment() {
        Log.d(TAG, "Restoring instance state");
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, -1);
        pager.setCurrentItem(lastPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(pagerAdapter != null && controller != null && controller.getMedia() != media) {
            media = controller.getMedia();
            pagerAdapter.onMediaChanged(media);
            pagerAdapter.setController(controller);
        }
        DBTasks.checkShouldRefreshFeeds(getActivity().getApplicationContext());

        EventBus.getDefault().register(this);
    }
/*
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
*/
    @Override
    protected void onAwaitingVideoSurface() {
        Log.d(TAG, "onAwaitingVideoSurface was called in audio player -> switching to video player");
        //startActivity(new Intent(getContext(), VideoplayerActivity.class));
    }

    @Override
    protected void postStatusMsg(int resId, boolean showToast) {
        if (resId == R.string.player_preparing_msg
                || resId == R.string.player_seeking_msg
                || resId == R.string.player_buffering_msg) {
            // TODO Show progress bar here
        }
        if (showToast) {
            Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void clearStatusMsg() {
        // TODO Hide progress bar here
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        butPlaybackSpeed = (Button) root.findViewById(R.id.butPlaybackSpeed);
        butCastDisconnect = (ImageButton) root.findViewById(R.id.butCastDisconnect);

        pager = (ViewPager) root.findViewById(R.id.pager);
        pagerAdapter = new MediaplayerInfoPagerAdapter(getActivity().getSupportFragmentManager(), media);
        pagerAdapter.setController(controller);
        pager.setAdapter(pagerAdapter);
        CirclePageIndicator pageIndicator = (CirclePageIndicator) root.findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(pager);
        loadLastFragment();
        pager.onSaveInstanceState();

        return root;
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
        if(controller != null && controller.getMedia() != media) {
            media = controller.getMedia();
            pagerAdapter.onMediaChanged(media);
        }
        return true;
    }

    public void notifyMediaPositionChanged() {
        if(pagerAdapter == null) {
            return;
        }
        ChaptersFragment chaptersFragment = pagerAdapter.getChaptersFragment();
        if(chaptersFragment != null) {
            ChaptersListAdapter adapter = (ChaptersListAdapter) chaptersFragment.getListAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_VIDEO) {
            Log.d(TAG, "ReloadNotification received, switching to Videoplayer now");
            getActivity().finish();
            startActivity(new Intent(getContext(), VideoplayerActivity.class));

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
    protected int getContentViewResourceId() {
        return R.layout.mediaplayerinfo_activity;
    }
/*
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
*/

    public void onEventMainThread(MessageEvent event) {
        /*Log.d(TAG, "onEvent(" + event + ")");
        View parentLayout = findViewById(R.id.drawer_layout);
        Snackbar snackbar = Snackbar.make(parentLayout, event.message, Snackbar.LENGTH_SHORT);
        if (event.action != null) {
            snackbar.setAction(getString(R.string.undo), v -> {
                event.action.run();
            });
        }
        snackbar.show();*/
    }


    public interface MediaplayerInfoContentFragment {
        void onMediaChanged(Playable media);
    }

    private static class MediaplayerInfoPagerAdapter extends FragmentStatePagerAdapter {

        private static final String TAG = "MPInfoPagerAdapter";

        private Playable media;
        private PlaybackController controller;

        public MediaplayerInfoPagerAdapter(FragmentManager fm, Playable media) {
            super(fm);
            this.media = media;
        }

        private CoverFragment coverFragment;
        private ItemDescriptionFragment itemDescriptionFragment;
        private ChaptersFragment chaptersFragment;

        public void onMediaChanged(Playable media) {
            Log.d(TAG, "media changing to " + ((media != null) ? media.getEpisodeTitle() : "null"));
            this.media = media;
            if(coverFragment != null) {
                coverFragment.onMediaChanged(media);
            }
            if(itemDescriptionFragment != null) {
                itemDescriptionFragment.onMediaChanged(media);
            }
            if(chaptersFragment != null) {
                chaptersFragment.onMediaChanged(media);
            }
        }

        public void setController(PlaybackController controller) {
            this.controller = controller;
            if(chaptersFragment != null) {
                chaptersFragment.setController(controller);
            }
        }

        @Nullable
        public ChaptersFragment getChaptersFragment() {
            return chaptersFragment;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            switch (position) {
                case POS_COVER:
                    if(coverFragment == null) {
                        coverFragment = CoverFragment.newInstance(media);
                    }
                    return coverFragment;
                case POS_DESCR:
                    if(itemDescriptionFragment == null) {
                        itemDescriptionFragment = ItemDescriptionFragment.newInstance(media, true, true);
                    }
                    return itemDescriptionFragment;
                case POS_CHAPTERS:
                    if(chaptersFragment == null) {
                        chaptersFragment = ChaptersFragment.newInstance(media);
                        chaptersFragment.setController(controller);
                    }
                    return chaptersFragment;
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
