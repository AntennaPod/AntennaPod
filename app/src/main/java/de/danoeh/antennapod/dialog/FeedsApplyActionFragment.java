package de.danoeh.antennapod.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemPermutors;
import de.danoeh.antennapod.core.util.FeedPermutors;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.FeedPermutors.SortOrder;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("NewApi")
public class FeedsApplyActionFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    private static final String TAG = "FeedsApplyActionFragmen";
    public static final int ACTION_REMOVE_NEW_FLAGS = 1;
    public static final int ACTION_MARK_AS_PLAYED = 2;
    public static final int ACTION_REMOVE_FEED = 3;
    public static final int ACTION_ALL = ACTION_REMOVE_NEW_FLAGS | ACTION_MARK_AS_PLAYED
            | ACTION_REMOVE_FEED;


    /**
     * Specify an action (defined by #flag) 's UI bindings.
     *
     * Includes: the menu / action item and the actual logic
     */
    private static class ActionBinding {
        int flag;
        @IdRes
        final int actionItemId;
        @NonNull
        final Runnable action;

        ActionBinding(int flag, @IdRes int actionItemId, @NonNull Runnable action) {
            this.flag = flag;
            this.actionItemId = actionItemId;
            this.action = action;
        }
    }

    private final List<? extends ActionBinding> actionBindings;
    private final Map<Long, Feed> idMap = new ArrayMap<>();
//    private final List<FeedItem> episodes = new ArrayList<>();
    private int actions;
    private final List<String> titles = new ArrayList<>();
    private final LongList checkedIds = new LongList();

    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private SpeedDialView mSpeedDialView;
    private androidx.appcompat.widget.Toolbar toolbar;


    private final List<Feed> feeds = new ArrayList<>();

    public FeedsApplyActionFragment() {
        actionBindings = Arrays.asList(
                new ActionBinding(ACTION_REMOVE_NEW_FLAGS,
                        R.id.remove_new_flag_batch, this::removeNewFlags),
                new ActionBinding(ACTION_MARK_AS_PLAYED,
                        R.id.mark_all_read_batch, this::markAsPlayed)
        );
    }

    public static FeedsApplyActionFragment newInstance(List<Feed> feeds, int actions) {
        FeedsApplyActionFragment f = new FeedsApplyActionFragment();
        f.feeds.addAll(feeds);
        for (Feed feed : feeds) {
            f.idMap.put(feed.getId(), feed);
        }
        f.actions = actions;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.feeds_apply_action_fragment, container, false);
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.feeds_apply_action_options);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(this);

        mListView = view.findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setOnItemClickListener((listView, view1, position, rowId) -> {
            long id = feeds.get(position).getId();
            if (checkedIds.contains(id)) {
                checkedIds.remove(id);
            } else {
                checkedIds.add(id);
            }
            refreshCheckboxes();
        });
        mListView.setOnItemLongClickListener((adapterView, view12, position, id) -> {
            new AlertDialog.Builder(getActivity())
                    .setItems(R.array.batch_long_press_options, (dialogInterface, item) -> {
                        int direction;
                        if (item == 0) {
                            direction = -1;
                        } else {
                            direction = 1;
                        }

                        int currentPosition = position + direction;
                        while (currentPosition >= 0 && currentPosition < feeds.size()) {
                            long id1 = feeds.get(currentPosition).getId();
                            if (!checkedIds.contains(id1)) {
                                checkedIds.add(id1);
                            }
                            currentPosition += direction;
                        }
                        refreshCheckboxes();
                    }).show();
            return true;
        });

        for (Feed feed : feeds) {
            titles.add(feed.getTitle());
        }

        mAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.simple_list_item_multiple_choice_on_start, titles);
        mListView.setAdapter(mAdapter);

        // Init action UI (via a FAB Speed Dial)
        mSpeedDialView = view.findViewById(R.id.fabSD);
        mSpeedDialView.inflate(R.menu.feeds_apply_action_speeddial);

        mSpeedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean open) {
                if (open && checkedIds.size() == 0) {
                    ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                            Snackbar.LENGTH_SHORT);
                    mSpeedDialView.close();
                }
            }
        });

        // show only specified actions, and bind speed dial UIs to the actual logic
