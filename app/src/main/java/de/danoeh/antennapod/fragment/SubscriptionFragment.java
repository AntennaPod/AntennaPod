package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.adapter.SubscriptionsAdapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBReader;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Fragment for displaying feed subscriptions
 */
public class SubscriptionFragment extends Fragment {

    public static final String TAG = "SubscriptionFragment";

    private GridView mSubscriptionGridLayout;
    private DBReader.NavDrawerData mDrawerData;
    private SubscriptionsAdapter mSubscriptionAdapter;
    private NavListAdapter.ItemAccess mItemAccess;

    private List<Feed> mSubscriptionList = new ArrayList<>();


    public SubscriptionFragment() {
    }


    public void setItemAccess(NavListAdapter.ItemAccess itemAccess) {
        mItemAccess = itemAccess;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscriptions, container, false);
        mSubscriptionGridLayout = (GridView) root.findViewById(R.id.subscriptions_grid);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSubscriptionAdapter = new SubscriptionsAdapter(getActivity(), mItemAccess);

        mSubscriptionGridLayout.setAdapter(mSubscriptionAdapter);

        Observable.fromCallable(() -> loadData())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    mDrawerData = result;
                    mSubscriptionList = mDrawerData.feeds;
                    mSubscriptionAdapter.setItemAccess(mItemAccess);
                    mSubscriptionAdapter.notifyDataSetChanged();
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                });


        mSubscriptionGridLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EventBus.getDefault().post(new SubscriptionEvent(mSubscriptionList.get(position)));
            }
        });

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.subscriptions_label);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public class SubscriptionEvent {
        public final Feed feed;

        SubscriptionEvent(Feed f) {
            feed = f;
        }
    }


    private DBReader.NavDrawerData loadData() {
        return DBReader.getNavDrawerData();
    }
}
