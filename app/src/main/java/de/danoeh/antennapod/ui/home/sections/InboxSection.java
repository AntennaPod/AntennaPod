package de.danoeh.antennapod.ui.home.sections;

import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.fragment.NewEpisodesFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.ui.home.HomeFragment;
import de.danoeh.antennapod.ui.home.HomeSection;
import kotlin.Unit;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class InboxSection extends HomeSection<FeedItem> {
    public static final String TAG = "InboxSection";

    public InboxSection(HomeFragment context) {
        super(context);
        viewBinding.recyclerView.setPadding(0, 0, 0, 0);
        viewBinding.recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view ->
                ((MainActivity) context.requireActivity()).loadFragment(NewEpisodesFragment.TAG, null);
    }

    @Override
    protected Unit onItemClick(View view, FeedItem feedItem) {
        //TODO PLAY
        long[] ids = FeedItemUtil.getIds(loadItems());
        int position = ArrayUtils.indexOf(ids, feedItem.getId());
        ((MainActivity) context.requireActivity()).loadChildFragment(ItemPagerFragment.newInstance(ids, position));
        return null;
    }

    @Override
    public void addSectionTo(LinearLayout parent) {
        EpisodeItemListAdapter adapter = new EpisodeItemListAdapter((MainActivity) context.requireActivity());
        adapter.updateItems(loadItems());
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.VERTICAL, false));
        viewBinding.recyclerView.setRecycledViewPool(((MainActivity) context.requireActivity()).getRecycledViewPool());
        viewBinding.recyclerView.setAdapter(adapter);

        super.addSectionTo(parent);
    }

    @Override
    protected String getSectionTitle() {
        return context.getString(R.string.new_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return context.getString(R.string.new_label);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        viewBinding.numNewItemsLabel.setVisibility(View.VISIBLE);
        viewBinding.numNewItemsLabel.setText(String.valueOf(PodDBAdapter.getInstance().getNumberOfNewItems()));
        return DBReader.getNewItemsList(0, 2);
    }
}