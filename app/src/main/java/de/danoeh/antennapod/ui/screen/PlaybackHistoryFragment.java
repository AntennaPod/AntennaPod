package de.danoeh.antennapod.ui.screen;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.event.playback.PlaybackHistoryEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.ui.common.DateFormatter;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListAdapter;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;
import de.danoeh.antennapod.ui.episodeslist.EpisodesListFragment;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class PlaybackHistoryFragment extends EpisodesListFragment {
    public static final String TAG = "PlaybackHistoryFragment";
    private static final FeedItemFilter FILTER_HISTORY = new FeedItemFilter(
            FeedItemFilter.IS_IN_HISTORY, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED);

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.inflateMenu(R.menu.playback_history);
        toolbar.setTitle(R.string.playback_history_label);
        updateToolbar();
        emptyView.setIcon(R.drawable.ic_history);
        emptyView.setTitle(R.string.no_history_head_label);
        emptyView.setMessage(R.string.no_history_label);
        return root;
    }

    protected EpisodeItemListAdapter createAdapter() {
        return new EpisodeItemListAdapter(getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                if (!inActionMode()) {
                    menu.findItem(R.id.multi_select).setVisible(true);
                }
                MenuItemUtils.setOnClickListeners(menu, PlaybackHistoryFragment.this::onContextItemSelected);
            }

            @Override
            protected void onSelectedItemsUpdated() {
                super.onSelectedItemsUpdated();
                FeedItemMenuHandler.onPrepareMenu(floatingSelectMenu.getMenu(), getSelectedItems());
                floatingSelectMenu.updateItemVisibility();
            }

            @Override
            protected void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
                String header = null;
                if (episodes.get(pos).getMedia() != null) {
                    String dateCurrent = DateFormatter.formatAbbrev(getContext(),
                            episodes.get(pos).getMedia().getPlaybackCompletionDate());
                    if (pos == 0) {
                        header = dateCurrent;
                    } else if (episodes.get(pos - 1).getMedia() != null) {
                        String datePrevious = DateFormatter.formatAbbrev(getContext(),
                                episodes.get(pos - 1).getMedia().getPlaybackCompletionDate());
                        header = dateCurrent.equals(datePrevious) ? null : dateCurrent;
                    }
                }
                if (header != null) {
                    holder.separatorLabel.setVisibility(View.VISIBLE);
                    holder.separatorLabel.setText(header);
                }
            }
        };
    }

    @Override
    protected FeedItemFilter getFilter() {
        return FeedItemFilter.unfiltered();
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    protected String getPrefName() {
        return TAG;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (super.onMenuItemClick(item)) {
            return true;
        }
        if (item.getItemId() == R.id.clear_history_item) {

            ConfirmationDialog conDialog = new ConfirmationDialog(
                    getActivity(),
                    R.string.clear_history_label,
                    R.string.clear_playback_history_msg) {

                @Override
                public void onConfirmButtonPressed(DialogInterface dialog) {
                    dialog.dismiss();
                    DBWriter.clearPlaybackHistory();
                }
            };
            conDialog.createNewDialog().show();

            return true;
        }
        return false;
    }

    @Override
    protected void updateToolbar() {
        // Not calling super, as we do not have a refresh button that could be updated
        toolbar.getMenu().findItem(R.id.clear_history_item).setVisible(!episodes.isEmpty());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistoryUpdated(PlaybackHistoryEvent event) {
        loadItems();
        updateToolbar();
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getEpisodes(0, page * EPISODES_PER_PAGE, FILTER_HISTORY, SortOrder.COMPLETION_DATE_NEW_OLD);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, FILTER_HISTORY,
                SortOrder.COMPLETION_DATE_NEW_OLD);
    }

    @Override
    protected int loadTotalItemCount() {
        return DBReader.getTotalEpisodeCount(FILTER_HISTORY);
    }
}
