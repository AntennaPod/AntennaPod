package de.danoeh.antennapod.fragment.homesections;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import slush.Slush;
import slush.listeners.OnBindListener;


public class QueueSection extends HomeSection {

    public QueueSection(Fragment context) {
        super(context);
        sectionTitle = context.getString(R.string.queue_label);
        sectionNavigateTitle = context.getString(R.string.inbox_label);
        //sectionFragment = new PowerEpisodesFragment();
        //((PowerEpisodesFragment) sectionFragment).hideToolbar = true;
        //expandsToFillHeight = true;

        new Slush.SingleType<FeedItem>()
                .setItemLayout(R.layout.feeditemlist_item)
                .setLayoutManager(new LinearLayoutManager(context.getContext(), RecyclerView.VERTICAL, true))
                .onBind((view, feedItem) -> {
                    ((TextView)view.findViewById(R.id.txtvTitle)).setText(feedItem.getTitle());
                })
                .setItems(loadItems())
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
        return DBReader.getNewItemsList(0, 10);
    }

}
