package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.ui.home.HomeSection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class InboxSection extends HomeSection {
    public static final String TAG = "InboxSection";
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
                MenuItemUtils.setOnClickListeners(menu, InboxSection.this::onContextItemSelected);
            }
        };
        adapter.setDummyViews(NUM_EPISODES);
        viewBinding.recyclerView.setAdapter(adapter);

        SwipeActions swipeActions = new SwipeActions(this, InboxFragment.TAG);
        swipeActions.attachTo(viewBinding.recyclerView);
        swipeActions.setFilter(new FeedItemFilter(FeedItemFilter.NEW));

        loadItems();
        return view;
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new InboxFragment());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        loadItems();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with DownloadEvent");
        DownloaderUpdate update = event.update;
        if (adapter != null && update.mediaIds.length > 0) {
            for (long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(items, mediaId);
                if (pos >= 0) {
                    adapter.notifyItemChangedCompat(pos);
                }
            }
        }
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_new_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.inbox_label);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() ->
                        new Pair<>(DBReader.getNewItemsList(0, NUM_EPISODES),
                                PodDBAdapter.getInstance().getNumberOfNewItems()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    items = data.first;
                    adapter.setDummyViews(0);
                    adapter.updateItems(items);
                    viewBinding.numNewItemsLabel.setVisibility(View.VISIBLE);
                    viewBinding.numNewItemsLabel.setText(String.valueOf(data.second));
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
