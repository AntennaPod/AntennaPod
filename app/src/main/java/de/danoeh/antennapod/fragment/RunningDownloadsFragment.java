package de.danoeh.antennapod.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import de.danoeh.antennapod.activity.MainActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadlistAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.view.EmptyViewHandler;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays all running downloads and provides actions to cancel them
 */
public class RunningDownloadsFragment extends ListFragment {

    private static final String TAG = "RunningDownloadsFrag";

    private DownloadlistAdapter adapter;
    private List<Downloader> downloaderList = new ArrayList<>();
    private boolean hasUnsuccessfulDownloads = DownloadRequester.getInstance().hasUnsuccessfulDownloads();
    private EmptyViewHandler runningDownloadStatusView;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        setHasOptionsMenu(true);
        adapter = new DownloadlistAdapter(getActivity(), itemAccess);
        setListAdapter(adapter);

        runningDownloadStatusView = new EmptyViewHandler(getActivity());
        runningDownloadStatusView.setIcon(R.attr.av_download);
        runningDownloadStatusView.setTitle(R.string.no_run_downloads_head_label);
        runningDownloadStatusView.setMessage(R.string.no_run_downloads_label);
        runningDownloadStatusView.attachToListView(getListView());

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setListAdapter(null);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(DownloadEvent event) {
        Log.d(TAG, "onEvent() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        adapter.notifyDataSetChanged();
        adjustStatusView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.running_downloads, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_item:
                DownloadRequester.getInstance().retryUnsuccessfulDownloads(getActivity(), true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Changes empty view in background of running downloads list according to download failure status.
     */
    private void adjustStatusView() {
        if (hasUnsuccessfulDownloads != DownloadRequester.getInstance().hasUnsuccessfulDownloads()) {
            hasUnsuccessfulDownloads = DownloadRequester.getInstance().hasUnsuccessfulDownloads();
            runningDownloadStatusView.setIcon(hasUnsuccessfulDownloads
                    ? R.attr.navigation_refresh : R.attr.av_download);
            runningDownloadStatusView.setTitle(hasUnsuccessfulDownloads
                    ? R.string.downloads_failed_head_label : R.string.no_run_downloads_head_label);
            runningDownloadStatusView.setMessage(hasUnsuccessfulDownloads
                    ? R.string.downloads_failed_label : R.string.no_run_downloads_label);
        }
    }

    private final DownloadlistAdapter.ItemAccess itemAccess = new DownloadlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return downloaderList.size();
        }

        @Override
        public Downloader getItem(int position) {
            if (0 <= position && position < downloaderList.size()) {
                return downloaderList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public void onSecondaryActionClick(Downloader downloader) {
            DownloadRequest downloadRequest = downloader.getDownloadRequest();
            DownloadRequester.getInstance().cancelDownload(getActivity(), downloadRequest.getSource());

            if (downloadRequest.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                    && UserPreferences.isEnableAutodownload()) {
                FeedMedia media = DBReader.getFeedMedia(downloadRequest.getFeedfileId());
                DBWriter.setFeedItemAutoDownload(media.getItem(), false);

                ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                        R.string.download_canceled_autodownload_enabled_msg, Toast.LENGTH_SHORT);
            }
        }
    };
}
