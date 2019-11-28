package de.danoeh.antennapod.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import androidx.core.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.core.event.DownloadLogEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Shows the download log
 */
public class DownloadLogFragment extends ListFragment {

    private static final String TAG = "DownloadLogFragment";

    private List<DownloadStatus> downloadLog = new ArrayList<>();
    private DownloadLogAdapter adapter;
    private Disposable disposable;

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        EmptyViewHandler emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.attr.av_download);
        emptyView.setTitle(R.string.no_log_downloads_head_label);
        emptyView.setMessage(R.string.no_log_downloads_label);
        emptyView.attachToListView(getListView());

        adapter = new DownloadLogAdapter(getActivity(), itemAccess);
        setListAdapter(adapter);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    private void onFragmentLoaded() {
        setListShown(true);
        adapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        DownloadStatus status = adapter.getItem(position);
        String url = "unknown";
        String message = getString(R.string.download_successful);
        if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            FeedMedia media = DBReader.getFeedMedia(status.getFeedfileId());
            if (media != null) {
                url = media.getDownload_url();
            }
        } else if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            Feed feed = DBReader.getFeed(status.getFeedfileId());
            if (feed != null) {
                url = feed.getDownload_url();
            }
        }

        if (!status.isSuccessful()) {
            message = status.getReasonDetailed();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.download_error_details);
        builder.setMessage(getString(R.string.download_error_details_message, message, url));
        builder.setPositiveButton(android.R.string.ok, null);
        Dialog dialog = builder.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setTextIsSelectable(true);
    }

    private final DownloadLogAdapter.ItemAccess itemAccess = new DownloadLogAdapter.ItemAccess() {

        @Override
        public int getCount() {
            return downloadLog.size();
        }

        @Override
        public DownloadStatus getItem(int position) {
            if (0 <= position && position < downloadLog.size()) {
                return downloadLog.get(position);
            } else {
                return null;
            }
        }
    };

    @Subscribe
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem clearHistory = menu.add(Menu.NONE, R.id.clear_history_item, Menu.CATEGORY_CONTAINER, R.string.clear_history_label);
        MenuItemCompat.setShowAsAction(clearHistory, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        TypedArray drawables = getActivity().obtainStyledAttributes(new int[]{R.attr.content_discard});
        clearHistory.setIcon(drawables.getDrawable(0));
        drawables.recycle();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.clear_history_item);
        if (menuItem != null) {
            menuItem.setVisible(!downloadLog.isEmpty());
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
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(DBReader::getDownloadLog)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        downloadLog = result;
                        onFragmentLoaded();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
