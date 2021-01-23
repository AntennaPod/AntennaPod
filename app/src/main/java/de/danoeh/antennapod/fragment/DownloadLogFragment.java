package de.danoeh.antennapod.fragment;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloadLogEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Shows the download log
 */
public class DownloadLogFragment extends ListFragment {

    private static final String TAG = "DownloadLogFragment";

    private List<DownloadStatus> downloadLog = new ArrayList<>();
    private DownloadLogAdapter adapter;
    private Disposable disposable;

    private boolean isUpdatingFeeds = false;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
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

        String messageFull = getString(R.string.download_error_details_message, message, url);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.download_error_details);
        builder.setMessage(messageFull);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.copy_to_clipboard, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.download_error_details), messageFull);
            clipboard.setPrimaryClip(clip);
            ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT);
        });
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
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.episode_actions).setVisible(false);
        menu.findItem(R.id.clear_logs_item).setVisible(!downloadLog.isEmpty());
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.clear_logs_item:
                    DBWriter.clearDownloadLog();
                    return true;
                case R.id.refresh_item:
                    AutoUpdateManager.runImmediate(requireContext());
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();

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
