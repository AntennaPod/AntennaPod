package de.danoeh.antennapod.fragment.homesections;

import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.StatisticsItem;
import de.danoeh.antennapod.core.util.comparator.CompareCompat;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.HomeFragment;
import de.danoeh.antennapod.fragment.preferences.StatisticsFragment;
import kotlin.Unit;


public class StatisticsSection extends HomeSection<StatisticsItem> {

    public static final String TAG = "StatisticsSection";

    public StatisticsSection(HomeFragment context) {
        super(context);
        sectionTitle = context.getString(R.string.classics_title);
        sectionNavigateTitle = context.getString(R.string.statistics_label);
    }

    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            context.getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_view, new StatisticsFragment())
                    .addToBackStack(context.getString(R.string.statistics_label)).commit();
        };
    }

    @Override
    protected Unit onItemClick(View view, StatisticsItem item) {
        Fragment fragment = FeedItemlistFragment.newInstance(item.feed.getId());
        ((MainActivity) context.requireActivity()).loadChildFragment(fragment);
        return null;
    }

    @Override
    public void addSectionTo(LinearLayout parent) {
        easySlush(R.layout.quick_feed_discovery_item, (view, item) -> {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int side = (int) displayMetrics.density * 140;
            view.getLayoutParams().height = side;
            view.getLayoutParams().width = side;
            ImageView cover = view.findViewById(R.id.discovery_cover);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cover.setElevation(2 * displayMetrics.density);
            }
            new CoverLoader((MainActivity) context.requireActivity())
                                .withUri(item.feed.getImageUrl())
                                .withFallbackUri(item.feed.getImageUrl())
                                .withCoverView(cover)
                                .load();
            });

        super.addSectionTo(parent);
    }

    @NonNull
    @Override
    protected List<StatisticsItem> loadItems() {
        List<StatisticsItem> statisticsData = DBReader.getStatistics();
        Collections.sort(statisticsData, (item1, item2) ->
                CompareCompat.compareLong(item1.timePlayedCountAll, item2.timePlayedCountAll));
        return statisticsData;
    }
}
