package de.danoeh.antennapod.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bumptech.glide.Glide;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.ProgressEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.ExternalPlayerFragment;
import de.danoeh.antennapod.fragment.ItemlistFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;
import de.danoeh.antennapod.preferences.PreferenceController;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * The activity that is shown when the user launches the app.
 */
public class MainActivity extends ActionBarActivity implements NavDrawerActivity {

    private static final String TAG = "MainActivity";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
            | EventDistributor.DOWNLOAD_QUEUED
            | EventDistributor.FEED_LIST_UPDATE
            | EventDistributor.UNREAD_ITEMS_UPDATE;

    public static final String PREF_NAME = "MainActivityPrefs";
    public static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";
    public static final String PREF_LAST_FRAGMENT_TAG = "prefMainActivityLastFragmentTag";

    public static final String EXTRA_NAV_TYPE = "nav_type";
    public static final String EXTRA_NAV_INDEX = "nav_index";
    public static final String EXTRA_FRAGMENT_TAG = "fragment_tag";
    public static final String EXTRA_FRAGMENT_ARGS = "fragment_args";

    public static final String SAVE_BACKSTACK_COUNT = "backstackCount";
    public static final String SAVE_TITLE = "title";

    public static final String[] NAV_DRAWER_TAGS = {
            QueueFragment.TAG,
            EpisodesFragment.TAG,
            DownloadsFragment.TAG,
            PlaybackHistoryFragment.TAG,
            AddFeedFragment.TAG
    };

    private Toolbar toolbar;
    private ExternalPlayerFragment externalPlayerFragment;
    private DrawerLayout drawerLayout;

    private View navDrawer;
    private ListView navList;
    private NavListAdapter navAdapter;
    private int mPosition = -1;

    private ActionBarDrawerToggle drawerToggle;

    private CharSequence currentTitle;

    private ProgressDialog pd;

