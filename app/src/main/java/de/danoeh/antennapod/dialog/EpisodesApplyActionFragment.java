package de.danoeh.antennapod.dialog;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    public String TAG = "EpisodeActionFragment";

    private ListView mListView;
    private ArrayAdapter<String> mAdapter;

    private Button btnAddToQueue;
    private Button btnMarkAsPlayed;
    private Button btnMarkAsUnplayed;
    private Button btnDownload;
    private Button btnDelete;

    private final Map<Long,FeedItem> idMap;
    private final List<FeedItem> episodes;
    private final List<String> titles = new ArrayList();
    private final LongList checkedIds = new LongList();

    private MenuItem mSelectToggle;

    private int textColor;

    public EpisodesApplyActionFragment(List<FeedItem> episodes) {
        this.episodes = episodes;
        this.idMap = new HashMap<>(episodes.size());
        for(FeedItem episode : episodes) {
            this.idMap.put(episode.getId(), episode);
        }
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

        mListView = (ListView) view.findViewById(android.R.id.list);
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

        for(FeedItem episode : episodes) {
            titles.add(episode.getTitle());
        }

        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_multiple_choice, titles);
        mListView.setAdapter(mAdapter);
        checkAll();

        btnAddToQueue = (Button) view.findViewById(R.id.btnAddToQueue);
        btnAddToQueue.setOnClickListener(v -> queueChecked());
        btnMarkAsPlayed = (Button) view.findViewById(R.id.btnMarkAsPlayed);
        btnMarkAsPlayed.setOnClickListener(v -> markedCheckedPlayed());
        btnMarkAsUnplayed = (Button) view.findViewById(R.id.btnMarkAsUnplayed);
        btnMarkAsUnplayed.setOnClickListener(v -> markedCheckedUnplayed());
        btnDownload = (Button) view.findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> downloadChecked());
        btnDelete = (Button) view.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> deleteChecked());

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodes_apply_action_options, menu);

        int[] attrs = { android.R.attr.textColor };
        TypedArray ta = getActivity().obtainStyledAttributes(attrs);
        textColor = ta.getColor(0, Color.GRAY);
        ta.recycle();

        menu.findItem(R.id.sort).setIcon(new IconDrawable(getActivity(),
                FontAwesomeIcons.fa_sort).color(textColor).actionBarSize());

        mSelectToggle = menu.findItem(R.id.select_toggle);
        mSelectToggle.setOnMenuItemClickListener(item -> {
            if (checkedIds.size() == episodes.size()) {
                checkNone();
            } else {
                checkAll();
            }
            return true;
        });

        menu.findItem(R.id.select_options).setIcon(new IconDrawable(getActivity(),
                FontAwesomeIcons.fa_caret_down).color(textColor).actionBarSize());
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        Icon icon;
        if(checkedIds.size() == episodes.size()) {
            icon = FontAwesomeIcons.fa_check_square_o;
        } else if(checkedIds.size() == 0) {
            icon = FontAwesomeIcons.fa_square_o;
        } else {
            icon = FontAwesomeIcons.fa_minus_square_o;
        }
        mSelectToggle.setIcon(new IconDrawable(getActivity(), icon).color(textColor).actionBarSize());

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
            if (false == lhs.hasMedia()) {
                ordering = 1;
            } else if (false == rhs.hasMedia()) {
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
            if(false == checkedIds.contains(episode.getId())) {
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
        List<FeedItem> toDownload = new ArrayList<FeedItem>(checkedIds.size());
        for(FeedItem episode : episodes) {
            if(checkedIds.contains(episode.getId())) {
                toDownload.add(episode);
            }
        }
        try {
            DBTasks.downloadFeedItems(getActivity(), toDownload.toArray(new FeedItem[0]));
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
