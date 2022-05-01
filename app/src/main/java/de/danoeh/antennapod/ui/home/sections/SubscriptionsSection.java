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
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.home.HomeSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubscriptionsSection extends HomeSection {
    public static final String TAG = "SubscriptionsSection";
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
        ((MainActivity) requireActivity()).loadChildFragment(new SubscriptionFragment());
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.rediscover_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.subscriptions_label);
    }

    private void loadItems() {
        List<NavDrawerData.DrawerItem> items = DBReader.getNavDrawerData().items;
        //Least played on top
        Collections.reverse(items);
        //mix up the first few podcasts
        if (items.size() > 4) {
            List<NavDrawerData.DrawerItem> topItems = items.subList(0, 4);
            items = items.subList(4, items.size());
            Collections.shuffle(topItems);
            topItems.addAll(items);
            items = topItems;
        }
        List<Feed> feeds = new ArrayList<>();
        for (NavDrawerData.DrawerItem item : items) {
            if (item.type == NavDrawerData.DrawerItem.Type.FEED) {
                feeds.add(((NavDrawerData.FeedDrawerItem) item).feed);
            }
        }
        listAdapter.updateData(feeds);
    }
}
