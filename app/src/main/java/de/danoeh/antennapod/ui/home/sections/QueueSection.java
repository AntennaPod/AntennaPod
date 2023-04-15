package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.HorizontalItemListAdapter;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.QueueEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.home.HomeSection;
import de.danoeh.antennapod.view.viewholder.HorizontalItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class QueueSection extends HomeSection {
    public static final String TAG = "QueueSection";
    private static final int NUM_EPISODES = 8;
    private HorizontalItemListAdapter listAdapter;
    private Disposable disposable;
    private List<FeedItem> queue = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        listAdapter = new HorizontalItemListAdapter((MainActivity) getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                MenuItemUtils.setOnClickListeners(menu, QueueSection.this::onContextItemSelected);
            }
        };
        listAdapter.setDummyViews(NUM_EPISODES);
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        viewBinding.recyclerView.setAdapter(listAdapter);
        int paddingHorizontal = (int) (12 * getResources().getDisplayMetrics().density);
        viewBinding.recyclerView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new QueueFragment());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQueueChanged(QueueEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (queue == null) {
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(queue, item.getId());
            if (pos >= 0) {
                queue.remove(pos);
                queue.add(pos, item);
                listAdapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EpisodeDownloadEvent event) {
        for (String downloadUrl : event.getUrls()) {
            int pos = FeedItemUtil.indexOfItemWithDownloadUrl(queue, downloadUrl);
            if (pos >= 0) {
                listAdapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (listAdapter == null) {
            return;
        }
        boolean foundCurrentlyPlayingItem = false;
        boolean currentlyPlayingItemIsFirst = true;
        for (int i = 0; i < listAdapter.getItemCount(); i++) {
            HorizontalItemViewHolder holder = (HorizontalItemViewHolder)
                    viewBinding.recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                continue;
            }
            if (holder.isCurrentlyPlayingItem()) {
                holder.notifyPlaybackPositionUpdated(event);
                foundCurrentlyPlayingItem = true;
                currentlyPlayingItemIsFirst = (i == 0);
                break;
            }
        }
        if (!foundCurrentlyPlayingItem || !currentlyPlayingItemIsFirst) {
            loadItems();
        }
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_continue_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.queue_label);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getPausedQueue(NUM_EPISODES))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(queue -> {
                    this.queue = queue;
                    listAdapter.setDummyViews(0);
                    listAdapter.updateData(queue);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));

    }
}
