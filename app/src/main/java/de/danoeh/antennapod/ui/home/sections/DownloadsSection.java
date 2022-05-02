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
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.home.HomeSection;

import java.util.List;

public class DownloadsSection extends HomeSection {
    public static final String TAG = "DownloadsSection";
    private EpisodeItemListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.recyclerView.setPadding(0, 0, 0, 0);
        viewBinding.recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        viewBinding.recyclerView.setRecycledViewPool(((MainActivity) requireActivity()).getRecycledViewPool());
        adapter = new EpisodeItemListAdapter((MainActivity) requireActivity());
        viewBinding.recyclerView.setAdapter(adapter);
        loadItems();
        return view;
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new DownloadsFragment());
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_downloads_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.downloads_label);
    }

    private void loadItems() {
        List<FeedItem> downloads = DBReader.getDownloadedItems();
        if (downloads.size() > 2) {
            downloads = downloads.subList(0, 2);
        }
        adapter.updateItems(downloads);
    }
}
