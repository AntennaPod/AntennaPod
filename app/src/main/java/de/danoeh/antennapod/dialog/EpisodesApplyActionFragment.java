package de.danoeh.antennapod.dialog;

import android.app.AlertDialog;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.leinardi.android.speeddial.SpeedDialView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.LongList;

public class EpisodesApplyActionFragment extends Fragment {

    public static final String TAG = "EpisodeActionFragment";

    public static final int ACTION_QUEUE = 1;
    private static final int ACTION_MARK_PLAYED = 2;
    private static final int ACTION_MARK_UNPLAYED = 4;
    private static final int ACTION_DOWNLOAD = 8;
    public static final int ACTION_REMOVE = 16;
    public static final int ACTION_REMOVE_FROM_QUEUE = 32;
    private static final int ACTION_ALL = ACTION_QUEUE | ACTION_MARK_PLAYED | ACTION_MARK_UNPLAYED
            | ACTION_DOWNLOAD | ACTION_REMOVE;

    private ListView mListView;
    private ArrayAdapter<String> mAdapter;

    private SpeedDialView mSpeedDialView;

    private final Map<Long,FeedItem> idMap = new ArrayMap<>();
    private final List<FeedItem> episodes = new ArrayList<>();
    private int actions;
    private final List<String> titles = new ArrayList<>();
    private final LongList checkedIds = new LongList();

    private MenuItem mSelectToggle;

    public static EpisodesApplyActionFragment newInstance(List<FeedItem> items) {
        return newInstance(items, ACTION_ALL);
    }

    public static EpisodesApplyActionFragment newInstance(List<FeedItem> items, int actions) {
        EpisodesApplyActionFragment f = new EpisodesApplyActionFragment();
        f.episodes.addAll(items);
        for(FeedItem episode : items) {
            f.idMap.put(episode.getId(), episode);
        }
        f.actions = actions;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.episodes_apply_action_fragment, container, false);

