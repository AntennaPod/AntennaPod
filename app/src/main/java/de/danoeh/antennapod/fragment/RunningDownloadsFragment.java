package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

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
import de.greenrobot.event.EventBus;

/**
 * Displays all running downloads and provides actions to cancel them
 */
public class RunningDownloadsFragment extends ListFragment {

    private static final String TAG = "RunningDownloadsFrag";

    private DownloadlistAdapter adapter;
    private List<Downloader> downloaderList;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        adapter = new DownloadlistAdapter(getActivity(), itemAccess);
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setListAdapter(null);
        adapter = null;
    }

    public void onEvent(DownloadEvent event) {
        Log.d(TAG, "onEvent() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }


    private final DownloadlistAdapter.ItemAccess itemAccess = new DownloadlistAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (downloaderList != null) ? downloaderList.size() : 0;
        }

        @Override
        public Downloader getItem(int position) {
            if (downloaderList != null && 0 <= position && position < downloaderList.size()) {
                return downloaderList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public void onSecondaryActionClick(Downloader downloader) {
            DownloadRequest downloadRequest = downloader.getDownloadRequest();
            DownloadRequester.getInstance().cancelDownload(getActivity(), downloadRequest.getSource());

            if(downloadRequest.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA &&
                    UserPreferences.isEnableAutodownload()) {
                FeedMedia media = DBReader.getFeedMedia(downloadRequest.getFeedfileId());
                DBWriter.setFeedItemAutoDownload(media.getItem(), false);
                Toast.makeText(getActivity(), R.string.download_canceled_autodownload_enabled_msg, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.download_canceled_msg, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
