package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChapterListAdapter;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;
import de.danoeh.antennapod.preferences.PreferenceController;

/**
 * Activity for playing audio files.
 */
public class AudioplayerActivity extends MediaplayerActivity implements ItemDescriptionFragment.ItemDescriptionFragmentCallback,
        NavDrawerActivity {
    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int POS_CHAPTERS = 2;
    private static final int NUM_CONTENT_FRAGMENTS = 3;
    private static final int POS_NONE = -1;

    final String TAG = "AudioplayerActivity";
    private static final String PREFS = "AudioPlayerActivityPreferences";
    private static final String PREF_KEY_SELECTED_FRAGMENT_POSITION = "selectedFragmentPosition";
    private static final String PREF_PLAYABLE_ID = "playableId";

    private DrawerLayout drawerLayout;
    private NavListAdapter navAdapter;
    private ListView navList;
    private View navDrawer;
    private ActionBarDrawerToggle drawerToggle;

    private Fragment[] detachedFragments;

    private CoverFragment coverFragment;
    private ItemDescriptionFragment descriptionFragment;
    private ListFragment chapterFragment;

    private Fragment currentlyShownFragment;
    private int currentlyShownPosition = -1;
    private int lastShownPosition = POS_NONE;
    /**
     * Used if onResume was called without loadMediaInfo.
     */
    private int savedPosition = -1;

    private TextView txtvTitle;
    private Button butPlaybackSpeed;
    private ImageButton butNavChaptersShownotes;
    private ImageButton butShowCover;

    private void resetFragmentView() {
        FragmentTransaction fT = getSupportFragmentManager().beginTransaction();

        if (coverFragment != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Removing cover fragment");
            fT.remove(coverFragment);
        }
        if (descriptionFragment != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Removing description fragment");
            fT.remove(descriptionFragment);
        }
        if (chapterFragment != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Removing chapter fragment");
            fT.remove(chapterFragment);
        }
        if (currentlyShownFragment != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Removing currently shown fragment");
            fT.remove(currentlyShownFragment);
        }
        for (int i = 0; i < detachedFragments.length; i++) {
            Fragment f = detachedFragments[i];
            if (f != null) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Removing detached fragment");
                fT.remove(f);
            }
        }
        fT.commit();
        currentlyShownFragment = null;
        coverFragment = null;
        descriptionFragment = null;
        chapterFragment = null;
        currentlyShownPosition = -1;
        detachedFragments = new Fragment[NUM_CONTENT_FRAGMENTS];
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onStop");
        cancelLoadTask();
        EventDistributor.getInstance().unregister(contentUpdate);

    }

    @Override
    protected void chooseTheme() {
        setTheme(UserPreferences.getNoTitleTheme());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detachedFragments = new Fragment[NUM_CONTENT_FRAGMENTS];
    }

    private void savePreferences() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Saving preferences");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (currentlyShownPosition >= 0 && controller != null
                && controller.getMedia() != null) {
            editor.putInt(PREF_KEY_SELECTED_FRAGMENT_POSITION,
                    currentlyShownPosition);
            editor.putString(PREF_PLAYABLE_ID, controller.getMedia()
                    .getIdentifier().toString());
        } else {
            editor.putInt(PREF_KEY_SELECTED_FRAGMENT_POSITION, -1);
            editor.putString(PREF_PLAYABLE_ID, "");
        }
        editor.commit();

        savedPosition = currentlyShownPosition;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // super.onSaveInstanceState(outState); would cause crash
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onSaveInstanceState");

    }

    @Override
    protected void onPause() {
        savePreferences();
        resetFragmentView();
        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreFromPreferences();
    }

    /**
     * Tries to restore the selected fragment position from the Activity's
     * preferences.
     *
     * @return true if restoreFromPrefernces changed the activity's state
     */
    private boolean restoreFromPreferences() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Restoring instance state");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int savedPosition = prefs.getInt(PREF_KEY_SELECTED_FRAGMENT_POSITION,
                -1);
        String playableId = prefs.getString(PREF_PLAYABLE_ID, "");

        if (savedPosition != -1
                && controller != null
                && controller.getMedia() != null
                && controller.getMedia().getIdentifier().toString()
                .equals(playableId)) {
            switchToFragment(savedPosition);
            return true;
        } else if (controller == null || controller.getMedia() == null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG,
                        "Couldn't restore from preferences: controller or media was null");
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG,
                        "Couldn't restore from preferences: savedPosition was -1 or saved identifier and playable identifier didn't match.\nsavedPosition: "
                                + savedPosition + ", id: " + playableId
                );

        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (StringUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            Intent intent = getIntent();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Received VIEW intent: "
                        + intent.getData().getPath());
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
        if (savedPosition != -1) {
            switchToFragment(savedPosition);
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
        if (BuildConfig.DEBUG)
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

    /**
     * Changes the currently displayed fragment.
     *
     * @param pos Must be POS_COVER, POS_DESCR, or POS_CHAPTERS
     */
    private void switchToFragment(int pos) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Switching contentView to position " + pos);
        if (currentlyShownPosition != pos && controller != null) {
            Playable media = controller.getMedia();
            if (media != null) {
                FragmentTransaction ft = getSupportFragmentManager()
                        .beginTransaction();
                if (currentlyShownFragment != null) {
                    detachedFragments[currentlyShownPosition] = currentlyShownFragment;
                    ft.detach(currentlyShownFragment);
                }
                switch (pos) {
                    case POS_COVER:
                        if (coverFragment == null) {
                            Log.i(TAG, "Using new coverfragment");
                            coverFragment = CoverFragment.newInstance(media);
                        }
                        currentlyShownFragment = coverFragment;
                        break;
                    case POS_DESCR:
                        if (descriptionFragment == null) {
                            descriptionFragment = ItemDescriptionFragment
                                    .newInstance(media, true, true);
                        }
                        currentlyShownFragment = descriptionFragment;
                        break;
                    case POS_CHAPTERS:
                        if (chapterFragment == null) {
                            chapterFragment = new ListFragment() {

                                @Override
                                public void onViewCreated(View view, Bundle savedInstanceState) {
                                    super.onViewCreated(view, savedInstanceState);
                                    // add padding
                                    final ListView lv = getListView();
                                    lv.setClipToPadding(false);
                                    final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
                                    lv.setPadding(0, vertPadding, 0, vertPadding);
                                }
                            };
                            chapterFragment.setListAdapter(new ChapterListAdapter(
                                    AudioplayerActivity.this, 0, media
                                    .getChapters(), media, new ChapterListAdapter.Callback() {
                                @Override
                                public void onPlayChapterButtonClicked(int position) {
                                    Chapter chapter = (Chapter)
                                            chapterFragment.getListAdapter().getItem(position);
                                    controller.seekToChapter(chapter);
                                }
                            }
                            ));
                        }
                        currentlyShownFragment = chapterFragment;
                        break;
                }
                if (currentlyShownFragment != null) {
                    lastShownPosition = currentlyShownPosition;
                    currentlyShownPosition = pos;
                    if (detachedFragments[pos] != null) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Reattaching fragment at position "
                                    + pos);
                        ft.attach(detachedFragments[pos]);
                    } else {
                        ft.add(R.id.contentView, currentlyShownFragment);
                    }
                    ft.disallowAddToBackStack();
                    ft.commit();
                    updateNavButtonDrawable();
                }
            }
        }
    }

    /**
     * Switches to the fragment that was displayed before the current one or the description fragment
     * if no fragment was previously displayed.
     */
    public void switchToLastFragment() {
        if (lastShownPosition != POS_NONE) {
            switchToFragment(lastShownPosition);
        } else {
            switchToFragment(POS_DESCR);
        }
    }

    private void updateNavButtonDrawable() {

        final int[] buttonTexts = new int[]{R.string.show_shownotes_label,
                R.string.show_chapters_label};

        final TypedArray drawables = obtainStyledAttributes(new int[]{
                R.attr.navigation_shownotes, R.attr.navigation_chapters});
        final Playable media = controller.getMedia();
        if (butNavChaptersShownotes != null && butShowCover != null && media != null) {

            butNavChaptersShownotes.setTag(R.id.imageloader_key, null);
            setNavButtonVisibility();
            switch (currentlyShownPosition) {
                case POS_COVER:
                    butShowCover.setVisibility(View.GONE);
                    if (lastShownPosition == POS_CHAPTERS) {
                        butNavChaptersShownotes.setImageDrawable(drawables.getDrawable(1));
                        butNavChaptersShownotes.setContentDescription(getString(buttonTexts[1]));
                    } else {
                        butNavChaptersShownotes.setImageDrawable(drawables.getDrawable(0));
                        butNavChaptersShownotes.setContentDescription(getString(buttonTexts[0]));
                    }
                    break;
                case POS_DESCR:
                    butShowCover.setVisibility(View.VISIBLE);
                    butNavChaptersShownotes.setImageDrawable(drawables.getDrawable(1));
                    butNavChaptersShownotes.setContentDescription(getString(buttonTexts[1]));
                    break;
                case POS_CHAPTERS:
                    butShowCover.setVisibility(View.VISIBLE);
                    butNavChaptersShownotes.setImageDrawable(drawables.getDrawable(0));
                    butNavChaptersShownotes.setContentDescription(getString(buttonTexts[0]));
                    break;
            }
        }
        drawables.recycle();
    }

    @Override
    protected void setupGUI() {
        super.setupGUI();
        resetFragmentView();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navList = (ListView) findViewById(R.id.nav_list);
        navDrawer = findViewById(R.id.nav_layout);
        butPlaybackSpeed = (Button) findViewById(R.id.butPlaybackSpeed);
        butNavChaptersShownotes = (ImageButton) findViewById(R.id.butNavChaptersShownotes);
        butShowCover = (ImageButton) findViewById(R.id.butCover);
        txtvTitle = (TextView) findViewById(R.id.txtvTitle);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            CharSequence currentTitle = getSupportActionBar().getTitle();

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                currentTitle = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(R.string.app_name);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getSupportActionBar().setTitle(currentTitle);
                supportInvalidateOptionsMenu();
            }
        };

        drawerToggle.setDrawerIndicatorEnabled(false);
        drawerLayout.setDrawerListener(drawerToggle);

        navAdapter = new NavListAdapter(itemAccess, this);
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int viewType = parent.getAdapter().getItemViewType(position);
                if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER) {
                    int relPos = (viewType == NavListAdapter.VIEW_TYPE_NAV) ? position : position - NavListAdapter.SUBSCRIPTION_OFFSET;
                    Intent intent = new Intent(AudioplayerActivity.this, MainActivity.class);
                    intent.putExtra(MainActivity.EXTRA_NAV_TYPE, viewType);
                    intent.putExtra(MainActivity.EXTRA_NAV_INDEX, relPos);
                    startActivity(intent);
                }
                drawerLayout.closeDrawer(navDrawer);
            }
        });
        drawerToggle.syncState();

        findViewById(R.id.nav_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(navDrawer);
                startActivity(new Intent(AudioplayerActivity.this, PreferenceController.getPreferenceActivity()));
            }
        });

        butNavChaptersShownotes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentlyShownPosition == POS_CHAPTERS) {
                    switchToFragment(POS_DESCR);
                } else if (currentlyShownPosition == POS_DESCR) {
                    switchToFragment(POS_CHAPTERS);
                } else if (currentlyShownPosition == POS_COVER) {
                    switchToLastFragment();
                }
            }
        });

        butShowCover.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToFragment(POS_COVER);
            }
        });

        butPlaybackSpeed.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (controller != null && controller.canSetPlaybackSpeed()) {
                    String[] availableSpeeds = UserPreferences
                            .getPlaybackSpeedArray();
                    String currentSpeed = UserPreferences.getPlaybackSpeed();

                    // Provide initial value in case the speed list has changed
                    // out from under us
                    // and our current speed isn't in the new list
                    String newSpeed;
                    if (availableSpeeds.length > 0) {
                        newSpeed = availableSpeeds[0];
                    } else {
                        newSpeed = "1.0";
                    }

                    for (int i = 0; i < availableSpeeds.length; i++) {
                        if (availableSpeeds[i].equals(currentSpeed)) {
                            if (i == availableSpeeds.length - 1) {
                                newSpeed = availableSpeeds[0];
                            } else {
                                newSpeed = availableSpeeds[i + 1];
                            }
                            break;
                        }
                    }
                    UserPreferences.setPlaybackSpeed(newSpeed);
                    controller.setPlaybackSpeed(Float.parseFloat(newSpeed));
                }
            }
        });

        butPlaybackSpeed.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                VariableSpeedDialog.showDialog(AudioplayerActivity.this);
                return true;
            }
        });
    }

    private void setNavButtonVisibility() {
        if (butNavChaptersShownotes != null) {
            if (controller != null) {
                Playable media = controller.getMedia();
                if (media != null) {
                    if (media.getChapters() != null || currentlyShownPosition == POS_COVER) {
                        butNavChaptersShownotes.setVisibility(View.VISIBLE);
                        return;
                    }
                }
            }
            butNavChaptersShownotes.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onPlaybackSpeedChange() {
        super.onPlaybackSpeedChange();
        updateButPlaybackSpeed();
    }

    private void updateButPlaybackSpeed() {
        if (controller != null && controller.canSetPlaybackSpeed()) {
            butPlaybackSpeed.setText(UserPreferences.getPlaybackSpeed());
        }
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
        final Playable media = controller.getMedia();
        if (media == null) {
            return false;
        }
        txtvTitle.setText(media.getEpisodeTitle());
        getSupportActionBar().setTitle("");
        Picasso.with(this)
                .load(media.getImageUri())
                .fit()
                .into(butShowCover);

        setNavButtonVisibility();

        if (currentlyShownPosition == -1) {
            if (!restoreFromPreferences()) {
                switchToFragment(POS_COVER);
            }
        }
        if (currentlyShownFragment instanceof AudioplayerContentFragment) {
            ((AudioplayerContentFragment) currentlyShownFragment)
                    .onDataSetChanged(media);
        }

        if (controller == null
                || !controller.canSetPlaybackSpeed()) {
            butPlaybackSpeed.setVisibility(View.GONE);
        } else {
            butPlaybackSpeed.setVisibility(View.VISIBLE);
        }

        updateButPlaybackSpeed();
        return true;
    }

    public void notifyMediaPositionChanged() {
        if (chapterFragment != null) {
            ArrayAdapter<SimpleChapter> adapter = (ArrayAdapter<SimpleChapter>) chapterFragment
                    .getListAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_VIDEO) {
            if (BuildConfig.DEBUG)
                Log.d(TAG,
                        "ReloadNotification received, switching to Videoplayer now");
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

    @Override
    public PlaybackController getPlaybackController() {
        return controller;
    }

    @Override
    public boolean isDrawerOpen() {
        return drawerLayout != null && navDrawer != null && drawerLayout.isDrawerOpen(navDrawer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!MenuItemUtils.isActivityDrawerOpen(this)) {
            return super.onCreateOptionsMenu(menu);
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!MenuItemUtils.isActivityDrawerOpen(this)) {
            return super.onPrepareOptionsMenu(menu);
        } else {
            return false;
        }
    }

    public interface AudioplayerContentFragment {
        public void onDataSetChanged(Playable media);
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

    private DBReader.NavDrawerData navDrawerData;
    private AsyncTask<Void, Void, DBReader.NavDrawerData> loadTask;

    private void loadData() {
        loadTask = new AsyncTask<Void, Void, DBReader.NavDrawerData>() {
            @Override
            protected DBReader.NavDrawerData doInBackground(Void... params) {
                return DBReader.getNavDrawerData(AudioplayerActivity.this);
            }

            @Override
            protected void onPostExecute(DBReader.NavDrawerData result) {
                super.onPostExecute(result);
                navDrawerData = result;
                if (navAdapter != null) {
                    navAdapter.notifyDataSetChanged();
                }
            }
        };
        loadTask.execute();
    }

    private void cancelLoadTask() {
        if (loadTask != null) {
            loadTask.cancel(true);
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EventDistributor.FEED_LIST_UPDATE & arg) != 0) {
                if (BuildConfig.DEBUG)
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
            if (navDrawerData != null && position < navDrawerData.feeds.size()) {
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
        public int getNumberOfUnreadItems() {
            return (navDrawerData != null) ? navDrawerData.numUnreadItems : 0;
        }
    };
}
