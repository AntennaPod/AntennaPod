package de.danoeh.antennapod.fragment.homesections;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import kotlin.Unit;


public class SurpriseSection extends HomeSection {

    public static final String TAG = "SurpriseSection";

    public SurpriseSection(Fragment context) {
        super(context);
        sectionTitle = "Surprise";
        //sectionNavigateTitle = context.getString(R.string.subscriptions_label);
        itemType = ItemType.COVER_LARGE;
    }

    @Override
    protected View.OnClickListener navigate() {
        return null;
    }

    @Override
    protected Unit onItemClick(View view, FeedItem feedItem) {
        //TODO PLAY
        long[] ids = FeedItemUtil.getIds(loadItems());
        int position = ArrayUtils.indexOf(ids, feedItem.getId());
        ((MainActivity) context.requireActivity()).loadChildFragment(ItemPagerFragment.newInstance(ids, position));
        return null;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return DBReader.getRecentlyPublishedEpisodes(6, 6, new FeedItemFilter(""), false);
    }
}
