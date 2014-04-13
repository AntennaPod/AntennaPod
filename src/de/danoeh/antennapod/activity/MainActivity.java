package de.danoeh.antennapod.activity;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.*;
import android.widget.*;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.fragment.*;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.ThemeUtils;

import java.util.List;

/**
 * The activity that is shown when the user launches the app.
 */
public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
            | EventDistributor.DOWNLOAD_QUEUED
            | EventDistributor.FEED_LIST_UPDATE
            | EventDistributor.UNREAD_ITEMS_UPDATE;

    private ExternalPlayerFragment externalPlayerFragment;
    private DrawerLayout drawerLayout;

    private ListView navList;
    private NavListAdapter navAdapter;

    private ActionBarDrawerToggle drawerToogle;

    private CharSequence drawerTitle;
    private CharSequence currentTitle;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        StorageUtils.checkStorageAvailability(this);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        drawerTitle = currentTitle = getTitle();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navList = (ListView) findViewById(R.id.nav_list);

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.nav_drawer_toggle});
        drawerToogle = new ActionBarDrawerToggle(this, drawerLayout, typedArray.getResourceId(0, 0), R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
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

        drawerLayout.setDrawerListener(drawerToogle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        externalPlayerFragment = new ExternalPlayerFragment();
        transaction.replace(R.id.playerFragment, externalPlayerFragment);



        transaction.commit();

        Fragment mainFragment = fm.findFragmentByTag("main");
        if (mainFragment != null) {
            transaction = fm.beginTransaction();
            transaction.replace(R.id.main_view, mainFragment);
            transaction.commit();
        } else {
            loadFragment(NavListAdapter.VIEW_TYPE_NAV, 0);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        navAdapter = new NavListAdapter(itemAccess, this);
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener(navListClickListener);

        loadData();

    }

    public ActionBar getMainActivtyActionBar() {
        return getSupportActionBar();
    }

    private void loadFragment(int viewType, int relPos) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fT = fragmentManager.beginTransaction();
        Fragment fragment = null;
        if (viewType == NavListAdapter.VIEW_TYPE_NAV) {
            switch (relPos) {
                case 0:
                    fragment = new NewEpisodesFragment();
                    break;
                case 1:
                    fragment = new QueueFragment();
                    break;
                case 2:
                    fragment = new DownloadsFragment();
                    break;
                case 3:
                    fragment = new PlaybackHistoryFragment();
                    break;
            }
            currentTitle = getString(NavListAdapter.NAV_TITLES[relPos]);

        } else if (viewType == NavListAdapter.VIEW_TYPE_SUBSCRIPTION) {
            Feed feed = itemAccess.getItem(relPos);
            currentTitle = feed.getTitle();
            fragment = ItemlistFragment.newInstance(feed.getId());

        }
        if (fragment != null) {
            fT.replace(R.id.main_view, fragment, "main");
        }
        fT.commit();
    }

    private AdapterView.OnItemClickListener navListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int viewType = parent.getAdapter().getItemViewType(position);
            if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER && position != selectedNavListIndex) {
                int relPos = (viewType == NavListAdapter.VIEW_TYPE_NAV) ? position : position - NavListAdapter.SUBSCRIPTION_OFFSET;
                loadFragment(viewType, relPos);
                selectedNavListIndex = position;
                navAdapter.notifyDataSetChanged();
            }
            drawerLayout.closeDrawer(navList);
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToogle.syncState();
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
        drawerToogle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("title", currentTitle.toString());
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelLoadTask();
        EventDistributor.getInstance().unregister(contentUpdate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToogle.onOptionsItemSelected(item)) {
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
        boolean drawerOpen = drawerLayout.isDrawerOpen(navList);
        menu.findItem(R.id.search_item).setVisible(!drawerOpen);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search_item);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView == null) {
            MenuItemCompat.setActionView(searchItem, new SearchView(this));
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        }
        searchView.setIconifiedByDefault(true);

        SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    private List<Feed> feeds;
    private AsyncTask<Void, Void, List<Feed>> loadTask;
    private int selectedNavListIndex = 0;

    private ItemAccess itemAccess = new ItemAccess() {
        @Override
        public int getCount() {
            if (feeds != null) {
                return feeds.size();
            } else {
                return 0;
            }
        }

        @Override
        public Feed getItem(int position) {
            if (feeds != null && position < feeds.size()) {
                return feeds.get(position);
            } else {
                return null;
            }
        }

        @Override
        public int getSelectedItemIndex() {
            return selectedNavListIndex;
        }


    };

    private void loadData() {
        loadTask = new AsyncTask<Void, Void, List<Feed>>() {
            @Override
            protected List<Feed> doInBackground(Void... params) {
                return DBReader.getFeedList(MainActivity.this);
            }

            @Override
            protected void onPostExecute(List<Feed> result) {
                super.onPostExecute(result);
                feeds = result;
                navAdapter.notifyDataSetChanged();
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

    private static class NavListAdapter extends BaseAdapter {

        static final int VIEW_TYPE_COUNT = 3;
        static final int VIEW_TYPE_NAV = 0;
        static final int VIEW_TYPE_SECTION_DIVIDER = 1;
        static final int VIEW_TYPE_SUBSCRIPTION = 2;

        static final int[] NAV_TITLES = {R.string.new_episodes_label, R.string.queue_label, R.string.downloads_label, R.string.playback_history_label, R.string.add_feed_label};


        static final int SUBSCRIPTION_OFFSET = 1 + NAV_TITLES.length;

        private ItemAccess itemAccess;
        private Context context;

        private NavListAdapter(ItemAccess itemAccess, Context context) {
            this.itemAccess = itemAccess;
            this.context = context;
        }

        @Override
        public int getCount() {
            return NAV_TITLES.length + 1 + itemAccess.getCount();
        }

        @Override
        public Object getItem(int position) {
            int viewType = getItemViewType(position);
            if (viewType == VIEW_TYPE_NAV) {
                return context.getString(NAV_TITLES[position]);
            } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
                return context.getString(R.string.podcasts_label);
            } else {
                return itemAccess.getItem(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            if (0 <= position && position < NAV_TITLES.length) {
                return VIEW_TYPE_NAV;
            } else if (position < NAV_TITLES.length + 1) {
                return VIEW_TYPE_SECTION_DIVIDER;
            } else {
                return VIEW_TYPE_SUBSCRIPTION;
            }
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            View v = null;
            if (viewType == VIEW_TYPE_NAV) {
                v = getNavView((String) getItem(position), position, convertView, parent);
            } else if (viewType == VIEW_TYPE_SECTION_DIVIDER) {
                v =  getSectionDividerView((String) getItem(position), position, convertView, parent);
            } else {
                v =  getFeedView(position - SUBSCRIPTION_OFFSET, convertView, parent);
            }
            if (v != null) {
                TextView txtvTitle = (TextView) v.findViewById(R.id.txtvTitle);
                if (position == itemAccess.getSelectedItemIndex()) {
                    txtvTitle.setTypeface(null, Typeface.BOLD);
                } else {
                    txtvTitle.setTypeface(null, Typeface.NORMAL);
                }
            }
            return v;
        }

        private View getNavView(String title, int position, View convertView, ViewGroup parent) {
            NavHolder holder;
            if (convertView == null) {
                holder = new NavHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                convertView = inflater.inflate(R.layout.nav_listitem, null);

                holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
                convertView.setTag(holder);
            } else {
                holder = (NavHolder) convertView.getTag();
            }

            holder.title.setText(title);

            return convertView;
        }

        private View getSectionDividerView(String title, int position, View convertView, ViewGroup parent) {
            SectionHolder holder;
            if (convertView == null) {
                holder = new SectionHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                convertView = inflater.inflate(R.layout.nav_section_item, null);

                holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
                convertView.setTag(holder);
            } else {
                holder = (SectionHolder) convertView.getTag();
            }

            holder.title.setText(title);

            convertView.setEnabled(false);
            convertView.setOnClickListener(null);

            return convertView;
        }

        private View getFeedView(int feedPos, View convertView, ViewGroup parent) {
            FeedHolder holder;
            Feed feed = itemAccess.getItem(feedPos);

            if (convertView == null) {
                holder = new FeedHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                convertView = inflater.inflate(R.layout.nav_feedlistitem, null);

                holder.title = (TextView) convertView.findViewById(R.id.txtvTitle);
                holder.image = (ImageView) convertView.findViewById(R.id.imgvCover);
                convertView.setTag(holder);
            } else {
                holder = (FeedHolder) convertView.getTag();
            }

            holder.title.setText(feed.getTitle());
            ImageLoader.getInstance().loadThumbnailBitmap(feed.getImage(), holder.image, (int) context.getResources().getDimension(R.dimen.thumbnail_length_navlist));

            return convertView;
        }

        static class NavHolder {
            TextView title;
        }

        static class SectionHolder {
            TextView title;
        }

        static class FeedHolder {
            TextView title;
            ImageView image;
        }
    }

    public interface ItemAccess {
        int getCount();

        Feed getItem(int position);

        int getSelectedItemIndex();

    }
}
