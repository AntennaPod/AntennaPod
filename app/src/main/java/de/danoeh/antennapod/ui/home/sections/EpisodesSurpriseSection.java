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
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.home.HomeSection;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Random;

public class EpisodesSurpriseSection extends HomeSection {
    public static final String TAG = "EpisodesSurpriseSection";
    private HorizontalItemListAdapter listAdapter;
    private int seed = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.shuffleButton.setVisibility(View.VISIBLE);
        viewBinding.shuffleButton.setOnClickListener(v -> {
            seed = new Random().nextInt();
            loadItems();
        });
        listAdapter = new HorizontalItemListAdapter((MainActivity) getActivity());
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        viewBinding.recyclerView.setAdapter(listAdapter);
        seed = new Random().nextInt();
        loadItems();
        return view;
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new EpisodesFragment());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_surprise_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.episodes_label);
    }

    private void loadItems() {
        List<FeedItem> recentItems = DBReader.getRandomEpisodes(8, seed);
        listAdapter.updateData(recentItems);
    }
}