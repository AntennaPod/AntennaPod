package de.danoeh.antennapod.fragment;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Shows the download log
 */
public class DownloadLogFragment extends ListFragment {

    private static final String TAG = "DownloadLogFragment";

    private List<DownloadStatus> downloadLog;
    private DownloadLogAdapter adapter;

    private boolean viewsCreated = false;
    private boolean itemsLoaded = false;

    private Subscription subscription;

    @Override
    public void onStart() {
        super.onStart();
        setHasOptionsMenu(true);
        EventDistributor.getInstance().register(contentUpdate);
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        viewsCreated = true;
        if (itemsLoaded) {
            onFragmentLoaded();
        }
    }

    private void onFragmentLoaded() {
        if (adapter == null) {
            adapter = new DownloadLogAdapter(getActivity(), itemAccess);
            setListAdapter(adapter);
        }
        setListShown(true);
        adapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
    }

    private final DownloadLogAdapter.ItemAccess itemAccess = new DownloadLogAdapter.ItemAccess() {

        @Override
        public int getCount() {
            return (downloadLog != null) ? downloadLog.size() : 0;
        }

        @Override
        public DownloadStatus getItem(int position) {
            if (downloadLog != null && 0 <= position && position < downloadLog.size()) {
                return downloadLog.get(position);
            } else {
                return null;
            }
        }
    };

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EventDistributor.DOWNLOADLOG_UPDATE) != 0) {
                loadItems();
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        if (itemsLoaded) {
            MenuItem clearHistory = menu.add(Menu.NONE, R.id.clear_history_item, Menu.CATEGORY_CONTAINER, R.string.clear_history_label);
            MenuItemCompat.setShowAsAction(clearHistory, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            TypedArray drawables = getActivity().obtainStyledAttributes(new int[]{R.attr.content_discard});
            clearHistory.setIcon(drawables.getDrawable(0));
            drawables.recycle();
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (itemsLoaded) {
            MenuItem menuItem = menu.findItem(R.id.clear_history_item);
            if(menuItem != null) {
                menuItem.setVisible(downloadLog != null && !downloadLog.isEmpty());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.clear_history_item:
                    DBWriter.clearDownloadLog();
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    private void loadItems() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        subscription = Observable.fromCallable(DBReader::getDownloadLog)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        downloadLog = result;
                        itemsLoaded = true;
                        if (viewsCreated) {
                            onFragmentLoaded();
                        }
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

}
