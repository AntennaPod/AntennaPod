package de.danoeh.antennapod.fragment.homesections;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.DateUtils;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.playback.PlaybackServiceStarter;
import de.danoeh.antennapod.fragment.HomeFragment;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import kotlin.Unit;
import slush.AdapterAppliedResult;
import slush.Slush;
import slush.utils.BasicDiffCallback;


public class QueueSection extends HomeSection {

    public static final String TAG = "QueueSection";

    private AdapterAppliedResult<FeedItem> slush;

    public QueueSection(HomeFragment context) {
        super(context);
        sectionTitle = "Continue";
        sectionNavigateTitle = context.getString(R.string.queue_label);
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            ((MainActivity) context.requireActivity()).loadFragment(QueueFragment.TAG, null);
        };
    }

    @Override
    protected Unit onItemClick(View view, FeedItem feedItem) {
        new PlaybackServiceStarter(context.requireContext(), feedItem.getMedia())
                .callEvenIfRunning(true)
                .startWhenPrepared(true)
                .shouldStream(!feedItem.isDownloaded())
                .start();
        return null;
    }

    @Override
    public void addSectionTo(LinearLayout parent) {
        slush = easySlush(R.layout.cover_play_title_item, (view, item) -> {
                    ImageView coverPlay = view.findViewById(R.id.cover_play);
                    TextView title = view.findViewById(R.id.playTitle);
                    TextView date = view.findViewById(R.id.playDate);
                    new CoverLoader((MainActivity) context.requireActivity())
                            .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                            .withFallbackUri(item.getFeed().getImageUrl())
                            .withCoverView(coverPlay)
                            .load();
                    title.setText(item.getTitle());
                    date.setText(DateUtils.formatAbbrev(context.requireContext(), item.getPubDate()));

                    view.setOnLongClickListener(v -> {
                        selectedItem = item;
                        context.setSelectedItem(item);
                        return false;
                    });
                    view.setOnCreateContextMenuListener(QueueSection.this);
                });

        super.addSectionTo(parent);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return DBReader.getPausedQueue(5);
    }

    @Override
    public void updateItems() {
        slush.getItemListEditor().changeAll(loadItems());
    }


}
