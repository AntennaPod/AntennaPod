package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.fragment.*;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.menuhandler.NavDrawerActivity;

import java.util.List;

/**
 * The activity that is shown when the user launches the app.
 */
public class MainActivity extends ActionBarActivity implements NavDrawerActivity{
    private static final String TAG = "MainActivity";
    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
            | EventDistributor.DOWNLOAD_QUEUED
            | EventDistributor.FEED_LIST_UPDATE
            | EventDistributor.UNREAD_ITEMS_UPDATE
            | EventDistributor.QUEUE_UPDATE;

    public static final String PREF_NAME = "MainActivityPrefs";
    public static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";

    public static final String EXTRA_NAV_INDEX = "nav_index";
    public static final String EXTRA_NAV_TYPE = "nav_type";
    public static final String EXTRA_FRAGMENT_ARGS = "fragment_args";

    public static final int POS_NEW = 0,
            POS_QUEUE = 1,
            POS_DOWNLOADS = 2,
            POS_HISTORY = 3,
            POS_ADD = 4;

    private ExternalPlayerFragment externalPlayerFragment;
    private DrawerLayout drawerLayout;

    private ListView navList;
    private NavListAdapter navAdapter;

    private ActionBarDrawerToggle drawerToggle;

    private CharSequence drawerTitle;
    private CharSequence currentTitle;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        StorageUtils.checkStorageAvailability(this);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        drawerTitle = currentTitle = getTitle();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navList = (ListView) findViewById(R.id.nav_list);

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.nav_drawer_toggle});
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, typedArray.getResourceId(0, 0), R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                currentTitle = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(drawerTitle);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getSupportActionBar().setTitle(currentTitle);
                supportInvalidateOptionsMenu();

            }
        };
        typedArray.recycle();

        drawerLayout.setDrawerListener(drawerToggle);
        FragmentManager fm = getSupportFragmentManager();

        FragmentTransaction transaction = fm.beginTransaction();

        Fragment mainFragment = fm.findFragmentByTag("main");
        if (mainFragment != null) {
            transaction.replace(R.id.main_view, mainFragment);
        } else {
            loadFragment(NavListAdapter.VIEW_TYPE_NAV, POS_NEW, null);
        }

        externalPlayerFragment = new ExternalPlayerFragment();
        transaction.replace(R.id.playerFragment, externalPlayerFragment);
        transaction.commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        navAdapter = new NavListAdapter(itemAccess, this);
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener(navListClickListener);

        checkFirstLaunch();
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_IS_FIRST_LAUNCH, true)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawerLayout.openDrawer(navList);
                }
            }, 1500);

            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(PREF_IS_FIRST_LAUNCH, false);
            edit.commit();
        }
    }

    public ActionBar getMainActivtyActionBar() {
        return getSupportActionBar();
    }

    public boolean isDrawerOpen() {
        return drawerLayout != null && navList != null && drawerLayout.isDrawerOpen(navList);
    }

    public List<Feed> getFeeds() {
        return (navDrawerData != null) ? navDrawerData.feeds : null;
    }

    private void loadFragment(int viewType, int relPos, Bundle args) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // clear back stack
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }

        FragmentTransaction fT = fragmentManager.beginTransaction();
        Fragment fragment = null;
        if (viewType == NavListAdapter.VIEW_TYPE_NAV) {
            switch (relPos) {
                case POS_NEW:
                    fragment = new NewEpisodesFragment();
                    break;
                case POS_QUEUE:
                    fragment = new QueueFragment();
                    break;
                case POS_DOWNLOADS:
                    fragment = new DownloadsFragment();
                    break;
                case POS_HISTORY:
                    fragment = new PlaybackHistoryFragment();
                    break;
                case POS_ADD:
                    fragment = new AddFeedFragment();
                    break;

            }
            currentTitle = getString(NavListAdapter.NAV_TITLES[relPos]);
            selectedNavListIndex = relPos;

        } else if (viewType == NavListAdapter.VIEW_TYPE_SUBSCRIPTION) {
            Feed feed = itemAccess.getItem(relPos);
            currentTitle = "";
            fragment = ItemlistFragment.newInstance(feed.getId());
            selectedNavListIndex = NavListAdapter.SUBSCRIPTION_OFFSET + relPos;

        }
        if (fragment != null) {
            if (args != null) {
                fragment.setArguments(args);
            }
            fT.replace(R.id.main_view, fragment, "main");
            fragmentManager.popBackStack();
        }
        fT.commit();
        getSupportActionBar().setTitle(currentTitle);
        if (navAdapter != null) {
            navAdapter.notifyDataSetChanged();
        }
    }

    public void loadNavFragment(int position, Bundle args) {
        loadFragment(NavListAdapter.VIEW_TYPE_NAV, position, args);
    }

    public void loadFeedFragment(long feedID) {
        if (navDrawerData != null) {
            for (int i = 0; i < navDrawerData.feeds.size(); i++) {
                if (navDrawerData.feeds.get(i).getId() == feedID) {
                    loadFragment(NavListAdapter.VIEW_TYPE_SUBSCRIPTION, i, null);
                    break;
                }
            }
        }
    }

    public void loadChildFragment(Fragment fragment) {
        Validate.notNull(fragment);
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.main_view, fragment, "main")
                .addToBackStack(null)
                .commit();
    }

    private AdapterView.OnItemClickListener navListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int viewType = parent.getAdapter().getItemViewType(position);
            if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER && position != selectedNavListIndex) {
                int relPos = (viewType == NavListAdapter.VIEW_TYPE_NAV) ? position : position - NavListAdapter.SUBSCRIPTION_OFFSET;
                loadFragment(viewType, relPos, null);
                selectedNavListIndex = position;
                navAdapter.notifyDataSetChanged();
            }
            drawerLayout.closeDrawer(navList);
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        if (savedInstanceState != null) {
            currentTitle = savedInstanceState.getString("title");
            if (!drawerLayout.isDrawerOpen(navList)) {
                getSupportActionBar().setTitle(currentTitle);
            }
            selectedNavListIndex = savedInstanceState.getInt("selectedNavIndex");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("title", getSupportActionBar().getTitle().toString());
        outState.putInt("selectedNavIndex", selectedNavListIndex);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
        EventDistributor.getInstance().register(contentUpdate);

        Intent intent = getIntent();
        if (navDrawerData != null && intent.hasExtra(EXTRA_NAV_INDEX) && intent.hasExtra(EXTRA_NAV_TYPE)) {
            handleNavIntent();
        }

        loadData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelLoadTask();
        EventDistributor.getInstance().unregister(contentUpdate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.show_preferences:
                startActivity(new Intent(this, PreferenceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    private DBReader.NavDrawerData navDrawerData;
    private AsyncTask<Void, Void, DBReader.NavDrawerData> loadTask;
    private int selectedNavListIndex = 0;

    private NavListAdapter.ItemAccess itemAccess = new NavListAdapter.ItemAccess() {
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
            return selectedNavListIndex;
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

    private void loadData() {
        cancelLoadTask();
        loadTask = new AsyncTask<Void, Void, DBReader.NavDrawerData>() {
            @Override
            protected DBReader.NavDrawerData doInBackground(Void... params) {
                return DBReader.getNavDrawerData(MainActivity.this);
            }

            @Override
            protected void onPostExecute(DBReader.NavDrawerData result) {
                super.onPostExecute(navDrawerData);
                boolean handleIntent = (navDrawerData == null);

                navDrawerData = result;
                navAdapter.notifyDataSetChanged();

                if (handleIntent) {
                    handleNavIntent();
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
            if ((EVENTS & arg) != 0) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Received contentUpdate Intent.");
                loadData();
            }
        }
    };

    private void handleNavIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_NAV_INDEX) && intent.hasExtra(EXTRA_NAV_TYPE)) {
            int index = intent.getIntExtra(EXTRA_NAV_INDEX, 0);
            int type = intent.getIntExtra(EXTRA_NAV_TYPE, NavListAdapter.VIEW_TYPE_NAV);
            Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
            loadFragment(type, index, args);
        }
        setIntent(new Intent(MainActivity.this, MainActivity.class)); // to avoid handling the intent twice when the configuration changes
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
