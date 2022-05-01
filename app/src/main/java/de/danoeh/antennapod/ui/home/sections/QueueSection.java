package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.HorizontalItemListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.ui.home.HomeSection;

public class QueueSection extends HomeSection {
    public static final String TAG = "QueueSection";
    private HorizontalItemListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        listAdapter = new HorizontalItemListAdapter((MainActivity) getActivity());
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        viewBinding.recyclerView.setAdapter(listAdapter);
        loadItems();
        return view;
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new QueueFragment());
    }

    /*@Override
    protected Unit onItemClick(View view, FeedItem feedItem) {
        boolean isPlaying = FeedItemUtil.isCurrentlyPlaying(feedItem.getMedia());
        if (isPlaying) {
            IntentUtils.sendLocalBroadcast(requireContext(), ACTION_PAUSE_PLAY_CURRENT_EPISODE);
        } else {
            new PlaybackServiceStarter(requireContext(), feedItem.getMedia())
                    .callEvenIfRunning(true)
                    .start();
        }
        playPauseIcon(view.findViewById(R.id.play_icon), !isPlaying);
        return null;
    }*/

    @Override
    protected String getSectionTitle() {
        return getString(R.string.continue_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.queue_label);
    }

    private void loadItems() {
        listAdapter.updateData(DBReader.getPausedQueue(5));
    }
}
