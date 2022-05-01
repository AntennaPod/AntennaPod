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
import de.danoeh.antennapod.adapter.HorizontalItemListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.home.HomeSection;

import java.util.Collections;
import java.util.List;

public class SurpriseSection extends HomeSection {
    public static final String TAG = "SurpriseSection";
    private HorizontalItemListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.shuffleButton.setVisibility(View.VISIBLE);
        viewBinding.shuffleButton.setOnClickListener(view -> loadItems());
        listAdapter = new HorizontalItemListAdapter((MainActivity) getActivity());
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        viewBinding.recyclerView.setAdapter(listAdapter);
        loadItems();
        return v;
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new EpisodesFragment());
    }

    /*@Override
    protected Unit onItemClick(View view, FeedItem feedItem) {
        new PlaybackServiceStarter(context.requireContext(), feedItem.getMedia())
                .callEvenIfRunning(true)
                .start();
        slush.getItemListEditor().removeItem(feedItem);
        slush.getItemListEditor().addItem(loadItems().get(0));
        return null;
    }*/

    @Override
    protected String getSectionTitle() {
        return getString(R.string.surprise_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.episodes_label);
    }

    private void loadItems() {
        List<FeedItem> recentItems = DBReader.getRecentlyPublishedEpisodes(0, 50,
                new FeedItemFilter(FeedItemFilter.NOT_QUEUED, FeedItemFilter.UNPLAYED));
        Collections.shuffle(recentItems);
        if (recentItems.size() > 8) {
            recentItems = recentItems.subList(0, 8);
        }
        listAdapter.updateData(recentItems);
    }
}