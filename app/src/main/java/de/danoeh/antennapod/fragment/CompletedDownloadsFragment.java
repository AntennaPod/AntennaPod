package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DownloadedEpisodesListAdapter;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.dialog.EpisodesApplyActionFragment;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static de.danoeh.antennapod.dialog.EpisodesApplyActionFragment.ACTION_ADD_TO_QUEUE;
import static de.danoeh.antennapod.dialog.EpisodesApplyActionFragment.ACTION_DELETE;

/**
 * Displays all running downloads and provides a button to delete them
 */
public class CompletedDownloadsFragment extends ListFragment {

    private static final String TAG = CompletedDownloadsFragment.class.getSimpleName();

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOADLOG_UPDATE |
            EventDistributor.UNREAD_ITEMS_UPDATE;

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
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        position -= l.getHeaderViewsCount();
        long[] ids = FeedItemUtil.getIds(items);
        ((MainActivity) requireActivity()).loadChildFragment(ItemFragment.newInstance(ids, position));
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

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                loadItems();
            }
        }
    };

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
