package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.SubscriptionsFilterDialog;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class NavDrawerFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    @VisibleForTesting
    public static final String PREF_LAST_FRAGMENT_TAG = "prefLastFragmentTag";
    @VisibleForTesting
    public static final String PREF_NAME = "NavDrawerPrefs";
    public static final String TAG = "NavDrawerFragment";

    public static final String[] NAV_DRAWER_TAGS = {
            QueueFragment.TAG,
            EpisodesFragment.TAG,
            SubscriptionFragment.TAG,
            DownloadsFragment.TAG,
            PlaybackHistoryFragment.TAG,
            AddFeedFragment.TAG,
            NavListAdapter.SUBSCRIPTION_LIST_TAG
    };

    private DBReader.NavDrawerData navDrawerData;
    private int selectedNavListIndex = -1;
    private int position = -1;
    private NavListAdapter navAdapter;
    private Disposable disposable;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.nav_list, container, false);

        progressBar = root.findViewById(R.id.progressBar);
        ListView navList = root.findViewById(R.id.nav_list);
        navAdapter = new NavListAdapter(itemAccess, getActivity());
        navList.setAdapter(navAdapter);
        navList.setOnItemClickListener(this);
        navList.setOnItemLongClickListener(this);
        registerForContextMenu(navList);
        updateSelection();

        root.findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), PreferenceActivity.class)));
        getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);
        return root;
    }

    private void updateSelection() {
        String lastNavFragment = getLastNavFragment(getContext());
        int tagIndex = navAdapter.getTags().indexOf(lastNavFragment);
        if (tagIndex >= 0) {
            selectedNavListIndex = tagIndex;
        } else if (StringUtils.isNumeric(lastNavFragment)) { // last fragment was not a list, but a feed
            long feedId = Long.parseLong(lastNavFragment);
            if (navDrawerData != null) {
                List<Feed> feeds = navDrawerData.feeds;
                for (int i = 0; i < feeds.size(); i++) {
                    if (feeds.get(i).getId() == feedId) {
                        selectedNavListIndex = navAdapter.getSubscriptionOffset() + i;
                        break;
                    }
                }
            }
        }
        navAdapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() != R.id.nav_list) {
            return;
        }
        AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = adapterInfo.position;
        if (position < navAdapter.getSubscriptionOffset()) {
            return;
        }
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.nav_feed_context, menu);
        Feed feed = navDrawerData.feeds.get(position - navAdapter.getSubscriptionOffset());
        menu.setHeaderTitle(feed.getTitle());
        // episodes are not loaded, so we cannot check if the podcast has new or unplayed ones!
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final int position = this.position;
        this.position = -1; // reset
        if (position < 0) {
            return false;
        }
        Feed feed = navDrawerData.feeds.get(position - navAdapter.getSubscriptionOffset());
        switch (item.getItemId()) {
            case R.id.remove_all_new_flags_item:
                ConfirmationDialog removeAllNewFlagsConfirmationDialog = new ConfirmationDialog(getContext(),
                        R.string.remove_all_new_flags_label,
                        R.string.remove_all_new_flags_confirmation_msg) {
                    @Override
                    public void onConfirmButtonPressed(DialogInterface dialog) {
                        dialog.dismiss();
                        DBWriter.removeFeedNewFlag(feed.getId());
                    }
                };
                removeAllNewFlagsConfirmationDialog.createNewDialog().show();
                return true;
            case R.id.mark_all_read_item:
                ConfirmationDialog markAllReadConfirmationDialog = new ConfirmationDialog(getContext(),
                        R.string.mark_all_read_label,
                        R.string.mark_all_read_confirmation_msg) {

                    @Override
                    public void onConfirmButtonPressed(DialogInterface dialog) {
                        dialog.dismiss();
                        DBWriter.markFeedRead(feed.getId());
                    }
                };
                markAllReadConfirmationDialog.createNewDialog().show();
                return true;
            case R.id.rename_item:
                new RenameFeedDialog(getActivity(), feed).show();
                return true;
            case R.id.remove_item:
                RemoveFeedDialog.show(getContext(), feed, () -> {
                    if (selectedNavListIndex == position) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).loadFragment(EpisodesFragment.TAG, null);
                        } else {
                            showMainActivity(EpisodesFragment.TAG);
                        }
                    }
                });
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showMainActivity(String tag) {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, tag);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadData();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQueueChanged(QueueEvent event) {
        Log.d(TAG, "onQueueChanged(" + event + ")");
        // we are only interested in the number of queue items, not download status or position
        if (event.action == QueueEvent.Action.DELETED_MEDIA
                || event.action == QueueEvent.Action.SORTED
                || event.action == QueueEvent.Action.MOVED) {
            return;
        }
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void showDrawerPreferencesDialog() {
        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        String[] navLabels = new String[NAV_DRAWER_TAGS.length];
        final boolean[] checked = new boolean[NAV_DRAWER_TAGS.length];
        for (int i = 0; i < NAV_DRAWER_TAGS.length; i++) {
            String tag = NAV_DRAWER_TAGS[i];
            navLabels[i] = navAdapter.getLabel(tag);
            if (!hiddenDrawerItems.contains(tag)) {
                checked[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.drawer_preferences);
        builder.setMultiChoiceItems(navLabels, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                hiddenDrawerItems.remove(NAV_DRAWER_TAGS[which]);
            } else {
                hiddenDrawerItems.add(NAV_DRAWER_TAGS[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            UserPreferences.setHiddenDrawerItems(hiddenDrawerItems);
            updateSelection();
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private final NavListAdapter.ItemAccess itemAccess = new NavListAdapter.ItemAccess() {
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
        public int getSelectedItemIndex() {
            return selectedNavListIndex;
        }

        @Override
        public int getQueueSize() {
            return (navDrawerData != null) ? navDrawerData.queueSize : 0;
        }

        @Override
        public int getNumberOfNewItems() {
            return (navDrawerData != null) ? navDrawerData.numNewItems : 0;
        }

        @Override
        public int getNumberOfDownloadedItems() {
            return (navDrawerData != null) ? navDrawerData.numDownloadedItems : 0;
        }

        @Override
        public int getReclaimableItems() {
            return (navDrawerData != null) ? navDrawerData.reclaimableSpace : 0;
        }

        @Override
        public int getFeedCounter(long feedId) {
            return navDrawerData != null ? navDrawerData.feedCounters.get(feedId) : 0;
        }

        @Override
        public int getFeedCounterSum() {
            if (navDrawerData == null) {
                return 0;
            }
            int sum = 0;
            for (int counter : navDrawerData.feedCounters.values()) {
                sum += counter;
            }
            return sum;
        }

    };

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        disposable = Observable.fromCallable(DBReader::getNavDrawerData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            navDrawerData = result;
                            updateSelection(); // Selected item might be a feed
                            navAdapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            progressBar.setVisibility(View.GONE);
                        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int viewType = parent.getAdapter().getItemViewType(position);
        if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER) {
            if (position < navAdapter.getSubscriptionOffset()) {
                String tag = navAdapter.getTags().get(position);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(tag, null);
                    ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    showMainActivity(tag);
                }
            } else {
                int pos = position - navAdapter.getSubscriptionOffset();
                long feedId = navDrawerData.feeds.get(pos).getId();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFeedFragmentById(feedId, null);
                    ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra(MainActivity.EXTRA_FEED_ID, feedId);
                    startActivity(intent);
                }
            }
        } else if (UserPreferences.getSubscriptionsFilter().isEnabled()
                && navAdapter.showSubscriptionList) {
            SubscriptionsFilterDialog.showDialog(requireContext());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < navAdapter.getTags().size()) {
            showDrawerPreferencesDialog();
            return true;
        } else {
            this.position = position;
            return false;
        }
    }

    public static void saveLastNavFragment(Context context, String tag) {
        Log.d(TAG, "saveLastNavFragment(tag: " + tag + ")");
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        if (tag != null) {
            edit.putString(PREF_LAST_FRAGMENT_TAG, tag);
        } else {
            edit.remove(PREF_LAST_FRAGMENT_TAG);
        }
        edit.apply();
    }

    public static String getLastNavFragment(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastFragment = prefs.getString(PREF_LAST_FRAGMENT_TAG, QueueFragment.TAG);
        Log.d(TAG, "getLastNavFragment() -> " + lastFragment);
        return lastFragment;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_LAST_FRAGMENT_TAG.equals(key)) {
            updateSelection();
            navAdapter.notifyDataSetChanged();
        }
    }
}
