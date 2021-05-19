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
import de.danoeh.antennapod.core.util.playback.PlaybackServiceStarter;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import kotlin.Unit;
import slush.Slush;


public class QueueSection extends HomeSection {

    public static final String TAG = "QueueSection";

    public QueueSection(Fragment context) {
        super(context);
        sectionTitle = "Continue";
        sectionNavigateTitle = context.getString(R.string.queue_label);
        itemType = ItemType.COVER_LARGE;
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

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return DBReader.getPausedQueue(5);
    }
}