    private Subscription subscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getNoTitleTheme());
        super.onCreate(savedInstanceState);
        StorageUtils.checkStorageAvailability(this);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(3.0f);

        currentTitle = getTitle();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navList = (ListView) findViewById(R.id.nav_list);
        navDrawer = findViewById(R.id.nav_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        if (savedInstanceState != null) {
            int backstackCount = savedInstanceState.getInt(SAVE_BACKSTACK_COUNT, 0);
            drawerToggle.setDrawerIndicatorEnabled(backstackCount == 0);
        }
        drawerLayout.setDrawerListener(drawerToggle);

        final FragmentManager fm = getSupportFragmentManager();

        fm.addOnBackStackChangedListener(() -> {
            drawerToggle.setDrawerIndicatorEnabled(fm.getBackStackEntryCount() == 0);
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        navAdapter = new NavListAdapter(itemAccess, this);
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener(navListClickListener);
        navList.setOnItemLongClickListener(newListLongClickListener);
        registerForContextMenu(navList);

        navAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                selectedNavListIndex = getSelectedNavListIndex();
            }
        });

        findViewById(R.id.nav_settings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(navDrawer);
            startActivity(new Intent(MainActivity.this, PreferenceController.getPreferenceActivity()));
        });

        FragmentTransaction transaction = fm.beginTransaction();

        Fragment mainFragment = fm.findFragmentByTag("main");
        if (mainFragment != null) {
            transaction.replace(R.id.main_view, mainFragment);
        } else {
            String lastFragment = getLastNavFragment();
            if (ArrayUtils.contains(NAV_DRAWER_TAGS, lastFragment)) {
                loadFragment(lastFragment, null);
            } else {
                try {
                    loadFeedFragmentById(Integer.valueOf(lastFragment), null);
                } catch (NumberFormatException e) {
                    // it's not a number, this happens if we removed
                    // a label from the NAV_DRAWER_TAGS
                    // give them a nice default...
                    loadFragment(QueueFragment.TAG, null);
                }
            }
        }
        externalPlayerFragment = new ExternalPlayerFragment();
        transaction.replace(R.id.playerFragment, externalPlayerFragment, ExternalPlayerFragment.TAG);
        transaction.commit();

        checkFirstLaunch();
    }

    private void saveLastNavFragment(String tag) {
        Log.d(TAG, "saveLastNavFragment(tag: " + tag +")");
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        if(tag != null) {
            edit.putString(PREF_LAST_FRAGMENT_TAG, tag);
        } else {
            edit.remove(PREF_LAST_FRAGMENT_TAG);
        }
        edit.commit();
    }

    private String getLastNavFragment() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastFragment = prefs.getString(PREF_LAST_FRAGMENT_TAG, QueueFragment.TAG);
        Log.d(TAG, "getLastNavFragment() -> " + lastFragment);
        return lastFragment;
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_IS_FIRST_LAUNCH, true)) {
            new Handler().postDelayed(() -> drawerLayout.openDrawer(navDrawer), 1500);

            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(PREF_IS_FIRST_LAUNCH, false);
            edit.commit();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

    public ActionBar getMainActivtyActionBar() {
        return getSupportActionBar();
    }

    public boolean isDrawerOpen() {
        return drawerLayout != null && navDrawer != null && drawerLayout.isDrawerOpen(navDrawer);
    }

    public List<Feed> getFeeds() {
        return (navDrawerData != null) ? navDrawerData.feeds : null;
    }

    public void loadFragment(int index, Bundle args) {
        Log.d(TAG, "loadFragment(index: " + index + ", args: " + args +")");
        if (index < navAdapter.getSubscriptionOffset()) {
            String tag = navAdapter.getTags().get(index);
            loadFragment(tag, args);
        } else {
            int pos = index - navAdapter.getSubscriptionOffset();
            loadFeedFragmentByPosition(pos, args);
        }
    }

    public void loadFragment(final String tag, Bundle args) {
        Log.d(TAG, "loadFragment(tag: " + tag + ", args: " + args + ")");
        Fragment fragment = null;
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
            default:
                // default to the queue
                fragment = new QueueFragment();
                break;
        }
        currentTitle = navAdapter.getLabel(tag);
        getSupportActionBar().setTitle(currentTitle);
        saveLastNavFragment(tag);
        if (args != null) {
            fragment.setArguments(args);
        }
        loadFragment(fragment);
    }

    private void loadFeedFragmentByPosition(int relPos, Bundle args) {
        if(relPos < 0) {
            return;
        }
        Feed feed = itemAccess.getItem(relPos);
        loadFeedFragmentById(feed.getId(), args);
    }

    public void loadFeedFragmentById(long feedId, Bundle args) {
        Fragment fragment = ItemlistFragment.newInstance(feedId);
        if(args != null) {
            fragment.setArguments(args);
        }
        saveLastNavFragment(String.valueOf(feedId));
        currentTitle = "";
        getSupportActionBar().setTitle(currentTitle);
        loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // clear back stack
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }
        FragmentTransaction t = fragmentManager.beginTransaction();
        t.replace(R.id.main_view, fragment, "main");
        fragmentManager.popBackStack();
        // TODO: we have to allow state loss here
        // since this function can get called from an AsyncTask which
        // could be finishing after our app has already committed state
        // and is about to get shutdown.  What we *should* do is
        // not commit anything in an AsyncTask, but that's a bigger
        // change than we want now.
        t.commitAllowingStateLoss();
        if (navAdapter != null) {
            navAdapter.notifyDataSetChanged();
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

    public void dismissChildFragment() {
        getSupportFragmentManager().popBackStack();
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    private int getSelectedNavListIndex() {
        String currentFragment = getLastNavFragment();
        if(currentFragment == null) {
            // should not happen, but better safe than sorry
            return -1;
        }
        int tagIndex = navAdapter.getTags().indexOf(currentFragment);
        if(tagIndex >= 0) {
            return tagIndex;
        } else if(ArrayUtils.contains(NAV_DRAWER_TAGS, currentFragment)) {
            // the fragment was just hidden
            return -1;
        } else { // last fragment was not a list, but a feed
            long feedId = Long.parseLong(currentFragment);
            if (navDrawerData != null) {
                List<Feed> feeds = navDrawerData.feeds;
                for (int i = 0; i < feeds.size(); i++) {
                    if (feeds.get(i).getId() == feedId) {
                        return i + navAdapter.getSubscriptionOffset();
                    }
                }
            }
            return -1;
        }
    }

    private AdapterView.OnItemClickListener navListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int viewType = parent.getAdapter().getItemViewType(position);
            if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER && position != selectedNavListIndex) {
                loadFragment(position, null);
            }
            drawerLayout.closeDrawer(navDrawer);
        }
    };

    private AdapterView.OnItemLongClickListener newListLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if(position < navAdapter.getTags().size()) {
                showDrawerPreferencesDialog();
                return true;
            } else {
                mPosition = position;
                return false;
            }
        }
    };


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        if (savedInstanceState != null) {
            currentTitle = savedInstanceState.getString(SAVE_TITLE);
            if (!drawerLayout.isDrawerOpen(navDrawer)) {
                getSupportActionBar().setTitle(currentTitle);
            }
            selectedNavListIndex = getSelectedNavListIndex();
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
        outState.putString(SAVE_TITLE, getSupportActionBar().getTitle().toString());
        outState.putInt(SAVE_BACKSTACK_COUNT, getSupportFragmentManager().getBackStackEntryCount());
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);

        Intent intent = getIntent();
        if (navDrawerData != null && intent.hasExtra(EXTRA_NAV_TYPE) &&
                (intent.hasExtra(EXTRA_NAV_INDEX) || intent.hasExtra(EXTRA_FRAGMENT_TAG))) {
            handleNavIntent();
        }

        loadData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        EventBus.getDefault().unregister(this);
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if(pd != null) {
            pd.dismiss();
        }
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
                dismissChildFragment();
            }
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
                        if(getSelectedNavListIndex() == position) {
                            loadFragment(EpisodesFragment.TAG, null);
                        }
                    }
                };
                ConfirmationDialog conDialog = new ConfirmationDialog(this,
                        R.string.remove_feed_label,
                        R.string.feed_delete_confirmation_msg) {
                    @Override
                    public void onConfirmButtonPressed(
                            DialogInterface dialog) {
                        dialog.dismiss();
                        remover.executeAsync();
                    }
                };
                conDialog.createNewDialog().show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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
        public int getNumberOfNewItems() {
            return (navDrawerData != null) ? navDrawerData.numNewItems : 0;
        }

        @Override
        public int getFeedCounter(long feedId) {
            return navDrawerData != null ? navDrawerData.feedCounters.get(feedId) : 0;
        }

    };

    private void loadData() {
        subscription = Observable.defer(() -> Observable.just(DBReader.getNavDrawerData()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    boolean handleIntent = (navDrawerData == null);

                    navDrawerData = result;
                    navAdapter.notifyDataSetChanged();

                    if (handleIntent) {
                        handleNavIntent();
                    }
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                });
    }

    public void onEvent(QueueEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        loadData();
    }

    public void onEventMainThread(ProgressEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        switch(event.action) {
            case START:
                pd = new ProgressDialog(this);
                pd.setMessage(event.message);
                pd.setIndeterminate(true);
                pd.setCancelable(false);
                pd.show();
                break;
            case END:
                if(pd != null) {
                    pd.dismiss();
                }
                break;
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EVENTS & arg) != 0) {
                Log.d(TAG, "Received contentUpdate Intent.");
                loadData();
            }
        }
    };

    private void handleNavIntent() {
        Log.d(TAG, "handleNavIntent()");
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_NAV_TYPE) &&
                intent.hasExtra(EXTRA_NAV_INDEX) || intent.hasExtra(EXTRA_FRAGMENT_TAG)) {
            int index = intent.getIntExtra(EXTRA_NAV_INDEX, -1);
            String tag = intent.getStringExtra(EXTRA_FRAGMENT_TAG);
            Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
            if (index >= 0) {
                loadFragment(index, args);
            } else if (tag != null) {
                loadFragment(tag, args);
            }
        }
        setIntent(new Intent(MainActivity.this, MainActivity.class)); // to avoid handling the intent twice when the configuration changes
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