//        for (ActionBinding binding : actionBindings) {
//            if ((actions & binding.flag) == 0) {
//                mSpeedDialView.removeActionItemById(binding.actionItemId);
//            }
//        }
        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            ActionBinding selectedBinding = null;
            for (ActionBinding binding : actionBindings) {
                if (actionItem.getId() == binding.actionItemId) {
                    selectedBinding = binding;
                    break;
                }
            }
            if (selectedBinding != null) {
                selectedBinding.action.run();
            } else {
                Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + actionItem.getId());
            }
            return true;
        });
        refreshCheckboxes();
        return view;
    }

    private void refreshCheckboxes() {
        for (int i = 0; i < feeds.size(); i++) {
            Feed episode = feeds.get(i);
            boolean checked = checkedIds.contains(episode.getId());
            mListView.setItemChecked(i, checked);
        }
        refreshToolbarState();
        toolbar.setTitle(getResources().getQuantityString(R.plurals.num_selected_label,
                checkedIds.size(), checkedIds.size()));
    }

    public void refreshToolbarState() {
        MenuItem selectAllItem = toolbar.getMenu().findItem(R.id.select_toggle);
        if (checkedIds.size() == feeds.size()) {
            selectAllItem.setIcon(ThemeUtils.getDrawableFromAttr(getContext(), R.attr.ic_select_none));
            selectAllItem.setTitle(R.string.deselect_all_label);
        } else {
            selectAllItem.setIcon(ThemeUtils.getDrawableFromAttr(getContext(), R.attr.ic_select_all));
            selectAllItem.setTitle(R.string.select_all_label);
        }
    }
    private static final Map<Integer, SortOrder> menuItemIdToSortOrder;
    static {
        Map<Integer, SortOrder> map = new ArrayMap<>();
//        map.put(R.id.sort_date_new_old, SortOrder.DATE_NEW_OLD);
//        map.put(R.id.sort_date_old_new, SortOrder.DATE_OLD_NEW);
        map.put(R.id.sort_title_a_z, SortOrder.FEED_TITLE_A_Z);
        map.put(R.id.sort_title_z_a, SortOrder.FEED_TITLE_Z_A);
        menuItemIdToSortOrder = Collections.unmodifiableMap(map);
    }
    /** Speeddial Actions **/

    private void removeNewFlags() {
        for (int i = 0; i < checkedIds.size(); ++i) {
            DBWriter.removeFeedNewFlag(checkedIds.get(i));
        }
//        close("Removed new flags", );
        getActivity().getSupportFragmentManager().popBackStack();
    }

    private void markAsPlayed() {
        for (int i = 0; i < checkedIds.size(); ++i) {
            DBWriter.markFeedRead(checkedIds.get(i));
        }
        getActivity().getSupportFragmentManager().popBackStack();

    }

    private void remove(Context context, Runnable onSuccess) {
//
//        ProgressDialog progressDialog = new ProgressDialog(context);
//        progressDialog.setMessage(context.getString(R.string.feed_remover_msg));
//        progressDialog.setIndeterminate(true);
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//
//        for (int i = 0; i < checkedIds.size(); ++i) {
//            int finalI = i;
//            Completable.fromCallable(() ->
//                    DBWriter.deleteFeed(context, checkedIds.get(finalI)).get())
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(
//                            () -> {
//                                Log.d(TAG, "Feed was deleted");
//                                if (onSuccess != null) {
//                                    onSuccess.run();
//                                }
//                                progressDialog.dismiss();
//                            }, error -> {
//                                Log.e(TAG, Log.getStackTraceString(error));
//                                progressDialog.dismiss();
//
//                            });
//        }
    }

    /** end **/
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        @StringRes int resId = 0;

        switch (item.getItemId()) {
            case R.id.select_options:
                return true;
//            case R.id.select_toggle:
//                if (checkedIds.size() == episodes.size()) {
//                    checkNone();
//                } else {
//                    checkAll();
//                }
//                return true;
            case R.id.check_all:
                checkAll();
                resId = R.string.selected_all_feeds_label;
                break;
            case R.id.check_none:
                checkNone();
                resId = R.string.deselected_all_label;
                break;
            default: // handle various sort options
                SortOrder sortOrder = menuItemIdToSortOrder.get(item.getItemId());
                if (sortOrder != null) {
                    sort(sortOrder);
                    return true;
                }
        }
        if (resId != 0) {
            ((MainActivity) getActivity()).showSnackbarAbovePlayer(resId, Snackbar.LENGTH_SHORT);
            return true;
        } else {
            return false;
        }
    }

    private void sort(@NonNull SortOrder sortOrder) {
        FeedPermutors.getPermutor(sortOrder)
                .reorder(feeds);
        refreshTitles();
        refreshCheckboxes();
    }

    private void refreshTitles() {
        titles.clear();
        for (Feed feed : feeds) {
            titles.add(feed.getTitle());
        }
        mAdapter.notifyDataSetChanged();
    }


    /** Filter actions **/
    private void checkAll() {
        for (Feed feed: feeds) {
            if (!checkedIds.contains(feed.getId())) {
                checkedIds.add(feed.getId());
            }
        }
        refreshCheckboxes();
    }

    private void checkNone() {
        checkedIds.clear();
        refreshCheckboxes();
    }

    /** end **/

    private void close(@PluralsRes int msgId, int numItems) {
        ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                getResources().getQuantityString(msgId, numItems, numItems), Snackbar.LENGTH_LONG);
        getActivity().getSupportFragmentManager().popBackStack();
    }
}
