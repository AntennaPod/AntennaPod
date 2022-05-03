package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class InboxFragment extends EpisodesListFragment implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "NewEpisodesFragment";
    private static final String PREF_NAME = "PrefNewEpisodesFragment";
    private static final String KEY_UP_ARROW = "up_arrow";

    private Toolbar toolbar;
    private boolean displayUpArrow;
    private volatile boolean isUpdatingFeeds;

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return item.isNew();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inboxContainer = View.inflate(getContext(), R.layout.inbox_fragment, null);
        View root = super.onCreateView(inflater, container, savedInstanceState);
        ((FrameLayout) inboxContainer.findViewById(R.id.inboxContent)).addView(root);
        emptyView.setTitle(R.string.no_inbox_head_label);
        emptyView.setMessage(R.string.no_inbox_label);

        toolbar = inboxContainer.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.inbox);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        SwipeActions swipeActions = new SwipeActions(this, TAG).attachTo(recyclerView);
        swipeActions.setFilter(new FeedItemFilter(FeedItemFilter.NEW));

        return inboxContainer;
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    private void updateToolbar() {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
            updateToolbar();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        super.onEventMainThread(event);
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            updateToolbar();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getNewItemsList(0, page * EPISODES_PER_PAGE);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return DBReader.getNewItemsList((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE);
    }
}
