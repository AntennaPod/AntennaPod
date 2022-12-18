package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.HorizontalFeedListAdapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.home.HomeSection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubscriptionsSection extends HomeSection {
    public static final String TAG = "SubscriptionsSection";
    private static final int NUM_FEEDS = 8;
    private HorizontalFeedListAdapter listAdapter;
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        viewBinding.recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        listAdapter = new HorizontalFeedListAdapter((MainActivity) getActivity());
        listAdapter.setDummyViews(NUM_FEEDS);
        viewBinding.recyclerView.setAdapter(listAdapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    protected void handleMoreClick() {
        ((MainActivity) requireActivity()).loadChildFragment(new SubscriptionFragment());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadItems();
    }

    @Override
    protected String getSectionTitle() {
        return getString(R.string.home_classics_title);
    }

    @Override
    protected String getMoreLinkTitle() {
        return getString(R.string.subscriptions_label);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getStatistics(true, 0, Long.MAX_VALUE).feedTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statisticsData -> {
                    Collections.sort(statisticsData, (item1, item2) ->
                            Long.compare(item2.timePlayed, item1.timePlayed));
                    List<Feed> feeds = new ArrayList<>();
                    for (int i = 0; i < statisticsData.size() && i < NUM_FEEDS; i++) {
                        feeds.add(statisticsData.get(i).feed);
                    }
                    listAdapter.setDummyViews(0);
                    listAdapter.updateData(feeds);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
