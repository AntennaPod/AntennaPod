package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.HorizontalFeedListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.home.HomeSection;
import de.danoeh.antennapod.ui.statistics.StatisticsFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatisticsSection extends HomeSection {
    public static final String TAG = "StatisticsSection";
    private HorizontalFeedListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        listAdapter = new HorizontalFeedListAdapter((MainActivity) getActivity());
        viewBinding.recyclerView.setAdapter(listAdapter);
        loadItems();
        return view;
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new StatisticsFragment());
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.classics_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.statistics_label);
    }

    private void loadItems() {
        List<StatisticsItem> statisticsData = DBReader.getStatistics(true, 0, Long.MAX_VALUE).feedTime;
        Collections.reverse(statisticsData);
        List<Feed> feeds = new ArrayList<>();
        for (StatisticsItem item : statisticsData) {
            feeds.add(item.feed);
        }
        listAdapter.updateData(feeds);
    }
}
