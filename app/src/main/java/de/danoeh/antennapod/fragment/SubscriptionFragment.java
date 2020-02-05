package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.Callable;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SubscriptionsAdapter;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Fragment for displaying feed subscriptions
 */
public class SubscriptionFragment extends Fragment {

    public static final String TAG = "SubscriptionFragment";
    private static final String PREFS = "SubscriptionFragment";
    private static final String PREF_NUM_COLUMNS = "columns";

    private GridView subscriptionGridLayout;
    private DBReader.NavDrawerData navDrawerData;
    private SubscriptionsAdapter subscriptionAdapter;
    private FloatingActionButton subscriptionAddButton;
    private EmptyViewHandler emptyView;

    private int mPosition = -1;
    private boolean isUpdatingFeeds = false;

    private Disposable disposable;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscriptions, container, false);
        subscriptionGridLayout = root.findViewById(R.id.subscriptions_grid);
        subscriptionGridLayout.setNumColumns(prefs.getInt(PREF_NUM_COLUMNS, 3));
        registerForContextMenu(subscriptionGridLayout);
        subscriptionAddButton = root.findViewById(R.id.subscriptions_add);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.subscriptions, menu);

        int columns = prefs.getInt(PREF_NUM_COLUMNS, 3);
        menu.findItem(R.id.subscription_num_columns_2).setChecked(columns == 2);
        menu.findItem(R.id.subscription_num_columns_3).setChecked(columns == 3);
        menu.findItem(R.id.subscription_num_columns_4).setChecked(columns == 4);
        menu.findItem(R.id.subscription_num_columns_5).setChecked(columns == 5);

        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.refresh_item:
                AutoUpdateManager.runImmediate(requireContext());
                return true;
            case R.id.subscription_num_columns_2:
                setColumnNumber(2);
                return true;
            case R.id.subscription_num_columns_3:
                setColumnNumber(3);
                return true;
            case R.id.subscription_num_columns_4:
                setColumnNumber(4);
                return true;
            case R.id.subscription_num_columns_5:
                setColumnNumber(5);
                return true;
            default:
                return false;
        }
    }

    private void setColumnNumber(int columns) {
        subscriptionGridLayout.setNumColumns(columns);
        prefs.edit().putInt(PREF_NUM_COLUMNS, columns).apply();
        getActivity().invalidateOptionsMenu();
    }

    private void setupEmptyView() {
        emptyView = new EmptyViewHandler(getContext());
        emptyView.setIcon(R.attr.ic_folder);
        emptyView.setTitle(R.string.no_subscriptions_head_label);
        emptyView.setMessage(R.string.no_subscriptions_label);
        emptyView.attachToListView(subscriptionGridLayout);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        subscriptionAdapter = new SubscriptionsAdapter((MainActivity) getActivity(), itemAccess);
        subscriptionGridLayout.setAdapter(subscriptionAdapter);
        subscriptionGridLayout.setOnItemClickListener(subscriptionAdapter);
        setupEmptyView();

        subscriptionAddButton.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadChildFragment(new AddFeedFragment());
            }
        });

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.subscriptions_label);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadSubscriptions();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if(disposable != null) {
            disposable.dispose();
        }
    }

    private void loadSubscriptions() {
        if(disposable != null) {
            disposable.dispose();
        }
        emptyView.hide();
        disposable = Observable.fromCallable(DBReader::getNavDrawerData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    navDrawerData = result;
                    subscriptionAdapter.notifyDataSetChanged();
                    emptyView.updateVisibility();
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

        MenuInflater inflater = requireActivity().getMenuInflater();
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
            case R.id.remove_all_new_flags_item:
                displayConfirmationDialog(
                        R.string.remove_all_new_flags_label,
                        R.string.remove_all_new_flags_confirmation_msg,
                        () -> DBWriter.removeFeedNewFlag(feed.getId()));
                return true;
            case R.id.mark_all_read_item:
                displayConfirmationDialog(
                        R.string.mark_all_read_label,
                        R.string.mark_all_read_confirmation_msg,
                        () -> DBWriter.markFeedRead(feed.getId()));
                return true;
            case R.id.rename_item:
                new RenameFeedDialog(getActivity(), feed).show();
                return true;
            case R.id.remove_item:
                displayRemoveFeedDialog(feed);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void displayRemoveFeedDialog(Feed feed) {
        final FeedRemover remover = new FeedRemover(getContext(), feed) {
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                loadSubscriptions();
            }
        };

        String message = getString(R.string.feed_delete_confirmation_msg, feed.getTitle());
        ConfirmationDialog dialog = new ConfirmationDialog(getContext(), R.string.remove_feed_label, message) {
            @Override
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();
                long mediaId = PlaybackPreferences.getCurrentlyPlayingFeedMediaId();
                if (mediaId > 0 && FeedItemUtil.indexOfItemWithMediaId(feed.getItems(), mediaId) >= 0) {
                    Log.d(TAG, "Currently playing episode is about to be deleted, skipping");
                    remover.skipOnCompletion = true;
                    int playerStatus = PlaybackPreferences.getCurrentPlayerStatus();
                    if(playerStatus == PlaybackPreferences.PLAYER_STATUS_PLAYING) {
                        IntentUtils.sendLocalBroadcast(getContext(), PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE);

                    }
                }
                remover.executeAsync();
            }
        };
        dialog.createNewDialog().show();
    }

    private <T> void displayConfirmationDialog(@StringRes int title, @StringRes int message, Callable<? extends T> task) {
        ConfirmationDialog dialog = new ConfirmationDialog(getActivity(), title, message) {
            @Override
            @SuppressLint("CheckResult")
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();
                Observable.fromCallable(task)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> loadSubscriptions(),
                                error -> Log.e(TAG, Log.getStackTraceString(error)));
            }
        };
        dialog.createNewDialog().show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadSubscriptions();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadSubscriptions();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();

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
