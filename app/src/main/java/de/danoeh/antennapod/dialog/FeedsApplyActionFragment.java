package de.danoeh.antennapod.dialog;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import androidx.appcompat.widget.Toolbar;

import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.LongList;
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
    private final Map<Long, FeedItem> idMap = new ArrayMap<>();
    private final List<FeedItem> episodes = new ArrayList<>();
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
                        R.id.add_to_queue_batch, this::removeNewFlags)
//                new ActionBinding(ACTION_REMOVE_FROM_QUEUE,
//                        R.id.remove_from_queue_batch, this::removeFromQueueChecked),
//                new ActionBinding(ACTION_MARK_PLAYED,
//                        R.id.mark_read_batch, this::markedCheckedPlayed),
//                new ActionBinding(ACTION_MARK_UNPLAYED,
//                        R.id.mark_unread_batch, this::markedCheckedUnplayed),
//                new ActionBinding(ACTION_DOWNLOAD,
//                        R.id.download_batch, this::downloadChecked),
//                new ActionBinding(ACTION_DELETE,
//                        R.id.delete_batch, this::deleteChecked)
        );
    }

    public static FeedsApplyActionFragment newInstance(List<Feed> feeds, int actions) {
        FeedsApplyActionFragment f = new FeedsApplyActionFragment();
        f.feeds.addAll(feeds);
        for (Feed feed : feeds) {

        }

        return f;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
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
//                if (open && checkedIds.size() == 0) {
//                    ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
//                            Snackbar.LENGTH_SHORT);
//                    mSpeedDialView.close();
//                }
            }
        });
        return view;
    }

    private void removeNewFlags() {
        for (int i = 0; i < checkedIds.size(); ++i) {
            DBWriter.removeFeedNewFlag(checkedIds.get(i));
        }
    }

    private void markAsPlayed() {
        for (int i = 0; i < checkedIds.size(); ++i) {
            DBWriter.markFeedRead(checkedIds.get(i));
        }
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


}
