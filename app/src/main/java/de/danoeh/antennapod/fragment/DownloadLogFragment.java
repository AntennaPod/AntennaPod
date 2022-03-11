package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloadLogEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.dialog.DownloadLogDetailsDialog;
import de.danoeh.antennapod.ui.common.PagedToolbarFragment;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the download log
 */
public class DownloadLogFragment extends ListFragment {

    private static final String TAG = "DownloadLogFragment";

    private List<DownloadStatus> downloadLog = new ArrayList<>();
    private List<Downloader> runningDownloads = new ArrayList<>();
    private DownloadLogAdapter adapter;
    private Disposable disposable;

    private boolean isUpdatingFeeds = false;

    @Override
    public void onStart() {
        super.onStart();
        loadDownloadLog();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);
        setListShown(true);

        EmptyViewHandler emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.drawable.ic_download);
        emptyView.setTitle(R.string.no_log_downloads_head_label);
        emptyView.setMessage(R.string.no_log_downloads_label);
        emptyView.attachToListView(getListView());

        adapter = new DownloadLogAdapter(getActivity(), this);
        setListAdapter(adapter);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Object item = adapter.getItem(position);
        if (item instanceof DownloadStatus) {
            new DownloadLogDetailsDialog(getContext(), (DownloadStatus) item).show();
        }
    }

    @Subscribe
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadDownloadLog();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.clear_logs_item).setVisible(!downloadLog.isEmpty());
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.clear_logs_item) {
            DBWriter.clearDownloadLog();
            return true;
        } else if (item.getItemId() == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
            return true;
        }
        return false;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(DownloadEvent event) {
        Log.d(TAG, "onEvent() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        runningDownloads = update.downloaders;
        adapter.setRunningDownloads(runningDownloads);
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    private void loadDownloadLog() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(DBReader::getDownloadLog)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        downloadLog = result;
                        adapter.setDownloadLog(downloadLog);
                        ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
