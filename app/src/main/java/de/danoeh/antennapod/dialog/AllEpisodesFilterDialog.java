package de.danoeh.antennapod.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButtonToggleGroup;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = super.onCreateView(getLayoutInflater(), container, savedInstanceState);
        setResetButtonVisibility(View.VISIBLE);
        return layout;
    }

    @Override
    void onFilterChanged(Set<String> newFilterValues) {
        EventBus.getDefault().post(new AllEpisodesFilterChangedEvent(newFilterValues));
    }

    public static class AllEpisodesFilterChangedEvent {
        public final Set<String> filterValues;

        public AllEpisodesFilterChangedEvent(Set<String> filterValues) {
            this.filterValues = filterValues;
        }
    }
}
