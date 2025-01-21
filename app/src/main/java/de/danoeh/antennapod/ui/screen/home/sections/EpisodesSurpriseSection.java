package de.danoeh.antennapod.ui.screen.home.sections;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.ui.episodeslist.HorizontalItemListAdapter;
import de.danoeh.antennapod.ui.episodeslist.HorizontalItemViewHolder;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.home.HomeSection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EpisodesSurpriseSection extends HomeSection {
    public static final String TAG = "EpisodesSurpriseSection";
    private static final int NUM_EPISODES = 8;
    private static int seed = 0;
    private HorizontalItemListAdapter listAdapter;
    private Disposable disposable;
    private List<FeedItem> episodes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.shuffleButton.setVisibility(View.VISIBLE);
        viewBinding.shuffleButton.setOnClickListener(v -> {
            seed = new Random().nextInt();
            viewBinding.recyclerView.scrollToPosition(0);
            loadItems();
        });
        listAdapter = new HorizontalItemListAdapter((MainActivity) getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                MenuItemUtils.setOnClickListeners(menu, EpisodesSurpriseSection.this::onContextItemSelected);
            }
        };
        listAdapter.setDummyViews(NUM_EPISODES);
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        viewBinding.recyclerView.setAdapter(listAdapter);
        int paddingHorizontal = (int) (12 * getResources().getDisplayMetrics().density);
        viewBinding.recyclerView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        if (seed == 0) {
            seed = new Random().nextInt();
        }
        viewBinding.emptyLabel.setText(R.string.home_no_recent_unplayed_episodes_text);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new AllEpisodesFragment());
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_surprise_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.episodes_label_more);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemEvent.indexOfItemWithId(episodes, item.getId());
            if (pos >= 0) {
                episodes.remove(pos);
                episodes.add(pos, item);
                listAdapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EpisodeDownloadEvent event) {
        for (String downloadUrl : event.getUrls()) {
            int pos = EpisodeDownloadEvent.indexOfItemWithDownloadUrl(episodes, downloadUrl);
            if (pos >= 0) {
                listAdapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedListUpdateEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (listAdapter == null) {
            return;
        }
        for (int i = 0; i < listAdapter.getItemCount(); i++) {
            HorizontalItemViewHolder holder = (HorizontalItemViewHolder)
                    viewBinding.recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null && holder.isCurrentlyPlayingItem()) {
                holder.notifyPlaybackPositionUpdated(event);
                break;
            }
        }
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getRandomEpisodes(NUM_EPISODES, seed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(episodes -> {
                    this.episodes = episodes;
                    listAdapter.setDummyViews(0);
                    listAdapter.updateData(episodes);

                    boolean isShuffleable = !episodes.isEmpty();
                    viewBinding.shuffleButton.setVisibility(isShuffleable ? View.VISIBLE : View.GONE);
                    viewBinding.recyclerView.setVisibility(isShuffleable ? View.VISIBLE : View.GONE);
                    viewBinding.emptyLabel.setVisibility(!isShuffleable ? View.VISIBLE : View.GONE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
