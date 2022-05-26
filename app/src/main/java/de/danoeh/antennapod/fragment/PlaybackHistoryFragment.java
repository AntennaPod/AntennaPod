package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.event.playback.PlaybackHistoryEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class PlaybackHistoryFragment extends EpisodesListFragment implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "PlaybackHistoryFragment";
    private static final String KEY_UP_ARROW = "up_arrow";

    private Toolbar toolbar;
    private boolean displayUpArrow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View historyContainer = View.inflate(getContext(), R.layout.list_container_fragment, null);
        View root = super.onCreateView(inflater, container, savedInstanceState);

        ((FrameLayout) historyContainer.findViewById(R.id.listContent)).addView(root);

        toolbar = historyContainer.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.playback_history_label);
        toolbar.setOnMenuItemClickListener(this);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);
        toolbar.inflateMenu(R.menu.playback_history);
        refreshToolbarState();

        listAdapter = new PlaybackHistoryListAdapter((MainActivity) getActivity());
        recyclerView.setAdapter(listAdapter);

        emptyView.setIcon(R.drawable.ic_history);
        emptyView.setTitle(R.string.no_history_head_label);
        emptyView.setMessage(R.string.no_history_label);

        return historyContainer;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    public void refreshToolbarState() {
        boolean hasHistory = episodes != null && !episodes.isEmpty();
        toolbar.getMenu().findItem(R.id.clear_history_item).setVisible(hasHistory);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.clear_history_item) {
            DBWriter.clearPlaybackHistory();
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistoryUpdated(PlaybackHistoryEvent event) {
        loadItems();
        refreshToolbarState();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
        refreshToolbarState();
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
        refreshToolbarState();
    }

    @Override
    protected void onFragmentLoaded(List<FeedItem> episodes) {
        super.onFragmentLoaded(episodes);
        listAdapter.notifyDataSetChanged();
        refreshToolbarState();
    }

    private class PlaybackHistoryListAdapter extends EpisodeItemListAdapter {

        public PlaybackHistoryListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            MenuItemUtils.setOnClickListeners(menu, PlaybackHistoryFragment.this::onContextItemSelected);
        }
    }

    @Override
    protected List<FeedItem> loadData() {
        return loadMoreData(0);
    }

    @Override
    protected List<FeedItem> loadMoreData(int page) {
        List<FeedItem> history = DBReader.getPlaybackHistory((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE);

        return history;
    }

    @Override
    protected int loadTotalItemCount() {
        return (int) DBReader.getPlaybackHistoryLength();
    }
}
