package de.danoeh.antennapod.fragment;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.event.PlaybackHistoryEvent;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.event.PlayerStatusEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.view.EmptyViewHandler;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class PlaybackHistoryFragment extends Fragment {
    public static final String TAG = "PlaybackHistoryFragment";

    private List<FeedItem> playbackHistory;
    private PlaybackHistoryListAdapter adapter;
    private Disposable disposable;
    private RecyclerView recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.playback_history_label);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        recyclerView = root.findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).build());
        recyclerView.setVisibility(View.GONE);
        adapter = new PlaybackHistoryListAdapter((MainActivity) getActivity());
        recyclerView.setAdapter(adapter);

        EmptyViewHandler emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.attr.ic_history);
        emptyView.setTitle(R.string.no_history_head_label);
        emptyView.setMessage(R.string.no_history_label);
        emptyView.attachToRecyclerView(recyclerView);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (playbackHistory == null) {
            return;
        } else if (adapter == null) {
            loadItems();
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(playbackHistory, item.getId());
            if (pos >= 0) {
                playbackHistory.remove(pos);
                playbackHistory.add(pos, item);
                adapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        if (adapter != null && update.mediaIds.length > 0) {
            for (long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(playbackHistory, mediaId);
                if (pos >= 0) {
                    adapter.notifyItemChangedCompat(pos);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem clearHistory = menu.add(Menu.NONE, R.id.clear_history_item, Menu.CATEGORY_CONTAINER, R.string.clear_history_label);
        MenuItemCompat.setShowAsAction(clearHistory, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        TypedArray drawables = getActivity().obtainStyledAttributes(new int[]{R.attr.ic_delete});
        clearHistory.setIcon(drawables.getDrawable(0));
        drawables.recycle();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.clear_history_item);
        if (menuItem != null) {
            menuItem.setVisible(playbackHistory != null && !playbackHistory.isEmpty());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.clear_history_item:
                    DBWriter.clearPlaybackHistory();
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        FeedItem selectedItem = adapter.getSelectedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistoryUpdated(PlaybackHistoryEvent event) {
        loadItems();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
        getActivity().supportInvalidateOptionsMenu();
    }

    private void onFragmentLoaded() {
        adapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        playbackHistory = result;
                        adapter.updateItems(playbackHistory);
                        onFragmentLoaded();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private List<FeedItem> loadData() {
        List<FeedItem> history = DBReader.getPlaybackHistory();
        DBReader.loadAdditionalFeedItemListData(history);
        return history;
    }

    private static class PlaybackHistoryListAdapter extends EpisodeItemListAdapter {

        public PlaybackHistoryListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void onBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            super.onBindViewHolder(holder, pos);
            // played items shouldn't be transparent for this fragment since, *all* items
            // in this fragment will, by definition, be played. So it serves no purpose and can make
            // it harder to read.
            holder.itemView.setAlpha(1.0f);
        }
    }
}
