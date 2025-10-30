package de.danoeh.antennapod.ui;

import android.os.Bundle;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.screen.feed.ItemFilterDialog;
import org.greenrobot.eventbus.EventBus;

import java.util.Set;

public class AllEpisodesFilterDialog extends ItemFilterDialog {

    public static AllEpisodesFilterDialog newInstance(FeedItemFilter filter) {
        AllEpisodesFilterDialog dialog = new AllEpisodesFilterDialog();
        Bundle arguments = new Bundle();
        arguments.putSerializable(ARGUMENT_FILTER, filter);
        dialog.setArguments(arguments);
        return dialog;
    }

    @Override
    public void onFilterChanged(Set<String> newFilterValues) {
        EventBus.getDefault().post(new AllEpisodesFilterChangedEvent(newFilterValues));
    }

    public static class AllEpisodesFilterChangedEvent {
        public final Set<String> filterValues;

        public AllEpisodesFilterChangedEvent(Set<String> filterValues) {
            this.filterValues = filterValues;
        }
    }
}
