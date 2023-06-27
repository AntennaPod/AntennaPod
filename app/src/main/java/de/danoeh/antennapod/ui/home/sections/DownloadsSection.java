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
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.event.DownloadLogEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.fragment.CompletedDownloadsFragment;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.home.HomeSection;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class DownloadsSection extends HomeSection {
    public static final String TAG = "DownloadsSection";
    private static final int NUM_EPISODES = 2;
    private EpisodeItemListAdapter adapter;
    private List<FeedItem> items;
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.recyclerView.setPadding(0, 0, 0, 0);
        viewBinding.recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        viewBinding.recyclerView.setRecycledViewPool(((MainActivity) requireActivity()).getRecycledViewPool());
        adapter = new EpisodeItemListAdapter((MainActivity) requireActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                MenuItemUtils.setOnClickListeners(menu, DownloadsSection.this::onContextItemSelected);
            }
        };
        adapter.setDummyViews(NUM_EPISODES);
        viewBinding.recyclerView.setAdapter(adapter);

        SwipeActions swipeActions = new SwipeActions(this, CompletedDownloadsFragment.TAG);
        swipeActions.attachTo(viewBinding.recyclerView);
        swipeActions.setFilter(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new CompletedDownloadsFragment());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter == null) {
            return;
        }
        for (int i = 0; i < adapter.getItemCount(); i++) {
            EpisodeItemViewHolder holder = (EpisodeItemViewHolder)
                    viewBinding.recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null && holder.isCurrentlyPlayingItem()) {
                holder.notifyPlaybackPositionUpdated(event);
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_downloads_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.downloads_label);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        SortOrder sortOrder = UserPreferences.getDownloadsSortedOrder();
        disposable = Observable.fromCallable(() -> DBReader.getEpisodes(0, Integer.MAX_VALUE,
                        new FeedItemFilter(FeedItemFilter.DOWNLOADED), sortOrder))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(downloads -> {
                    if (downloads.size() > NUM_EPISODES) {
                        downloads = downloads.subList(0, NUM_EPISODES);
                    }
                    items = downloads;
                    adapter.setDummyViews(0);
                    adapter.updateItems(items);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
