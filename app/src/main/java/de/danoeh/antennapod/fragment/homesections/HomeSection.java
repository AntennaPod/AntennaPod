package de.danoeh.antennapod.fragment.homesections;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import kotlin.Unit;
import slush.Slush;
import slush.listeners.OnBindListener;

/**
 * Section on the HomeFragment
 */
public abstract class HomeSection {

    public enum ItemType {
        COVER_SMALL, COVER_LARGE, EPISODE_ITEM;
    }

    Fragment context;

    View section;
    TextView tvTitle;
    TextView tvNavigate;
    RecyclerView recyclerView;

    //must be set descendant
    protected String sectionTitle;
    protected String sectionNavigateTitle;
    protected ItemType itemType;

    public HomeSection(Fragment context) {
        this.context = context;
        section = View.inflate(context.requireActivity(), R.layout.home_section, null);
        tvTitle = section.findViewById(R.id.sectionTitle);
        tvNavigate = section.findViewById(R.id.sectionNavigate);
        recyclerView = section.findViewById(R.id.sectionRecyclerView);
    }

    public void addSectionTo(LinearLayout parent) {
        int itemLayout;
        int orientation = RecyclerView.HORIZONTAL;

        switch (itemType) {
            default:
            case COVER_SMALL:
                itemLayout = R.layout.quick_feed_discovery_item;
                break;
            case COVER_LARGE:
                itemLayout = R.layout.cover_play_title_item;
                break;
        }

        List<FeedItem> items = loadItems();

        if (itemType == ItemType.EPISODE_ITEM) {
            EpisodeItemListAdapter adapter = new EpisodeItemListAdapter((MainActivity) context.requireActivity());
            adapter.updateItems(items);
            recyclerView.setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.VERTICAL, false));
            recyclerView.setRecycledViewPool(((MainActivity) context.requireActivity()).getRecycledViewPool());
            recyclerView.setAdapter(adapter);
        } else {
            new Slush.SingleType<FeedItem>()
                    .setItemLayout(itemLayout)
                    .setLayoutManager(new LinearLayoutManager(context.getContext(), orientation, false))
                    .onBind(bind())
                    .setItems(items)
                    .onItemClickWithItem(this::onItemClick)
                    .into(recyclerView);
        }

        tvTitle.setText(sectionTitle);
        tvNavigate.setText(sectionNavigateTitle.toLowerCase()+" >>");
        tvNavigate.setOnClickListener(navigate());

        if (items.size() > 0) {
            //don't add if empty
            parent.addView(section);
        }
    }

    @NonNull
    protected abstract View.OnClickListener navigate();

    @NonNull
    protected abstract List<FeedItem> loadItems();

    protected abstract Unit onItemClick(View view, FeedItem feedItem);

    private OnBindListener<FeedItem> bind() {
        switch (itemType) {
                default:
                case COVER_SMALL:
                    return (view, feedItem) -> {
                        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                        int side = (int) displayMetrics.density * 120;
                        view.getLayoutParams().height = side;
                        view.getLayoutParams().width = side;
                        ImageView cover = view.findViewById(R.id.discovery_cover);
                        new CoverLoader((MainActivity) context.requireActivity())
                                .withUri(ImageResourceUtils.getEpisodeListImageLocation(feedItem))
                                .withFallbackUri(feedItem.getFeed().getImageUrl())
                                .withCoverView(cover)
                                .load();
                    };
                case COVER_LARGE:
                    return (view, feedItem) -> {
                        ImageView coverPlay = view.findViewById(R.id.cover_play);
                        TextView title = view.findViewById(R.id.playTitle);
                        TextView date = view.findViewById(R.id.playDate);
                        new CoverLoader((MainActivity) context.requireActivity())
                                .withUri(ImageResourceUtils.getEpisodeListImageLocation(feedItem))
                                .withFallbackUri(feedItem.getFeed().getImageUrl())
                                .withCoverView(coverPlay)
                                .load();
                        title.setText(feedItem.getTitle());
                        date.setText(DateUtils.formatAbbrev(context.requireContext(), feedItem.getPubDate()));
                    };
            }
    }

}