        mListView = view.findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setOnItemClickListener((ListView, view1, position, rowId) -> {
            long id = episodes.get(position).getId();
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
                        while (currentPosition >= 0 && currentPosition < episodes.size()) {
                            long id1 = episodes.get(currentPosition).getId();
                            if (!checkedIds.contains(id1)) {
                                checkedIds.add(id1);
                            }
                            currentPosition += direction;
                        }
                        refreshCheckboxes();
                    }).show();
            return true;
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (AbsListView.OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
                    showSpeedDialIfAnyChecked();
                 } else {
                    hideSpeedDial();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });

        for(FeedItem episode : episodes) {
            titles.add(episode.getTitle());
        }

        mAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.simple_list_item_multiple_choice_on_start, titles);
        mListView.setAdapter(mAdapter);
        /// checkAll(); // TODO: no longer check all by default

        // replacement for the buttons
        mSpeedDialView = view.findViewById(R.id.fabSD);
        mSpeedDialView.inflate(R.menu.episodes_apply_action_speeddial);

        // show only specified actions, and bind speed dial UIs to the actual logic
        if((actions & ACTION_QUEUE) == 0) {
            mSpeedDialView.removeActionItemById(R.id.addToQueue);
        }
        if((actions & ACTION_REMOVE_FROM_QUEUE) == 0) {
            mSpeedDialView.removeActionItemById(R.id.removeFromQueue);
        }
        if((actions & ACTION_MARK_PLAYED) == 0) {
            mSpeedDialView.removeActionItemById(R.id.markAsPlayed);
        }
        if((actions & ACTION_MARK_UNPLAYED) == 0) {
            mSpeedDialView.removeActionItemById(R.id.markAsUnplayed);
        }
        if((actions & ACTION_DOWNLOAD) == 0) {
            mSpeedDialView.removeActionItemById(R.id.download);
        }
        if((actions & ACTION_REMOVE) == 0) {
            mSpeedDialView.removeActionItemById(R.id.delete);
        }
        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            switch(actionItem.getId()) {
                case R.id.addToQueue:
                    queueChecked();
                    break;
                case R.id.removeFromQueue:
                    Toast.makeText(getContext(), "To implement: remove from queue", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.markAsPlayed:
                    markedCheckedPlayed();
                    break;
                case R.id.markAsUnplayed:
                    markedCheckedUnplayed();
                    break;
                case R.id.download:
                    downloadChecked();
                    break;
                case R.id.delete:
                    deleteChecked();
                    break;
                default:
                    Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + actionItem.getId());
            }
            return true;
        });

        showSpeedDialIfAnyChecked();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        compatEnsureSpeedDialClickable();
    }

    private void compatEnsureSpeedDialClickable() {
        // On pre-Lolliop devices (that does not support elevation),
        // need to explicitly bring the fab to the front, otherwise it won't be clickable.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ViewGroup root = (ViewGroup)getView();
            root.bringChildToFront(root.findViewById(R.id.fabSDScrollCtr));
            root.requestLayout();
            root.invalidate();
        }
    }

    private void showSpeedDialIfAnyChecked() {
        mSpeedDialView.setVisibility(checkedIds.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private void hideSpeedDial() {
        mSpeedDialView.setVisibility(View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodes_apply_action_options, menu);

        mSelectToggle = menu.findItem(R.id.select_toggle);
        mSelectToggle.setOnMenuItemClickListener(item -> {
            if (checkedIds.size() == episodes.size()) {
                checkNone();
            } else {
                checkAll();
            }
            return true;
        });
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        // Prepare icon for select toggle button

        int[] icon = new int[1];
        if (checkedIds.size() == episodes.size()) {
            icon[0] = R.attr.ic_check_box;
        } else if (checkedIds.size() == 0) {
            icon[0] = R.attr.ic_check_box_outline;
        } else {
            icon[0] = R.attr.ic_indeterminate_check_box;
        }

        TypedArray a = getActivity().obtainStyledAttributes(icon);
        Drawable iconDrawable = a.getDrawable(0);
        a.recycle();

        mSelectToggle.setIcon(iconDrawable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int resId = 0;
        switch(item.getItemId()) {
            case R.id.select_options:
                return true;
            case R.id.check_all:
                checkAll();
                resId = R.string.selected_all_label;
                break;
            case R.id.check_none:
                checkNone();
                resId = R.string.deselected_all_label;
                break;
            case R.id.check_played:
                checkPlayed(true);
                resId = R.string.selected_played_label;
                break;
            case R.id.check_unplayed:
                checkPlayed(false);
                resId = R.string.selected_unplayed_label;
                break;
            case R.id.check_downloaded:
                checkDownloaded(true);
                resId = R.string.selected_downloaded_label;
                break;
            case R.id.check_not_downloaded:
                checkDownloaded(false);
                resId = R.string.selected_not_downloaded_label;
                break;
            case R.id.check_queued:
                checkQueued(true);
                resId = R.string.selected_queued_label;
                break;
            case R.id.check_not_queued:
                checkQueued(false);
                resId = R.string.selected_not_queued_label;
                break;
            case R.id.check_has_media:
                checkWithMedia();
                resId = R.string.selected_has_media_label;
                break;
            case R.id.sort_title_a_z:
                sortByTitle(false);
                return true;
            case R.id.sort_title_z_a:
                sortByTitle(true);
                return true;
            case R.id.sort_date_new_old:
                sortByDate(true);
                return true;
            case R.id.sort_date_old_new:
                sortByDate(false);
                return true;
            case R.id.sort_duration_long_short:
                sortByDuration(true);
                return true;
            case R.id.sort_duration_short_long:
                sortByDuration(false);
                return true;
        }
        if(resId != 0) {
            Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }
    }

    private void sortByTitle(final boolean reverse) {
        Collections.sort(episodes, (lhs, rhs) -> {
            if (reverse) {
                return -1 * lhs.getTitle().compareTo(rhs.getTitle());
            } else {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });
        refreshTitles();
        refreshCheckboxes();
    }

    private void sortByDate(final boolean reverse) {
        Collections.sort(episodes, (lhs, rhs) -> {
            if (lhs.getPubDate() == null) {
                return -1;
            } else if (rhs.getPubDate() == null) {
                return 1;
            }
            int code = lhs.getPubDate().compareTo(rhs.getPubDate());
            if (reverse) {
                return -1 * code;
            } else {
                return code;
            }
        });
        refreshTitles();
        refreshCheckboxes();
    }

    private void sortByDuration(final boolean reverse) {
        Collections.sort(episodes, (lhs, rhs) -> {
            int ordering;
            if (!lhs.hasMedia()) {
                ordering = 1;
            } else if (!rhs.hasMedia()) {
                ordering = -1;
            } else {
                ordering = lhs.getMedia().getDuration() - rhs.getMedia().getDuration();
            }
        if(reverse) {
            return -1 * ordering;
        } else {
            return ordering;
        }
    });
        refreshTitles();
        refreshCheckboxes();
    }

    private void checkAll() {
        for (FeedItem episode : episodes) {
            if(!checkedIds.contains(episode.getId())) {
                checkedIds.add(episode.getId());
            }
        }
        refreshCheckboxes();
    }

    private void checkNone() {
        checkedIds.clear();
        refreshCheckboxes();
    }

    private void checkPlayed(boolean isPlayed) {
        for (FeedItem episode : episodes) {
            if(episode.isPlayed() == isPlayed) {
                if(!checkedIds.contains(episode.getId())) {
                    checkedIds.add(episode.getId());
                }
            } else {
                if(checkedIds.contains(episode.getId())) {
                    checkedIds.remove(episode.getId());
                }
            }
        }
        refreshCheckboxes();
    }

    private void checkDownloaded(boolean isDownloaded) {
        for (FeedItem episode : episodes) {
            if(episode.hasMedia() && episode.getMedia().isDownloaded() == isDownloaded) {
                if(!checkedIds.contains(episode.getId())) {
                    checkedIds.add(episode.getId());
                }
            } else {
                if(checkedIds.contains(episode.getId())) {
                    checkedIds.remove(episode.getId());
                }
            }
        }
        refreshCheckboxes();
    }

    private void checkQueued(boolean isQueued) {
        for (FeedItem episode : episodes) {
            if(episode.isTagged(FeedItem.TAG_QUEUE) == isQueued) {
                checkedIds.add(episode.getId());
            } else {
                checkedIds.remove(episode.getId());
            }
        }
        refreshCheckboxes();
    }

    private void checkWithMedia() {
        for (FeedItem episode : episodes) {
            if(episode.hasMedia()) {
                checkedIds.add(episode.getId());
            } else {
                checkedIds.remove(episode.getId());
            }
        }
        refreshCheckboxes();
    }

    private void refreshTitles() {
        titles.clear();
        for(FeedItem episode : episodes) {
            titles.add(episode.getTitle());
        }
        mAdapter.notifyDataSetChanged();
    }

    private void refreshCheckboxes() {
        for (int i = 0; i < episodes.size(); i++) {
            FeedItem episode = episodes.get(i);
            boolean checked = checkedIds.contains(episode.getId());
            mListView.setItemChecked(i, checked);
        }
        ActivityCompat.invalidateOptionsMenu(EpisodesApplyActionFragment.this.getActivity());
        showSpeedDialIfAnyChecked();

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            String title = checkedIds.size() > 0 ? checkedIds.size() + " selected" : ""; // TODO: restore to the original title
            actionBar.setTitle(title);
        }
    }

    private void queueChecked() {
        DBWriter.addQueueItem(getActivity(), true, checkedIds.toArray());
        close();
    }

    private void markedCheckedPlayed() {
        DBWriter.markItemPlayed(FeedItem.PLAYED, checkedIds.toArray());
        close();
    }

    private void markedCheckedUnplayed() {
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, checkedIds.toArray());
        close();
    }

    private void downloadChecked() {
        // download the check episodes in the same order as they are currently displayed
        List<FeedItem> toDownload = new ArrayList<>(checkedIds.size());
        for(FeedItem episode : episodes) {
            if(checkedIds.contains(episode.getId())) {
                toDownload.add(episode);
            }
        }
        try {
            DBTasks.downloadFeedItems(getActivity(), toDownload.toArray(new FeedItem[toDownload.size()]));
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
        }
        close();
    }

    private void deleteChecked() {
        for(long id : checkedIds.toArray()) {
            FeedItem episode = idMap.get(id);
            if(episode.hasMedia()) {
                DBWriter.deleteFeedMediaOfItem(getActivity(), episode.getMedia().getId());
            }
        }
        close();
    }

    private void close() {
        getActivity().getSupportFragmentManager().popBackStack();
    }

}
