package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadlistAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.download.DownloadRequest;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DownloadRequester;

import java.util.List;

/**
 * Shows all running downloads in a list. The list objects are DownloadStatus
 * objects created by a DownloadObserver.
 */
public class DownloadActivity extends ActionBarActivity implements
        ActionMode.Callback {

    private static final String TAG = "DownloadActivity";
    private static final int MENU_SHOW_LOG = 0;
    private static final int MENU_CANCEL_ALL_DOWNLOADS = 1;
    private DownloadlistAdapter dla;
    private DownloadRequester requester;

    private ActionMode mActionMode;
    private DownloadRequest selectedDownload;

    private ListView listview;

    private DownloadObserver downloadObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_activity);

        listview = (ListView) findViewById(R.id.listview);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Creating Activity");
        requester = DownloadRequester.getInstance();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        downloadObserver = new DownloadObserver(this, new Handler(), observerCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        downloadObserver.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        downloadObserver.onResume();
        if (dla != null) {
            dla.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Stopping Activity");
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        listview.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view,
                                           int position, long id) {
                DownloadRequest selection = dla.getItem(position)
                        .getDownloadRequest();
                if (selection != null && mActionMode != null) {
                    mActionMode.finish();
                }
                dla.setSelectedItemIndex(position);
                selectedDownload = selection;
                mActionMode = startSupportActionMode(DownloadActivity.this);
                return true;
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, MENU_SHOW_LOG, Menu.NONE,
                R.string.show_download_log),
                MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, MENU_CANCEL_ALL_DOWNLOADS, Menu.NONE,
                R.string.cancel_all_downloads_label),
                MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case MENU_SHOW_LOG:
                startActivity(new Intent(this, DownloadLogActivity.class));
                break;
            case MENU_CANCEL_ALL_DOWNLOADS:
                requester.cancelAllDownloads(this);
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (selectedDownload != null) {
            TypedArray drawables = obtainStyledAttributes(new int[]{R.attr.navigation_cancel});
            menu.add(Menu.NONE, R.id.cancel_download_item, Menu.NONE,
                    R.string.cancel_download_label).setIcon(
                    drawables.getDrawable(0));
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean handled = false;
        switch (item.getItemId()) {
            case R.id.cancel_download_item:
                requester.cancelDownload(this, selectedDownload.getSource());
                handled = true;
                break;
        }
        mActionMode.finish();
        return handled;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        selectedDownload = null;
        dla.setSelectedItemIndex(DownloadlistAdapter.SELECTION_NONE);
    }


    private DownloadObserver.Callback observerCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged() {
            if (dla != null) {
                dla.notifyDataSetChanged();
            }
        }

        @Override
        public void onDownloadDataAvailable(List<Downloader> downloaderList) {
            //dla = new DownloadlistAdapter(DownloadActivity.this, 0,
             //       downloaderList);
            listview.setAdapter(dla);
            dla.notifyDataSetChanged();
        }
    };

}
