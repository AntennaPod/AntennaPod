package de.danoeh.antennapod.ui.home.sections;

import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.annimon.stream.Stream;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.ui.home.HomeFragment;
import de.danoeh.antennapod.ui.home.HomeSection;
import de.danoeh.antennapod.ui.statistics.StatisticsFragment;
import kotlin.Unit;

import java.util.Collections;
import java.util.List;


public class StatisticsSection extends HomeSection<DBReader.StatisticsResult> {

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
    protected Unit onItemClick(View view, DBReader.StatisticsResult item) {
        //Fragment fragment = FeedItemlistFragment.newInstance(item.feed.getId());
        //((MainActivity) context.requireActivity()).loadChildFragment(fragment);
        return null;
    }

    @Override
    public void addSectionTo(LinearLayout parent) {
        easySlush(R.layout.quick_feed_discovery_item, (view, item) -> {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int side = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, displayMetrics);
            view.getLayoutParams().height = side;
            view.getLayoutParams().width = side;
            ImageView cover = view.findViewById(R.id.discovery_cover);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cover.setElevation(2 * displayMetrics.density);
            }
            /*new CoverLoader((MainActivity) context.requireActivity())
                                .withUri(item.feed.getImageUrl())
                                .withFallbackUri(item.feed.getImageUrl())
                                .withCoverView(cover)
                                .load();*/
        });

        super.addSectionTo(parent);
    }

    @NonNull
    @Override
    protected List<DBReader.StatisticsResult> loadItems() {
        List<DBReader.StatisticsResult> statisticsData = Stream.of(DBReader.getStatistics(true, 0, Long.MAX_VALUE))
                .toList();
        Collections.reverse(statisticsData);
        return statisticsData;
    }
}