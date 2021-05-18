package de.danoeh.antennapod.fragment.homesections;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import slush.Slush;


public class QueueSection extends HomeSection {

    public QueueSection(Fragment context) {
        super(context);
        sectionTitle = context.getString(R.string.queue_label);
        sectionNavigateTitle = context.getString(R.string.inbox_label);
        //sectionFragment = new PowerEpisodesFragment();
        //((PowerEpisodesFragment) sectionFragment).hideToolbar = true;
        //expandsToFillHeight = true;

        new Slush.SingleType<FeedItem>()
                .setItemLayout(R.layout.quick_feed_discovery_item)
                .setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.HORIZONTAL, false))
                .onBind((view, feedItem) -> {
                    view.getLayoutParams().height = getItemSize(false);
                    view.getLayoutParams().width = getItemSize(false);
                    ImageView cover = view.findViewById(R.id.discovery_cover);
                    new CoverLoader((MainActivity) context.requireActivity())
                            .withUri(ImageResourceUtils.getEpisodeListImageLocation(feedItem))
                            .withFallbackUri(feedItem.getFeed().getImageUrl())
                            .withCoverView(cover)
                            .load();
                })
                .setItems(loadItems())
                .onItemClickWithItem((view, feedItem) -> {
                    long[] ids = FeedItemUtil.getIds(loadItems());
                    int position = ArrayUtils.indexOf(ids, feedItem.getId());
                    ((MainActivity) context.requireActivity()).loadChildFragment(ItemPagerFragment.newInstance(ids, position));
                    return null;
                })
                .into(recyclerView);
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            ((MainActivity) context.requireActivity()).loadFragment(InboxFragment.TAG, null);
        };
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return DBReader.getRecentlyPublishedEpisodes(0, 10, new FeedItemFilter(""), false);
    }

}
