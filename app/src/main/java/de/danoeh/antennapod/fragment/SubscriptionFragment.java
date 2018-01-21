package de.danoeh.antennapod.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SubscriptionsAdapter;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Fragment for displaying feed subscriptions
 */
public class SubscriptionFragment extends Fragment {

    public static final String TAG = "SubscriptionFragment";

    private static final int EVENTS = EventDistributor.FEED_LIST_UPDATE
            | EventDistributor.UNREAD_ITEMS_UPDATE;

    private GridView subscriptionGridLayout;
    private DBReader.NavDrawerData navDrawerData;
    private SubscriptionsAdapter subscriptionAdapter;

    private int mPosition = -1;

    private Subscription subscription;

    public SubscriptionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // So, we certainly *don't* have an options menu,
        // but unless we say we do, old options menus sometimes
        // persist.  mfietz thinks this causes the ActionBar to be invalidated
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscriptions, container, false);
        subscriptionGridLayout = (GridView) root.findViewById(R.id.subscriptions_grid);
        registerForContextMenu(subscriptionGridLayout);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        subscriptionAdapter = new SubscriptionsAdapter((MainActivity)getActivity(), itemAccess);

        subscriptionGridLayout.setAdapter(subscriptionAdapter);

        loadSubscriptions();

        subscriptionGridLayout.setOnItemClickListener(subscriptionAdapter);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.subscriptions_label);
        }

        EventDistributor.getInstance().register(contentUpdate);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    private void loadSubscriptions() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        subscription = Observable.fromCallable(DBReader::getNavDrawerData)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    navDrawerData = result;
                    subscriptionAdapter.notifyDataSetChanged();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = adapterInfo.position;

        Object selectedObject = subscriptionAdapter.getItem(position);
        if (selectedObject.equals(SubscriptionsAdapter.ADD_ITEM_OBJ)) {
            mPosition = position;
            return;
        }

        Feed feed = (Feed)selectedObject;

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.nav_feed_context, menu);

        menu.setHeaderTitle(feed.getTitle());

        mPosition = position;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final int position = mPosition;
        mPosition = -1; // reset
        if(position < 0) {
            return false;
        }

        Object selectedObject = subscriptionAdapter.getItem(position);
        if (selectedObject.equals(SubscriptionsAdapter.ADD_ITEM_OBJ)) {
            // this is the add object, do nothing
            return false;
        }

        Feed feed = (Feed)selectedObject;
        switch(item.getItemId()) {
            case R.id.mark_all_seen_item:
                Observable.fromCallable(() -> DBWriter.markFeedSeen(feed.getId()))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> loadSubscriptions(),
                                error -> Log.e(TAG, Log.getStackTraceString(error)));
                return true;
            case R.id.mark_all_read_item:
                Observable.fromCallable(() -> DBWriter.markFeedRead(feed.getId()))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> loadSubscriptions(),
                                error -> Log.e(TAG, Log.getStackTraceString(error)));
                return true;
            case R.id.rename_item:
                new RenameFeedDialog(getActivity(), feed).show();
                return true;
            case R.id.remove_item:
                final FeedRemover remover = new FeedRemover(getContext(), feed) {
                    @Override
                    protected void onPostExecute(Void result) {
                        super.onPostExecute(result);
                        loadSubscriptions();
                    }
                };
                ConfirmationDialog conDialog = new ConfirmationDialog(getContext(),
                        R.string.remove_feed_label,
                        getString(R.string.feed_delete_confirmation_msg, feed.getTitle())) {
                    @Override
                    public void onConfirmButtonPressed(
                            DialogInterface dialog) {
                        dialog.dismiss();
                        long mediaId = PlaybackPreferences.getCurrentlyPlayingFeedMediaId();
                        if (mediaId > 0 &&
                                FeedItemUtil.indexOfItemWithMediaId(feed.getItems(), mediaId) >= 0) {
                            Log.d(TAG, "Currently playing episode is about to be deleted, skipping");
                            remover.skipOnCompletion = true;
                            int playerStatus = PlaybackPreferences.getCurrentPlayerStatus();
                            if(playerStatus == PlaybackPreferences.PLAYER_STATUS_PLAYING) {
                                getActivity().sendBroadcast(new Intent(
                                        PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE));
                            }
                        }
                        remover.executeAsync();
                    }
                };
                conDialog.createNewDialog().show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSubscriptions();
    }

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EVENTS & arg) != 0) {
                Log.d(TAG, "Received contentUpdate Intent.");
                loadSubscriptions();
            }
        }
    };

    private final SubscriptionsAdapter.ItemAccess itemAccess = new SubscriptionsAdapter.ItemAccess() {
        @Override
        public int getCount() {
            if (navDrawerData != null) {
                return navDrawerData.feeds.size();
            } else {
                return 0;
            }
        }

        @Override
        public Feed getItem(int position) {
            if (navDrawerData != null && 0 <= position && position < navDrawerData.feeds.size()) {
                return navDrawerData.feeds.get(position);
            } else {
                return null;
            }
        }

        @Override
        public int getFeedCounter(long feedId) {
            return navDrawerData != null ? navDrawerData.feedCounters.get(feedId) : 0;
        }
    };
}
