package de.danoeh.antennapod.fragment.homesections;

import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.HomeFragment;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import kotlin.Unit;
import slush.Slush;


public class SubsSection extends HomeSection {

    public static final String TAG = "SubsSection";

    public SubsSection(HomeFragment context) {
        super(context);
        sectionTitle = "Rediscover";
        sectionNavigateTitle = context.getString(R.string.subscriptions_label);
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            ((MainActivity) context.requireActivity()).loadFragment(SubscriptionFragment.TAG, null);
        };
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
        new Slush.SingleType<FeedItem>()
                .setItemLayout(R.layout.quick_feed_discovery_item)
                .setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.HORIZONTAL, false))
                .onBind((view, item) -> {
                    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                    int side = (int) displayMetrics.density * 140;
                    view.getLayoutParams().height = side;
                    view.getLayoutParams().width = side;
                    ImageView cover = view.findViewById(R.id.discovery_cover);
                    new CoverLoader((MainActivity) context.requireActivity())
                            .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                            .withFallbackUri(item.getFeed().getImageUrl())
                            .withCoverView(cover)
                            .load();

                    view.setOnLongClickListener(v -> {
                        selectedItem = item;
                        context.setSelectedItem(item);
                        return false;
                    });
                    view.setOnCreateContextMenuListener(SubsSection.this);
                })
                .setItems(loadItems())
                .onItemClickWithItem(this::onItemClick)
                .into(recyclerView);

        super.addSectionTo(parent);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return DBReader.getRecentlyPublishedEpisodes(0, 6, new FeedItemFilter(""), false);
    }
}
