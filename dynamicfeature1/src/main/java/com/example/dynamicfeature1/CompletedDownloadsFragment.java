package com.example.dynamicfeature1;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.activity.MainActivity;
import de.danoeh.antennapodSA.adapter.DownloadedEpisodesListAdapter;
import de.danoeh.antennapodSA.core.event.DownloadLogEvent;
import de.danoeh.antennapodSA.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.storage.DBReader;
import de.danoeh.antennapodSA.core.storage.DBWriter;
import de.danoeh.antennapodSA.core.util.FeedItemUtil;
import de.danoeh.antennapodSA.dialog.EpisodesApplyActionFragment;
import de.danoeh.antennapodSA.fragment.ItemPagerFragment;
import de.danoeh.antennapodSA.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static de.danoeh.antennapodSA.dialog.EpisodesApplyActionFragment.ACTION_ADD_TO_QUEUE;
import static de.danoeh.antennapodSA.dialog.EpisodesApplyActionFragment.ACTION_DELETE;

/**
 * Displays all running downloads and provides a button to delete them
 */
public class CompletedDownloadsFragment extends ListFragment {

    private static final String TAG = CompletedDownloadsFragment.class.getSimpleName();

    private List<FeedItem> items = new ArrayList<>();
    private DownloadedEpisodesListAdapter listAdapter;
    private Disposable disposable;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        addVerticalPadding();
        addEmptyView();

        listAdapter = new DownloadedEpisodesListAdapter(getActivity(), itemAccess);
        setListAdapter(listAdapter);
        setListShown(false);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

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
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        position -= l.getHeaderViewsCount();
        long[] ids = FeedItemUtil.getIds(items);
        ((MainActivity) requireActivity()).loadChildFragment(ItemPagerFragment.newInstance(ids, position));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.downloads_completed, menu);
        menu.findItem(R.id.episode_actions).setVisible(items.size() > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.episode_actions) {
            ((MainActivity) requireActivity())
                    .loadChildFragment(EpisodesApplyActionFragment.newInstance(items, ACTION_DELETE | ACTION_ADD_TO_QUEUE));
            return true;
        }
        return false;
    }

    private void addEmptyView() {
        EmptyViewHandler emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.attr.av_download);
        emptyView.setTitle(R.string.no_comp_downloads_head_label);
        emptyView.setMessage(R.string.no_comp_downloads_label);
        emptyView.attachToListView(getListView());
    }

    private void addVerticalPadding() {
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);
    }

    private final DownloadedEpisodesListAdapter.ItemAccess itemAccess = new DownloadedEpisodesListAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public FeedItem getItem(int position) {
            if (0 <= position && position < items.size()) {
                return items.get(position);
            } else {
                return null;
            }
        }

        @Override
        public void onFeedItemSecondaryAction(FeedItem item) {
            DBWriter.deleteFeedMediaOfItem(requireActivity(), item.getMedia().getId());
        }
    };

    @Subscribe
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadItems();
    }

    @Subscribe
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(DBReader::getDownloadedItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    items = result;
                    onItemsLoaded();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void onItemsLoaded() {
        setListShown(true);
        listAdapter.notifyDataSetChanged();
        requireActivity().invalidateOptionsMenu();
    }
}
